package com.mphasis.tse.config;

import com.mphasis.tse.config.listener.JobListener;
import com.mphasis.tse.config.processor.TradeRecordProcessor;
import com.mphasis.tse.config.writer.TradeItemWriter;
import com.mphasis.tse.entity.TradeWrapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import static com.mphasis.tse.utils.BatchUtils.*;

@Slf4j
@Configuration
public class TradeBatchConfig {

    private int chunkSize;
    private final JobListener jobListener;
    private final TradeRecordProcessor processor;
    private final TradeItemWriter writer;

    public TradeBatchConfig(@Value("${batch.trade.chunk-size}")int chunkSize,
                            JobListener jobListener,
                            TradeRecordProcessor tradeRecordProcessor, TradeItemWriter itemWriter)
    {
        this.jobListener = jobListener;
        this.processor = tradeRecordProcessor;
        this.writer = itemWriter;
        this.chunkSize=chunkSize;
    }

    @Bean
    @StepScope
    public FlatFileItemReader<String []> transactionReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        return new FlatFileItemReaderBuilder<String[]>()
                .name(READER_NAME)
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .lineMapper(((line, lineNumber) -> line.split(DELIMITER)))
                .build();
    }

    @Bean
    public Step tradeProcessingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<String[]> reader,
            AsyncTaskExecutor stepTaskExecutor,
            ItemProcessor<String[], TradeWrapper> processor,
            ItemWriter<TradeWrapper> writer) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<String[], TradeWrapper>chunk(chunkSize,transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(stepTaskExecutor)
                .build();
    }


    @Bean(name =JOB_NAME )
    public Job tradeFileProcessingJob(JobRepository jobRepository,
                                      Step tradeProcessingStep) {

        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(jobListener)
                .start(tradeProcessingStep)
                .build();
    }
}
