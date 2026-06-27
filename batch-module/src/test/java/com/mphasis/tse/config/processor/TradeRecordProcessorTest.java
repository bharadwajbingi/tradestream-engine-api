package com.mphasis.tse.config.processor;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TradeTransaction;
import com.mphasis.tse.entity.TradeWrapper;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.enums.ErrorStatus;
import com.mphasis.tse.mapper.TradeTransactionMapper;
import com.mphasis.tse.repository.TransactionMainTableRepository;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import com.mphasis.tse.validation.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TradeRecordProcessor covering:
 * - Invalid transaction ID detection
 * - Duplicate transaction detection
 * - Validation error propagation
 * - Successful mapping of valid rows
 *
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5
 */
@ExtendWith(MockitoExtension.class)
class TradeRecordProcessorTest {

    @Mock
    private ValidationService validationService;

    @Mock
    private TradeTransactionMapper tradeTransactionMapper;

    @Mock
    private TransactionMainTableRepository transactionMainTableRepository;

    @Mock
    private TransactionMetaTableRepository transactionMetaTableRepository;

    private TradeRecordProcessor processor;

    private FileLoadMetaData fileLoadMetaData;

    private static final Long FILE_META_ID = 1L;

    /**
     * Valid 22-element row: 21 trade fields + row number at index 21.
     */
    private static String[] validRow() {
        return new String[]{
                "TXN12345",   // 0: transactionId
                "20240101",   // 1: fileHeaderDate
                "ACC123",     // 2: accountNumber
                "1",          // 3: transactionType
                "NYC",        // 4: batchLocation
                "100",        // 5: batchNumber
                "20240101",   // 6: updateBatchDate
                "5",          // 7: relatedFileNumber
                "BUY",        // 8: actionName
                "12345",      // 9: relatedFileKey
                "Y",          // 10: doNotReportFlag
                "Test",       // 11: explanation
                "3",          // 12: minorAssetsClass
                "1001",       // 13: owningPortfolio
                "JD",         // 14: posterInitials
                "2",          // 15: transactionSubtype
                "123.45",     // 16: cashEffect
                "50.00",      // 17: cashPaidOut
                "7",          // 18: brokerNumber
                "1000.00",    // 19: oldBalance
                "1123.45",    // 20: newBalance
                "1"           // 21: rowNumber
        };
    }

    @BeforeEach
    void setUp() {
        fileLoadMetaData = new FileLoadMetaData();
        fileLoadMetaData.setFileId(FILE_META_ID);
        fileLoadMetaData.setFilename("test_trades.csv");
        fileLoadMetaData.setUploadTime(LocalDateTime.now());

        when(transactionMetaTableRepository.findById(FILE_META_ID))
                .thenReturn(Optional.of(fileLoadMetaData));

        processor = new TradeRecordProcessor(
                validationService,
                tradeTransactionMapper,
                transactionMetaTableRepository,
                FILE_META_ID
        );
    }

    // ========== Invalid Transaction ID Tests (Requirement 7.1) ==========

    @Test
    @DisplayName("Empty transaction ID returns INVALID_TRANSACTION_ID error")
    void process_emptyTransactionId_returnsInvalidTransactionIdError() throws Exception {
        String[] row = validRow();
        row[0] = "";

        TradeWrapper result = processor.process(row);

        assertThat(result).isNotNull();
        assertThat(result.getTradeTransaction()).isNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getStatus()).isEqualTo(ErrorStatus.INVALID_TRANSACTION_ID);
    }

    @Test
    @DisplayName("Transaction ID without TXN prefix returns INVALID_TRANSACTION_ID error")
    void process_noTxnPrefix_returnsInvalidTransactionIdError() throws Exception {
        String[] row = validRow();
        row[0] = "ABC123";

        TradeWrapper result = processor.process(row);

        assertThat(result).isNotNull();
        assertThat(result.getTradeTransaction()).isNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getStatus()).isEqualTo(ErrorStatus.INVALID_TRANSACTION_ID);
    }

    @Test
    @DisplayName("Transaction ID exceeding 20 characters returns INVALID_TRANSACTION_ID error")
    void process_tooLongTransactionId_returnsInvalidTransactionIdError() throws Exception {
        String[] row = validRow();
        row[0] = "TXN123456789012345678"; // 21 chars total (TXN + 18 chars)

        TradeWrapper result = processor.process(row);

        assertThat(result).isNotNull();
        assertThat(result.getTradeTransaction()).isNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getStatus()).isEqualTo(ErrorStatus.INVALID_TRANSACTION_ID);
    }

    // ========== Duplicate Detection Tests (Requirement 7.2) ==========

    @Test
    @DisplayName("Duplicate transaction ID returns DUPLICATE error on second call")
    void process_duplicateTransactionId_returnsDuplicateError() throws Exception {
        String[] row1 = validRow();
        String[] row2 = validRow();
        row2[21] = "2"; // different row number

        // Mock validation to return no errors for the first call
        when(validationService.validate(any(String[].class), any(FileLoadMetaData.class)))
                .thenReturn(Collections.emptyList());

        // Mock mapper for the first call
        TradeTransaction mappedTransaction = new TradeTransaction();
        mappedTransaction.setTransactionId("TXN12345");
        when(tradeTransactionMapper.toEntity(any(String[].class))).thenReturn(mappedTransaction);

        // First call should succeed (no DUPLICATE error)
        TradeWrapper firstResult = processor.process(row1);
        assertThat(firstResult.getErrors()).isEmpty();
        assertThat(firstResult.getTradeTransaction()).isNotNull();

        // Second call with same transaction ID should return DUPLICATE
        TradeWrapper secondResult = processor.process(row2);
        assertThat(secondResult).isNotNull();
        assertThat(secondResult.getErrors()).hasSize(1);
        assertThat(secondResult.getErrors().get(0).getStatus()).isEqualTo(ErrorStatus.DUPLICATE);
    }

    // ========== Validation Error Propagation Tests (Requirement 7.3) ==========

    @Test
    @DisplayName("Validation errors are propagated with rowNumber and recordTrackingId set")
    void process_validationErrors_propagatedWithMetadata() throws Exception {
        String[] row = validRow();
        row[0] = "TXN99999"; // unique valid ID to avoid duplicate

        // Create mock validation errors
        TransactionError error1 = new TransactionError();
        error1.setErrorField("accountNumber");
        error1.setErrorMessage("Mandatory field missing");
        error1.setStatus(ErrorStatus.FAILED);

        TransactionError error2 = new TransactionError();
        error2.setErrorField("cashEffect");
        error2.setErrorMessage("Invalid decimal value");
        error2.setStatus(ErrorStatus.FAILED);

        when(validationService.validate(any(String[].class), any(FileLoadMetaData.class)))
                .thenReturn(List.of(error1, error2));

        TradeWrapper result = processor.process(row);

        assertThat(result).isNotNull();
        assertThat(result.getTradeTransaction()).isNull();
        assertThat(result.getErrors()).hasSize(2);

        // Verify rowNumber and recordTrackingId are set on each error
        for (TransactionError error : result.getErrors()) {
            assertThat(error.getRowNumber()).isEqualTo(1);
            assertThat(error.getRecordTrackingId()).isNotNull();
            assertThat(error.getRecordTrackingId()).isNotEmpty();
        }
    }

    // ========== Successful Mapping Tests (Requirement 7.4) ==========

    @Test
    @DisplayName("Valid row produces TradeWrapper with non-null tradeTransaction and metadata set")
    void process_validRow_returnsSuccessfulTradeWrapper() throws Exception {
        String[] row = validRow();
        row[0] = "TXN67890"; // unique valid ID

        // Mock validation to return no errors
        when(validationService.validate(any(String[].class), any(FileLoadMetaData.class)))
                .thenReturn(Collections.emptyList());

        // Mock mapper to return a TradeTransaction
        TradeTransaction mappedTransaction = new TradeTransaction();
        mappedTransaction.setTransactionId("TXN67890");
        when(tradeTransactionMapper.toEntity(any(String[].class))).thenReturn(mappedTransaction);

        TradeWrapper result = processor.process(row);

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getTradeTransaction()).isNotNull();
        assertThat(result.getTradeTransaction().getMetaData()).isEqualTo(fileLoadMetaData);
        assertThat(result.getTradeTransaction().getRecordTrackingId()).isNotNull();
        assertThat(result.getTradeTransaction().getRecordTrackingId()).isNotEmpty();
        assertThat(result.getTradeTransaction().getRowNumber()).isEqualTo(1);
    }

    // ========== Mock Setup Verification (Requirement 7.5) ==========

    @Test
    @DisplayName("TransactionMetaTableRepository.findById returns valid FileLoadMetaData")
    void mockSetup_findByIdReturnsValidMetaData() throws Exception {
        String[] row = validRow();
        row[0] = "TXN11111"; // unique valid ID

        when(validationService.validate(any(String[].class), any(FileLoadMetaData.class)))
                .thenReturn(Collections.emptyList());

        TradeTransaction mappedTransaction = new TradeTransaction();
        mappedTransaction.setTransactionId("TXN11111");
        when(tradeTransactionMapper.toEntity(any(String[].class))).thenReturn(mappedTransaction);

        TradeWrapper result = processor.process(row);

        // Verify the processor successfully retrieved metadata (no exception thrown)
        assertThat(result).isNotNull();
        assertThat(result.getTradeTransaction()).isNotNull();
        assertThat(result.getTradeTransaction().getMetaData()).isNotNull();
        assertThat(result.getTradeTransaction().getMetaData().getFileId()).isEqualTo(FILE_META_ID);
        assertThat(result.getTradeTransaction().getMetaData().getFilename()).isEqualTo("test_trades.csv");
    }
}
