package com.mphasis.tse.config.processor;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TradeTransaction;
import com.mphasis.tse.entity.TradeWrapper;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.enums.ErrorStatus;
import com.mphasis.tse.mapper.TradeTransactionMapper;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import com.mphasis.tse.repository.TransactionMainTableRepository;

import com.mphasis.tse.validation.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@StepScope
@Component
public class TradeRecordProcessor implements ItemProcessor<String[], TradeWrapper> {
    private final ValidationService validationService;
    private final TradeTransactionMapper tradeTransactionMapper;
    private final TransactionMainTableRepository transactionMainTableRepository;
    private final TransactionMetaTableRepository transactionMetaTableRepository;
    private final Long fileMetaId;
    private final Set<String> seenTransactionIds = ConcurrentHashMap.newKeySet();
    private FileLoadMetaData fileLoadMetaData;

    public TradeRecordProcessor(
            ValidationService validationService,
            TradeTransactionMapper tradeTransactionMapper,
            TransactionMainTableRepository transactionMainTableRepository,
            TransactionMetaTableRepository transactionMetaTableRepository,
            @Value("#{jobParameters['fileMetaId']}") Long fileMetaId) {
        this.validationService = validationService;
        this.tradeTransactionMapper = tradeTransactionMapper;
        this.transactionMainTableRepository = transactionMainTableRepository;
        this.transactionMetaTableRepository = transactionMetaTableRepository;
        this.fileMetaId = fileMetaId;
    }

    @Override
    public TradeWrapper process(String[] row) {
        TradeWrapper wrapper = new TradeWrapper();

        FileLoadMetaData metadata = currentFileLoadMetaData();
        String recordTrackingId = UUID.randomUUID().toString();
        
        // 1. Get row number from the last index of the array
        int rowNumber = Integer.parseInt(row[row.length - 1]);

        // 2. Validate transactionId itself (invalid if empty, length > 20, or not matching regex)
        String transactionId = row[0];
        boolean isTxnIdInvalid = (transactionId == null || transactionId.trim().isEmpty() 
            || transactionId.length() > 20 || !transactionId.matches("TXN[a-zA-Z0-9]+"));

        if (isTxnIdInvalid) {
            TransactionError error = buildError(metadata, recordTrackingId, transactionId, row,
                    "transactionId",
                    "Invalid Transaction ID: must start with TXN, be alphanumeric and length <= 20",
                    ErrorStatus.INVALID_TRANSACTION_ID,
                    rowNumber);
            wrapper.setErrors(List.of(error));
            return wrapper;
        }

        // 3. Duplicate check against trade_transaction table
        Long userId = (metadata.getUser() != null) ? metadata.getUser().getId() : null;
        boolean duplicateExists = transactionMainTableRepository.existsByTransactionIdForOwner(transactionId, userId);

        if (duplicateExists) {
            TransactionError error = buildError(metadata, recordTrackingId, transactionId, row,
                    "transactionId",
                    "Duplicate transaction in system success table",
                    ErrorStatus.DUPLICATE,
                    rowNumber);
            wrapper.setErrors(List.of(error));
            return wrapper;
        }

        // 4. Duplicate check in the current batch seen IDs
        if (!seenTransactionIds.add(transactionId)) {
            TransactionError error = buildError(metadata, recordTrackingId, transactionId, row,
                    "transactionId",
                    "Duplicate transaction in current batch",
                    ErrorStatus.DUPLICATE,
                    rowNumber);
            wrapper.setErrors(List.of(error));
            return wrapper;
        }

        // 5. Standard validation checks
        List<TransactionError> validationErrors = validationService.validate(row, metadata);
        if (!validationErrors.isEmpty()) {
            for (TransactionError error : validationErrors) {
                error.setRowNumber(rowNumber);
                error.setRecordTrackingId(recordTrackingId);
            }
            wrapper.getErrors().addAll(validationErrors);
            return wrapper;
        }

        // 6. Map and return successful TradeTransaction
        TradeTransaction tradeRecord = tradeTransactionMapper.toEntity(row);
        tradeRecord.setMetaData(metadata);
        tradeRecord.setRecordTrackingId(recordTrackingId);
        tradeRecord.setRowNumber(rowNumber);
        wrapper.setTradeTransaction(tradeRecord);
        return wrapper;
    }

    private FileLoadMetaData currentFileLoadMetaData() {
        if (fileLoadMetaData == null) {
            fileLoadMetaData = transactionMetaTableRepository.findById(fileMetaId)
                    .orElseThrow(() -> new IllegalStateException("File metadata not found for id: " + fileMetaId));
        }
        return fileLoadMetaData;
    }

    private TransactionError buildError(FileLoadMetaData metadata,
                                        String recordTrackingId,
                                        String transactionId,
                                        String[] row,
                                        String errorField,
                                        String errorMessage,
                                        ErrorStatus status,
                                        int rowNumber) {
        return TransactionError.builder()
                .metaData(metadata)
                .recordTrackingId(recordTrackingId)
                .transactionId(transactionId)
                .errorField(errorField)
                .errorMessage(errorMessage)
                .status(status)
                .createdTime(LocalDateTime.now())
                .accountNumber(row.length > 2 ? row[2] : null)
                .rowNumber(rowNumber)
                .build();
    }
}
