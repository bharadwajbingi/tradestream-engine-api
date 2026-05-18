package com.mphasis.tse.config.listener;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.exception.FileNotFoundException;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
@Getter
@Component
public class JobListener implements JobExecutionListener {

    private final TransactionMetaTableRepository transactionMetaTableRepository;

    private FileLoadMetaData fileLoadMetaData;
    private Set<String> seenTransactionIds;

    public JobListener(TransactionMetaTableRepository transactionMetaTableRepository) {
        this.transactionMetaTableRepository = transactionMetaTableRepository;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {

        seenTransactionIds = ConcurrentHashMap.newKeySet();
        Long metaId = jobExecution.getJobParameters().getLong("fileMetaId");

        if (metaId == null) {
            throw new FileNotFoundException("metaId is required for job execution");
        }

        fileLoadMetaData = transactionMetaTableRepository.findById(metaId)
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
            fileLoadMetaData = transactionMetaTableRepository.findById(metaId)
                    .orElseThrow(() -> {
                        log.error("File metadata not found for id: {}", metaId);
                        return new FileNotFoundException("File metadata not found for id: " + metaId);
                    });

            log.info("Loaded FileLoadMetaData for id={} and fileName={}", metaId, fileLoadMetaData.getFilename());

            ExecutionContext context = jobExecution.getExecutionContext();

            int successCount = context.containsKey("successCount") ? context.getInt("successCount") : 0;
            int errorCount = context.containsKey("errorCount") ? context.getInt("errorCount") : 0;

            log.info("Job Execution Summary: successCount={}, errorCount={}", successCount, errorCount);

            long readCount = jobExecution.getStepExecutions()
                    .stream()
                    .filter(step -> "tradeProcessingStep".equals(step.getStepName()))
                    .mapToLong(StepExecution::getReadCount)
                    .sum();

            log.info("Total records read by step 'tradeProcessingStep': {}", readCount);

            if (jobExecution.getStatus().isUnsuccessful()) {
                log.warn("Job failed with status: {}", jobExecution.getStatus());
                fileLoadMetaData.setStatus(FileStatus.FAILED);

            } else if (errorCount > 0) {
                log.warn("Job completed with errors. errorCount={}", errorCount);
                fileLoadMetaData.setStatus(FileStatus.COMPLETED_WITH_ERROR);

            } else if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                log.info("Job completed successfully");
                fileLoadMetaData.setStatus(FileStatus.COMPLETED);
            }

            fileLoadMetaData.setSuccessCount(successCount);
            fileLoadMetaData.setErrorCount(errorCount);
            fileLoadMetaData.setTotalRecords((int) readCount);

            transactionMetaTableRepository.save(fileLoadMetaData);

            log.info("FileLoadMetaData updated successfully for metaId={}", metaId);
            log.info("afterJob() completed");
        }
    }

