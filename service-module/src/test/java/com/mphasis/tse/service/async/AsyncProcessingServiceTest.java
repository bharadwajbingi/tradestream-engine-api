package com.mphasis.tse.service.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AsyncProcessingServiceTest {
    @Mock
    private JobOperator jobOperator;
    @InjectMocks
    private AsyncProcessingService asyncProcessingService;
    @Test
    void testProcess_success() throws Exception {
        Job job = mock(Job.class);
        JobParameters params = new JobParameters();
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.COMPLETED);
        when(jobOperator.run(any(Job.class), any(JobParameters.class)))
                .thenReturn(jobExecution);

        asyncProcessingService.process(job, params);
        verify(jobOperator, times(1))
                .run(any(Job.class), any(JobParameters.class));
    }
    @Test
    void testProcess_exception() throws Exception {
        Job job = mock(Job.class);
        JobParameters params = new JobParameters();

        when(jobOperator.run(any(Job.class), any(JobParameters.class)))
                .thenThrow(new RuntimeException("Job failed"));

        asyncProcessingService.process(job, params);

        verify(jobOperator, times(1))
                .run(any(Job.class), any(JobParameters.class));
    }
}




