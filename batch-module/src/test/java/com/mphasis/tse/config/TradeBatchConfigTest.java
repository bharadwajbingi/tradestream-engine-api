package com.mphasis.tse.config;

import com.mphasis.tse.config.listener.JobListener;
import com.mphasis.tse.config.processor.TradeRecordProcessor;
import com.mphasis.tse.config.writer.TradeItemWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradeBatchConfigTest {
    private TradeBatchConfig config;
    private JobListener jobListener;
    private TradeRecordProcessor processor;
    private TradeItemWriter writer;
    @BeforeEach
    void setUp() {
        jobListener = mock(JobListener.class);
        processor = mock(TradeRecordProcessor.class);
        writer = mock(TradeItemWriter.class);
        config = new TradeBatchConfig(100, jobListener, processor, writer);
    }

    @Test
    void testTransactionReader() throws Exception{
        File tempFile = File.createTempFile("test", ".csv");
        Files.write(tempFile.toPath(), List.of(
                "header1,header2,header3",
                "A,B,C"
                ));
        ItemStreamReader<String[]> reader =
                config.transactionReader(tempFile.getAbsolutePath());
        reader.open(new ExecutionContext());
        String[] result = reader.read();
        assertNotNull(result);
        assertEquals(22, result.length);
        assertEquals("A", result[0]);
        assertEquals("B", result[1]);
        assertEquals("C", result[2]);
        assertEquals("2", result[21]);
        reader.close();
    }

    @Test
    void testTradeProcessingStep() {
        JobRepository jobRepository = mock(JobRepository.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        ItemStreamReader<String[]> reader =
                mock(ItemStreamReader.class);
        AsyncTaskExecutor taskExecutor = mock(AsyncTaskExecutor.class);
        Step step = config.tradeProcessingStep(
                jobRepository,
                transactionManager,
                reader,
                taskExecutor,
                processor,
                writer
        );
        assertNotNull(step);
    }

    @Test
    void testTradeFileProcessingJob() {
        JobRepository jobRepository = mock(JobRepository.class);
        Step step = mock(Step.class);
        Job job = config.tradeFileProcessingJob(jobRepository, step);
        assertNotNull(job);
    }
}
