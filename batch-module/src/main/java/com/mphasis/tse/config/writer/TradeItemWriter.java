package com.mphasis.tse.config.writer;

import com.mphasis.tse.entity.TradeTransaction;
import com.mphasis.tse.entity.TradeWrapper;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.entity.TransactionRegistry;

import com.mphasis.tse.enums.ErrorStatus;
import com.mphasis.tse.repository.TransactionErrorRepository;
import com.mphasis.tse.repository.TransactionMainTableRepository;
import com.mphasis.tse.repository.TransactionRegistryRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@StepScope
@Component
public class TradeItemWriter implements ItemWriter<TradeWrapper> {

    private final TransactionMainTableRepository tradeTransactionRepository;
    private final TransactionErrorRepository transactionErrorRepository;
    private final TransactionRegistryRepository transactionRegistryRepository;

    private StepExecution stepExecution;

    public TradeItemWriter(TransactionMainTableRepository tradeTransactionRepository,
                           TransactionErrorRepository transactionErrorRepository,
                           TransactionRegistryRepository transactionRegistryRepository) {

        this.tradeTransactionRepository = tradeTransactionRepository;
        this.transactionErrorRepository = transactionErrorRepository;
        this.transactionRegistryRepository = transactionRegistryRepository;
    }

    @Value("#{stepExecution}")
    public void setStepExecution(StepExecution stepExecution)
    {
        this.stepExecution = stepExecution;
    }


    @Override
    public void write(Chunk<? extends TradeWrapper> chunk) {

        log.info("Starting write() with {} records in chunk", chunk.size());

        List<TradeTransaction> successList = new ArrayList<>();
        List<TransactionError> errorList = new ArrayList<>();
        List<TransactionRegistry> registryList = new ArrayList<>();
        List<String> successTransactionIds = new ArrayList<>();
        int error = 0;

        for (TradeWrapper wrapper : chunk) {

            if (wrapper.getTradeTransaction() != null) {
                TradeTransaction transaction = wrapper.getTradeTransaction();
                successList.add(transaction);
                registryList.add(new TransactionRegistry(transaction.getTransactionId()));
                successTransactionIds.add(transaction.getTransactionId());
                log.debug("Processed successful transactionId={}", transaction.getTransactionId());
            }

            if (wrapper.getErrors() != null && !wrapper.getErrors().isEmpty()) {
                errorList.addAll(wrapper.getErrors());
                error++;
                log.debug("Processed {} errors for a record", wrapper.getErrors().size());
            }
        }

        if (!successList.isEmpty()) {
            log.info("Saving {} successful transactions", successList.size());
            tradeTransactionRepository.saveAll(successList);
        }

        if (!registryList.isEmpty()) {
            log.info("Saving {} registry records", registryList.size());
            transactionErrorRepository.updateStatusByTransactionIds(
                    successTransactionIds,
                    ErrorStatus.RESOLVED
            );
            transactionRegistryRepository.saveAll(registryList);
        }

        if (!errorList.isEmpty()) {
            log.info("Saving {} error records", errorList.size());
            transactionErrorRepository.saveAll(errorList);
        }

        ExecutionContext context = stepExecution.getJobExecution().getExecutionContext();

        int successCount = context.getInt("successCount", 0);
        context.putInt("successCount", successCount + successList.size());
        log.info("Updated successCount from {} to {}", successCount, successCount + successList.size());

        int errorCount = context.getInt("errorCount", 0);
        context.putInt("errorCount", errorCount + error);
        log.info("Updated errorCount from {} to {}", errorCount, errorCount + error);

        log.info("write() completed");
    }
}

