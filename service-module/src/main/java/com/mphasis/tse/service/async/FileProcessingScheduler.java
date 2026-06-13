package com.mphasis.tse.service.async;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingScheduler {

    private final TransactionMetaTableRepository transactionMetaTableRepository;
    private final AsyncProcessingService asyncProcessingService;
    
    @Qualifier("tradeFileProcessingJob")
    private final Job job;

    @Scheduled(fixedDelayString = "${batch.scheduler.delay:5000}")
    public void processPendingFiles() {
        // 1. Get users with currently running jobs
        List<Long> activeUserIds = transactionMetaTableRepository.findUsersWithActiveJobs();
        java.util.Set<Long> processingUsers = new java.util.HashSet<>(activeUserIds);

        // 2. Get all pending files
        List<FileLoadMetaData> pendingFiles = transactionMetaTableRepository.findByStatusOrderByUploadTimeAsc(FileStatus.PENDING);

        if (pendingFiles.isEmpty()) {
            return;
        }

        // 3. Process one pending file per user
        for (FileLoadMetaData pendingFile : pendingFiles) {
            if (pendingFile.getUser() == null) continue;
            Long userId = pendingFile.getUser().getId();
            
            // If user is already processing a file, skip to next file
            if (processingUsers.contains(userId)) {
                continue;
            }

            // Mark user as processing so we don't pick their second pending file
            processingUsers.add(userId);

            // Mark as STARTED
            pendingFile.setStatus(FileStatus.STARTED);
            transactionMetaTableRepository.save(pendingFile);

            // Skip files with missing file path (corrupted data)
            if (pendingFile.getFilePath() == null || pendingFile.getFilePath().isBlank()) {
                log.warn("Skipping fileId={} — filePath is null or blank. Marking as FAILED.", pendingFile.getFileId());
                pendingFile.setStatus(FileStatus.FAILED);
                transactionMetaTableRepository.save(pendingFile);
                continue;
            }

            log.info("Scheduler picked up fileId={} filename={} for user={} for processing", 
                    pendingFile.getFileId(), pendingFile.getFilename(), userId);

            // 4. Launch Spring Batch Job asynchronously via dedicated thread pool
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", pendingFile.getFilePath())
                    .addLong("fileMetaId", pendingFile.getFileId())
                    .addLong("runId", System.currentTimeMillis())
                    .toJobParameters();

            asyncProcessingService.process(job, jobParameters);
        }
    }
}
