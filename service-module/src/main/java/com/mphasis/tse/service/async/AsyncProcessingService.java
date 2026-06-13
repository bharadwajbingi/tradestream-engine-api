package com.mphasis.tse.service.async;

import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncProcessingService {

    private final JobOperator jobOperator;
    private final TransactionMetaTableRepository transactionMetaTableRepository;

    @Async("jobLauncherExecutor")
    public void process(Job job, JobParameters jobParameters) {
        Long fileMetaId = jobParameters.getLong("fileMetaId");
        String filePath = jobParameters.getString("filePath");
        try {

            JobExecution execution = jobOperator.start(job, jobParameters);
            log.info("Batch job completed launch cycle. jobId={}, executionId={}, fileMetaId={}",
                    job.getName(), execution.getId(), fileMetaId);

            // Only delete temp file after successful job completion
            if (execution.getStatus().isUnsuccessful()) {
                log.warn("Job finished unsuccessfully for fileMetaId={}. Keeping temp file for recovery.", fileMetaId);
            } else {
                deleteTempFile(filePath);
            }

        } catch (Exception e) {
            log.error("Async processing failed for fileMetaId={} error={}", fileMetaId, e.getMessage(), e);
            markFileFailed(fileMetaId);
            // Don't delete temp file on failure — AutoRecoveryService may retry
        }
    }

    private void markFileFailed(Long fileMetaId) {
        if (fileMetaId == null) {
            return;
        }
        transactionMetaTableRepository.findById(fileMetaId).ifPresent(meta -> {
            meta.setStatus(FileStatus.FAILED);
            transactionMetaTableRepository.save(meta);
        });
    }

    private void deleteTempFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(filePath));
            log.info("Deleted temp upload file path={}", filePath);
        } catch (Exception e) {
            log.warn("Could not delete temp upload file path={} error={}", filePath, e.getMessage());
        }
    }
}
