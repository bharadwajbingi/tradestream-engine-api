package com.mphasis.tse.config.listener;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.exception.FileNotFoundException;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobListenerTest {
    @InjectMocks
    private JobListener jobListener;
    @Mock
    private TransactionMetaTableRepository repository;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private JobParameters jobParameters;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    private JobInstance jobInstance;
    @Mock
    private StepExecution stepExecution;
    private FileLoadMetaData fileLoadMetaData;


    @Test
    void testBeforeJob_success() {
        fileLoadMetaData = new FileLoadMetaData();
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(fileLoadMetaData));
        jobListener.beforeJob(jobExecution);
        assertEquals(FileStatus.PROCESSING, fileLoadMetaData.getStatus());
        verify(repository).save(fileLoadMetaData);
    }

    @Test
    void testBeforeJob_metaIdNull() {
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(null);
        assertThrows(FileNotFoundException.class,
                () -> jobListener.beforeJob(jobExecution));
    }

    @Test
    void testBeforeJob_metadataNotFound() {
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(FileNotFoundException.class,
                () -> jobListener.beforeJob(jobExecution));
    }

    @Test
    void testAfterJob_completedSuccess() {
        fileLoadMetaData = new FileLoadMetaData();
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(fileLoadMetaData));
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.containsKey("successCount")).thenReturn(true);
        when(executionContext.getInt("successCount")).thenReturn(10);
        when(executionContext.containsKey("errorCount")).thenReturn(true);
        when(executionContext.getInt("errorCount")).thenReturn(0);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStepExecutions()).thenReturn(Set.of(stepExecution));
        when(stepExecution.getStepName()).thenReturn("tradeProcessingStep");
        when(stepExecution.getReadCount()).thenReturn(10L);
        jobListener.afterJob(jobExecution);
        assertEquals(FileStatus.COMPLETED, fileLoadMetaData.getStatus());
        assertEquals(10, fileLoadMetaData.getSuccessCount());
        assertEquals(0, fileLoadMetaData.getErrorCount());
        verify(repository).save(fileLoadMetaData);
    }

    @Test
    void testAfterJob_completedWithErrors() {
        fileLoadMetaData = new FileLoadMetaData();
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(fileLoadMetaData));
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.containsKey("successCount")).thenReturn(true);
        when(executionContext.getInt("successCount")).thenReturn(5);
        when(executionContext.containsKey("errorCount")).thenReturn(true);
        when(executionContext.getInt("errorCount")).thenReturn(2);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStepExecutions()).thenReturn(Set.of(stepExecution));
        when(stepExecution.getStepName()).thenReturn("tradeProcessingStep");
        when(stepExecution.getReadCount()).thenReturn(7L);
        jobListener.afterJob(jobExecution);
        assertEquals(FileStatus.COMPLETED_WITH_ERROR, fileLoadMetaData.getStatus());
    }

    @Test
    void testAfterJob_failed() {
        fileLoadMetaData = new FileLoadMetaData();
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(fileLoadMetaData));
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.containsKey("successCount")).thenReturn(false);
        when(executionContext.containsKey("errorCount")).thenReturn(false);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(jobExecution.getStepExecutions()).thenReturn(Set.of());
        jobListener.afterJob(jobExecution);
        assertEquals(FileStatus.FAILED, fileLoadMetaData.getStatus());
    }

    @Test
    void testAfterJob_metaIdNull() {
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(null);
        assertThrows(FileNotFoundException.class,
                () -> jobListener.afterJob(jobExecution));
    }

    @Test
    void testAfterJob_metadataNotFound() {
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(FileNotFoundException.class,
                () -> jobListener.afterJob(jobExecution));
    }

    @Test
    void testAfterJob_successCountAbsent() {
        fileLoadMetaData = new FileLoadMetaData();
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(fileLoadMetaData));
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.containsKey("successCount")).thenReturn(false);
        when(executionContext.containsKey("errorCount")).thenReturn(false);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStepExecutions()).thenReturn(Set.of());
        jobListener.afterJob(jobExecution);
        assertEquals(0, fileLoadMetaData.getSuccessCount());
    }

    @Test
    void testAfterJob_emptySteps() {
        fileLoadMetaData = new FileLoadMetaData();
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(fileLoadMetaData));
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.containsKey("successCount")).thenReturn(false);
        when(executionContext.containsKey("errorCount")).thenReturn(false);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStepExecutions()).thenReturn(Set.of());
        jobListener.afterJob(jobExecution);
        assertEquals(0, fileLoadMetaData.getTotalRecords());
    }

    @Test
    void testAfterJob_completedWithoutErrors() {
        fileLoadMetaData = new FileLoadMetaData();
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(fileLoadMetaData));
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.containsKey("successCount")).thenReturn(true);
        when(executionContext.getInt("successCount")).thenReturn(10);
        when(executionContext.containsKey("errorCount")).thenReturn(true);
        when(executionContext.getInt("errorCount")).thenReturn(0);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStepExecutions()).thenReturn(Set.of(stepExecution));
        when(stepExecution.getStepName()).thenReturn("tradeProcessingStep");
        when(stepExecution.getReadCount()).thenReturn(10L);
        jobListener.afterJob(jobExecution);
        assertEquals(FileStatus.COMPLETED, fileLoadMetaData.getStatus());
    }

    @Test
    void testAfterJob_stoppedStatus() {
        fileLoadMetaData = new FileLoadMetaData();
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobParameters.getLong("fileMetaId")).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(fileLoadMetaData));
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.containsKey("successCount")).thenReturn(false);
        when(executionContext.containsKey("errorCount")).thenReturn(false);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STOPPED);
        when(jobExecution.getStepExecutions()).thenReturn(Set.of());
        jobListener.afterJob(jobExecution);

    }
}