package com.mphasis.tse.service.async;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingScheduler {

    private final TransactionMetaTableRepository transactionMetaTableRepository;
    private final JobLauncher jobLauncher;
    
    @Qualifier("tradeFileProcessingJob")
    private final Job job;

    // Limit concurrency to 1 to ensure reliability over speed and no memory spikes
    private static final int MAX_CONCURRENT_JOBS = 1;

    @Scheduled(fixedDelayString = "${batch.scheduler.delay:5000}")
    public void processPendingFiles() {
        // 1. Check current running jobs
        long runningJobsCount = transactionMetaTableRepository.countByStatusIn(
                List.of(FileStatus.STARTED, FileStatus.PROCESSING)
        );

        if (runningJobsCount >= MAX_CONCURRENT_JOBS) {
            log.trace("Max concurrent jobs ({}) reached. Waiting for next cycle.", MAX_CONCURRENT_JOBS);
            return;
        }

        // 2. Find oldest PENDING file
        FileLoadMetaData pendingFile = transactionMetaTableRepository.findFirstByStatusOrderByUploadTimeAsc(FileStatus.PENDING);
        
        if (pendingFile == null) {
            return; // No files to process
        }

        // 3. Mark as STARTED
        pendingFile.setStatus(FileStatus.STARTED);
        transactionMetaTableRepository.save(pendingFile);

        log.info("Scheduler picked up fileId={} filename={} for processing", pendingFile.getFileId(), pendingFile.getFilename());

        // 4. Launch Spring Batch Job
        try {
            // Using only fileMetaId ensures Spring Batch can resume a failed job since parameters match exactly
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", pendingFile.getFilePath())
                    .addLong("fileMetaId", pendingFile.getFileId())
                    .toJobParameters();

            // Run job synchronously in this scheduler thread (since concurrency is 1, this blocks until done)
            // Wait, if it runs synchronously, the @Scheduled task blocks until it finishes. This naturally enforces concurrency!
            // But if JobLauncher is async (because of jobLauncherExecutor), it will return immediately.
            // In Spring Batch, JobLauncher is synchronous by default unless configured otherwise.
            // Our app defines a custom "jobLauncherExecutor" Async bean, but we use the default jobLauncher here?
            // Actually, we can just use our existing AsyncProcessingService!
            // Let's just use JobLauncher. If it's sync, great. If async, our DB check handles concurrency.
            
            JobExecution execution = jobLauncher.run(job, jobParameters);
            log.info("Batch job launched. executionId={}, status={}", execution.getId(), execution.getStatus());

        } catch (Exception e) {
            log.error("Failed to launch batch job for fileId={}", pendingFile.getFileId(), e);
            pendingFile.setStatus(FileStatus.FAILED);
            transactionMetaTableRepository.save(pendingFile);
        }
    }
}
