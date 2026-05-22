package com.mphasis.tse.config.listener;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.exception.FileNotFoundException;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import com.mphasis.tse.repository.TransactionErrorRepository;
import com.mphasis.tse.enums.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class JobListener implements JobExecutionListener {

    private final TransactionMetaTableRepository transactionMetaTableRepository;
    private final TransactionErrorRepository transactionErrorRepository;

    public JobListener(TransactionMetaTableRepository transactionMetaTableRepository,
                       TransactionErrorRepository transactionErrorRepository) {
        this.transactionMetaTableRepository = transactionMetaTableRepository;
        this.transactionErrorRepository = transactionErrorRepository;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        Long metaId = jobExecution.getJobParameters().getLong("fileMetaId");

        if (metaId == null) {
            throw new FileNotFoundException("metaId is required for job execution");
        }

        FileLoadMetaData fileLoadMetaData = transactionMetaTableRepository.findById(metaId)
                .orElseThrow(() -> new FileNotFoundException("File metadata not found for id: " + metaId));

        fileLoadMetaData.setStatus(FileStatus.PROCESSING);
        fileLoadMetaData.setUploadTime(LocalDateTime.now());
        transactionMetaTableRepository.save(fileLoadMetaData);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("afterJob() started for Job: {}", jobExecution.getJobInstance().getJobName());
        Long metaId = jobExecution.getJobParameters().getLong("fileMetaId");

        if (metaId == null) {
            log.error("metaId missing in job parameters. JobExecutionId={}", jobExecution.getId());
            throw new FileNotFoundException("metaId is required for job execution");
        }

        log.info("metaId found: {}", metaId);
        FileLoadMetaData fileLoadMetaData = transactionMetaTableRepository.findById(metaId)
                .orElseThrow(() -> {
                    log.error("File metadata not found for id: {}", metaId);
                    return new FileNotFoundException("File metadata not found for id: " + metaId);
                });

        log.info("Loaded FileLoadMetaData for id={} and fileName={}", metaId, fileLoadMetaData.getFilename());

        ExecutionContext context = jobExecution.getExecutionContext();

        int successCount = context.containsKey("successCount") ? context.getInt("successCount") : 0;
        int errorCount = context.containsKey("errorCount") ? context.getInt("errorCount") : 0;
        int duplicateCount = context.containsKey("duplicateCount") ? context.getInt("duplicateCount") : 0;

        log.info("Job Execution Summary: successCount={}, errorCount={}, duplicateCount={}", successCount, errorCount, duplicateCount);

        long readCount = jobExecution.getStepExecutions()
                .stream()
                .filter(step -> "tradeProcessingStep".equals(step.getStepName()))
                .mapToLong(StepExecution::getReadCount)
                .sum();

        log.info("Total records read by step 'tradeProcessingStep': {}", readCount);

        long activeErrors = transactionErrorRepository.countByMetaData_FileIdAndStatusIn(
                metaId,
                java.util.List.of(ErrorStatus.FAILED, ErrorStatus.INVALID_TRANSACTION_ID)
        );

        if (jobExecution.getStatus().isUnsuccessful()) {
            log.warn("Job failed with status: {}", jobExecution.getStatus());
            fileLoadMetaData.setStatus(FileStatus.FAILED);
        } else if (activeErrors > 0) {
            log.warn("Job completed with errors. activeErrors={}", activeErrors);
            fileLoadMetaData.setStatus(FileStatus.COMPLETED_WITH_ERROR);
        } else {
            log.info("Job completed successfully with no active errors");
            fileLoadMetaData.setStatus(FileStatus.COMPLETED);
        }

        fileLoadMetaData.setSuccessCount(successCount);
        fileLoadMetaData.setErrorCount(errorCount);
        fileLoadMetaData.setDuplicateCount(duplicateCount);
        fileLoadMetaData.setTotalRecords((int) readCount);

        if (fileLoadMetaData.getUploadTime() != null) {
            long durationMs = java.time.Duration.between(fileLoadMetaData.getUploadTime(), LocalDateTime.now()).toMillis();
            fileLoadMetaData.setProcessingTimeMs(durationMs);
        }

        transactionMetaTableRepository.save(fileLoadMetaData);

        log.info("FileLoadMetaData updated successfully for metaId={}", metaId);
        log.info("afterJob() completed");
    }
}
