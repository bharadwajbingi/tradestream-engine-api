package com.mphasis.tse.service.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncProcessingService {

    private final JobOperator jobOperator;

    @Async("jobLauncherExecutor")
    public void process(Job job, JobParameters jobParameters) {
        try {

            JobExecution execution = jobOperator.start(job, jobParameters);


        } catch (Exception e) {
            log.error("Async failed  error: {}",e.getMessage());
        }
    }
}