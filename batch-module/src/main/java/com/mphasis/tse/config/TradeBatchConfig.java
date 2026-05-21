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
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public ItemStreamReader<String []> transactionReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        FlatFileItemReader<String[]> delegate = new FlatFileItemReaderBuilder<String[]>()
                .name(READER_NAME)
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .lineMapper(this::parseCsvLine)
                .build();

        return new SynchronizedItemStreamReaderBuilder<String[]>()
                .delegate(delegate)
                .build();
    }

    private String[] parseCsvLine(String line, int lineNumber) {
        try (CSVParser parser = CSVParser.parse(line, CSVFormat.DEFAULT)) {
            CSVRecord record = parser.iterator().next();
            List<String> values = new ArrayList<>();
            record.forEach(values::add);

            String[] fields = new String[EXPECTED_TRADE_COLUMNS + 1];
            for (int i = 0; i < EXPECTED_TRADE_COLUMNS; i++) {
                fields[i] = (i < values.size() && values.get(i) != null) ? values.get(i).trim() : "";
            }
            fields[EXPECTED_TRADE_COLUMNS] = String.valueOf(lineNumber);
            return fields;
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("Unable to parse CSV line " + lineNumber, e);
        }
    }

    @Bean
    public Step tradeProcessingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<String[]> reader,
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
