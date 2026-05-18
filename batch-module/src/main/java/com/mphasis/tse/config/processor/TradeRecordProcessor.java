package com.mphasis.tse.config.processor;

import com.mphasis.tse.config.listener.JobListener;
import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TradeTransaction;
import com.mphasis.tse.entity.TradeWrapper;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.enums.ErrorStatus;
import com.mphasis.tse.mapper.TradeTransactionMapper;
import com.mphasis.tse.repository.TransactionRegistryRepository;

import com.mphasis.tse.validation.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.mphasis.tse.enums.ErrorStatus.DUPLICATE;

@Slf4j
@Component
public class TradeRecordProcessor implements ItemProcessor<String[], TradeWrapper> {
    private final ValidationService validationService;

    private final TradeTransactionMapper tradeTransactionMapper;
    private final TransactionRegistryRepository transactionRegistryRepository;
    private final JobListener jobListener;

    public TradeRecordProcessor(
            ValidationService validationService,
            TradeTransactionMapper tradeTransactionMapper,
            TransactionRegistryRepository transactionRegistryRepository,
            JobListener jobListener) {
        this.validationService = validationService;
        this.tradeTransactionMapper = tradeTransactionMapper;
        this.transactionRegistryRepository = transactionRegistryRepository;
        this.jobListener = jobListener;
    }

    @Override
    public TradeWrapper process(String[] row) {
        TradeWrapper wrapper = new TradeWrapper();

        FileLoadMetaData fileLoadMetaData = jobListener.getFileLoadMetaData();
        Set<String> seenTransactionId = jobListener.getSeenTransactionIds();
        String transactionId = row[0];
        if (transactionRegistryRepository.existsByTransactionId(transactionId)) {

            wrapper.setErrors(List.of(
                    buildError( row,  "Duplicate in registry", DUPLICATE)
            ));
            return wrapper;
        }
        if (!seenTransactionId.add(transactionId)) {

            wrapper.setErrors(List.of(
                    buildError( row,  "Duplicate in current batch", DUPLICATE)
            ));
            return wrapper;
        }
        List<TransactionError> validationErrors = validationService.validate(row, fileLoadMetaData);
        if (!validationErrors.isEmpty()) {
            wrapper.getErrors().addAll(validationErrors);
            return wrapper;
        }
        TradeTransaction tradeRecord = tradeTransactionMapper.toEntity(row);
        tradeRecord.setMetaData(fileLoadMetaData);
        wrapper.setTradeTransaction(tradeRecord);
        return wrapper;
    }

    private TransactionError buildError(
                                        String [] record,
                                        String message,ErrorStatus status) {
        FileLoadMetaData metaData=jobListener.getFileLoadMetaData();
        return  TransactionError.builder()
                .metaData(metaData)
                .transactionId(record[0])
                .errorField("transactionId")
                .errorMessage(message)
                .status(status)
                .createdTime(LocalDateTime.now())
                .accountNumber(record[2])
                .build();
    }

}


