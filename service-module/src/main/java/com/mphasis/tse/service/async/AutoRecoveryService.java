package com.mphasis.tse.service.async;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoRecoveryService {

    private final TransactionMetaTableRepository transactionMetaTableRepository;

    /**
     * Runs on startup to recover any jobs stuck from a previous crash.
     * No time check needed — app just restarted, anything STARTED/PROCESSING is definitely stuck.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        log.info("Running auto-recovery for stuck file processing jobs...");
        
        List<FileLoadMetaData> stuckFiles = transactionMetaTableRepository.findByStatusIn(
                List.of(FileStatus.STARTED, FileStatus.PROCESSING)
        );

        if (stuckFiles.isEmpty()) {
            log.info("No stuck jobs found. System is healthy.");
            return;
        }

        log.warn("Found {} jobs stuck in STARTED/PROCESSING state. Reverting to PENDING for automatic resume.", stuckFiles.size());

        for (FileLoadMetaData file : stuckFiles) {
            file.setStatus(FileStatus.PENDING);
            transactionMetaTableRepository.save(file);
            log.info("Reverted fileId={} ({}) to PENDING state.", file.getFileId(), file.getFilename());
        }
        
        log.info("Auto-recovery complete. The FileProcessingScheduler will pick up the pending files.");
    }

    /**
     * Periodic recovery — catches files stuck due to rejection, DB timeout, or silent failures
     * while the app is still running (no restart needed).
     * Only recovers files stuck for more than 30 minutes to avoid false recovery of legitimately slow jobs.
     */
    @Scheduled(fixedDelay = 1800000) // every 30 minutes
    public void recoverStuckJobsPeriodic() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        List<FileLoadMetaData> stuckFiles = transactionMetaTableRepository.findByStatusIn(
                List.of(FileStatus.STARTED, FileStatus.PROCESSING)
        );

        // Filter only files stuck longer than 30 minutes
        List<FileLoadMetaData> trulyStuck = stuckFiles.stream()
                .filter(f -> f.getUploadTime() != null && f.getUploadTime().isBefore(cutoff))
                .toList();

        if (trulyStuck.isEmpty()) {
            return; // silent — don't log every 30 minutes if nothing is stuck
        }

        log.warn("Periodic recovery: found {} jobs stuck for >30 min. Reverting to PENDING.", trulyStuck.size());

        for (FileLoadMetaData file : trulyStuck) {
            file.setStatus(FileStatus.PENDING);
            transactionMetaTableRepository.save(file);
            log.info("Periodic recovery: reverted fileId={} ({}) to PENDING.", file.getFileId(), file.getFilename());
        }
    }
}
