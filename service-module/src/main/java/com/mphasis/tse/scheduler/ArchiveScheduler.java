package com.mphasis.tse.scheduler;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import com.mphasis.tse.service.IFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class ArchiveScheduler {

    private final TransactionMetaTableRepository transactionMetaTableRepository;
    private final IFileService fileService;

    public ArchiveScheduler(TransactionMetaTableRepository transactionMetaTableRepository,
                            IFileService fileService) {
        this.transactionMetaTableRepository = transactionMetaTableRepository;
        this.fileService = fileService;
    }

    // Scheduled to run daily at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleAutoArchive() {
        log.info("Auto-Archive scheduler triggered");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<FileLoadMetaData> expiredFiles = transactionMetaTableRepository.findExpiredFilesForArchiving(cutoff);
        log.info("Found {} files older than 30 days to archive", expiredFiles.size());
        for (FileLoadMetaData file : expiredFiles) {
            try {
                fileService.archiveFileLoad(file.getFileId());
                log.info("Auto-archived file ID: {}, name: {}", file.getFileId(), file.getFilename());
            } catch (Exception e) {
                log.error("Failed to auto-archive file ID: {}", file.getFileId(), e);
            }
        }
    }
}
