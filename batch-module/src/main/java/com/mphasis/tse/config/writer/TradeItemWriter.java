package com.mphasis.tse.config.writer;

import com.mphasis.tse.entity.TradeTransaction;
import com.mphasis.tse.entity.TradeWrapper;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.entity.TransactionRegistry;
import com.mphasis.tse.entity.User;
import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.ErrorStatus;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.repository.TransactionErrorRepository;
import com.mphasis.tse.repository.TransactionMainTableRepository;
import com.mphasis.tse.repository.TransactionRegistryRepository;
import com.mphasis.tse.repository.TransactionMetaTableRepository;

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
    private final TransactionMetaTableRepository transactionMetaTableRepository;

    private StepExecution stepExecution;

    public TradeItemWriter(TransactionMainTableRepository tradeTransactionRepository,
                           TransactionErrorRepository transactionErrorRepository,
                           TransactionRegistryRepository transactionRegistryRepository,
                           TransactionMetaTableRepository transactionMetaTableRepository) {
        this.tradeTransactionRepository = tradeTransactionRepository;
        this.transactionErrorRepository = transactionErrorRepository;
        this.transactionRegistryRepository = transactionRegistryRepository;
        this.transactionMetaTableRepository = transactionMetaTableRepository;
    }

    @Value("#{stepExecution}")
    public void setStepExecution(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public synchronized void write(Chunk<? extends TradeWrapper> chunk) {
        log.info("Starting write() with {} records in chunk", chunk.size());

        List<TradeTransaction> successList = new ArrayList<>();
        List<TransactionError> errorList = new ArrayList<>();
        List<TransactionRegistry> registryList = new ArrayList<>();
        List<String> successTransactionIds = new ArrayList<>();
        int errorCountInChunk = 0;
        int duplicateCountInChunk = 0;

        // Collect all potential transactionIds from successful entries in this chunk
        List<String> candidateTxnIds = new ArrayList<>();
        Long ownerUserId = null;
        for (TradeWrapper wrapper : chunk) {
            if (wrapper.getTradeTransaction() != null) {
                candidateTxnIds.add(wrapper.getTradeTransaction().getTransactionId());
                if (ownerUserId == null && wrapper.getTradeTransaction().getMetaData() != null) {
                    ownerUserId = ownerUserId(wrapper.getTradeTransaction().getMetaData());
                }
            }
        }

        // Query REGISTRY in BULK to find already existing transactionIds for this user
        java.util.Set<String> existingDbTxnIds = new java.util.HashSet<>();
        if (!candidateTxnIds.isEmpty()) {
            existingDbTxnIds.addAll(
                transactionRegistryRepository.findExistingTransactionIdsForOwner(candidateTxnIds, ownerUserId)
            );
            log.info("Bulk duplicate check: found {} existing transactions in registry for {} candidates", 
                    existingDbTxnIds.size(), candidateTxnIds.size());
        }

        for (TradeWrapper wrapper : chunk) {
            // Process validation errors
            if (wrapper.getErrors() != null && !wrapper.getErrors().isEmpty()) {
                errorList.addAll(wrapper.getErrors());
                errorCountInChunk++;
                log.debug("Processed {} validation errors for a record", wrapper.getErrors().size());
            }

            // Process mapped transaction
            if (wrapper.getTradeTransaction() != null) {
                TradeTransaction transaction = wrapper.getTradeTransaction();
                if (existingDbTxnIds.contains(transaction.getTransactionId())) {
                    // It is a duplicate! Create a duplicate error
                    TransactionError error = TransactionError.builder()
                            .metaData(transaction.getMetaData())
                            .recordTrackingId(transaction.getRecordTrackingId())
                            .transactionId(transaction.getTransactionId())
                            .errorField("transactionId")
                            .errorMessage("Duplicate transaction in system success table")
                            .status(ErrorStatus.DUPLICATE)
                            .createdTime(java.time.LocalDateTime.now())
                            .accountNumber(transaction.getAccountNumber())
                            .rowNumber(transaction.getRowNumber())
                            .build();
                    errorList.add(error);
                    duplicateCountInChunk++;
                    log.info("Detected duplicate transactionId={} in database. Row mapped as DUPLICATE error.", transaction.getTransactionId());
                } else {
                    // Genuine success!
                    successList.add(transaction);
                    User user = (transaction.getMetaData() != null) ? transaction.getMetaData().getUser() : null;
                    registryList.add(new TransactionRegistry(transaction.getTransactionId(), user));
                    successTransactionIds.add(transaction.getTransactionId());
                    log.debug("Processed successful transactionId={}", transaction.getTransactionId());
                }
            }
        }

        if (!successList.isEmpty()) {
            log.info("Saving {} successful transactions", successList.size());
            tradeTransactionRepository.saveAll(successList);
        }

        if (!registryList.isEmpty()) {
            log.info("Saving {} registry records", registryList.size());
            transactionErrorRepository.updateStatusByTransactionIdsAndUserId(
                    successTransactionIds,
                    ErrorStatus.RESOLVED,
                    ownerUserId(successList.get(0).getMetaData())
            );
            transactionRegistryRepository.saveAll(registryList);
        }

        if (!errorList.isEmpty()) {
            log.info("Saving {} error records with fingerprint de-duplication", errorList.size());
            List<TransactionError> toSave = new ArrayList<>();
            for (TransactionError err : errorList) {
                if (err.getStatus() == ErrorStatus.FAILED || err.getStatus() == ErrorStatus.INVALID_TRANSACTION_ID) {
                    List<TransactionError> existing = transactionErrorRepository.findExistingActiveErrorForUser(
                            err.getTransactionId(),
                            err.getErrorField(),
                            err.getErrorMessage(),
                            err.getStatus(),
                            ownerUserId(err.getMetaData())
                    );
                    if (!existing.isEmpty()) {
                        TransactionError existingErr = existing.get(0);
                        existingErr.setMetaData(err.getMetaData());
                        existingErr.setRowNumber(err.getRowNumber());
                        existingErr.setAccountNumber(err.getAccountNumber());
                        existingErr.setCreatedTime(java.time.LocalDateTime.now());
                        toSave.add(existingErr);
                        log.info("Fingerprint match found! Updated existing error record id={} with status={} to new fileId={} and rowNumber={}",
                                existingErr.getErrorId(), err.getStatus(), err.getMetaData().getFileId(), err.getRowNumber());
                        continue;
                    }
                }
                toSave.add(err);
            }
            transactionErrorRepository.saveAll(toSave);
        }

        int successCount = 0;
        int errorCount = 0;
        int duplicateCount = 0;
        ExecutionContext context = null;

        if (stepExecution != null && stepExecution.getJobExecution() != null) {
            context = stepExecution.getJobExecution().getExecutionContext();
        }

        if (context != null) {
            successCount = context.getInt("successCount", 0);
            errorCount = context.getInt("errorCount", 0);
            duplicateCount = context.getInt("duplicateCount", 0);
        }

        int newSuccessCount = successCount + successList.size();
        int newErrorCount = errorCount + errorCountInChunk;
        int newDuplicateCount = duplicateCount + duplicateCountInChunk;

        if (context != null) {
            context.putInt("successCount", newSuccessCount);
            context.putInt("errorCount", newErrorCount);
            context.putInt("duplicateCount", newDuplicateCount);
            log.info("Updated successCount to {}, errorCount to {}, duplicateCount to {}", newSuccessCount, newErrorCount, newDuplicateCount);
        }

        // Lively Database Progress Update
        if (stepExecution != null 
                && stepExecution.getJobExecution() != null 
                && stepExecution.getJobExecution().getJobParameters() != null) {
            Long fileMetaId = stepExecution.getJobExecution().getJobParameters().getLong("fileMetaId");
            if (fileMetaId != null) {
                FileLoadMetaData meta = transactionMetaTableRepository.findById(fileMetaId).orElse(null);
                if (meta != null) {
                    meta.setSuccessCount(newSuccessCount);
                    meta.setErrorCount(newErrorCount);
                    meta.setDuplicateCount(newDuplicateCount);
                    meta.setStatus(FileStatus.PROCESSING);
                    transactionMetaTableRepository.save(meta);
                    log.info("Lively database update for metaId={}: successCount={}, errorCount={}, duplicateCount={}", 
                            fileMetaId, newSuccessCount, newErrorCount, newDuplicateCount);
                }
            }
        }

        log.info("write() completed");
    }

    private Long ownerUserId(FileLoadMetaData metaData) {
        return metaData != null && metaData.getUser() != null ? metaData.getUser().getId() : null;
    }
}
