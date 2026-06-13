package com.mphasis.tse.service.async;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoRecoveryService {

    private final TransactionMetaTableRepository transactionMetaTableRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverStuckJobs() {
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
}
