package com.mphasis.tse.config.processor;

import com.mphasis.tse.entity.*;
import com.mphasis.tse.mapper.TradeTransactionMapper;
import com.mphasis.tse.repository.TransactionMainTableRepository;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import com.mphasis.tse.validation.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
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
    private String[] row;
    private FileLoadMetaData fileLoadMetaData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new TradeRecordProcessor(
                validationService,
                tradeTransactionMapper,
                transactionMainTableRepository,
                transactionMetaTableRepository,
                1L
        );
        row = new String[22];
        row[0] = "TXN123";
        row[2] = "ACC123";
        row[21] = "2";
        fileLoadMetaData = new FileLoadMetaData();
        fileLoadMetaData.setFileId(1L);
        User user = new User();
        user.setId(42L);
        fileLoadMetaData.setUser(user);
        when(transactionMetaTableRepository.findById(1L)).thenReturn(Optional.of(fileLoadMetaData));
    }

    @Test
    void testDuplicateInSystem() {
        when(transactionMainTableRepository.existsByTransactionIdForOwner("TXN123", 42L))
                .thenReturn(true);
        TradeWrapper result = processor.process(row);
        assertNotNull(result.getErrors());
        assertFalse(result.getErrors().isEmpty());
        assertEquals("Duplicate transaction in system success table", result.getErrors().get(0).getErrorMessage());
    }

    @Test
    void testDuplicateInCurrentBatch() {
        when(transactionMainTableRepository.existsByTransactionIdForOwner("TXN123", 42L))
                .thenReturn(false);
        when(validationService.validate(any(), any())).thenReturn(Collections.emptyList());
        when(tradeTransactionMapper.toEntity(any())).thenReturn(new TradeTransaction());
        processor.process(row);
        TradeWrapper result = processor.process(row);
        assertNotNull(result.getErrors());
        assertFalse(result.getErrors().isEmpty());
        assertEquals("Duplicate transaction in current batch", result.getErrors().get(0).getErrorMessage());
    }

    @Test
    void testValidationErrors() {
        when(transactionMainTableRepository.existsByTransactionIdForOwner("TXN123", 42L))
                .thenReturn(false);
        List<TransactionError> errors = List.of(
                TransactionError.builder()
                        .errorMessage("Validation failed")
                        .build()
        );
        when(validationService.validate(any(), any()))
                .thenReturn(errors);
        TradeWrapper result = processor.process(row);
        assertNotNull(result.getErrors());
        assertEquals(1, result.getErrors().size());
        assertEquals("Validation failed", result.getErrors().get(0).getErrorMessage());
    }

    @Test
    void testSuccessfulProcessing() {
        when(transactionMainTableRepository.existsByTransactionIdForOwner("TXN123", 42L))
                .thenReturn(false);
        when(validationService.validate(any(), any()))
                .thenReturn(Collections.emptyList());
        TradeTransaction mockTransaction = new TradeTransaction();
        when(tradeTransactionMapper.toEntity(any()))
                .thenReturn(mockTransaction);
        TradeWrapper result = processor.process(row);
        assertNotNull(result.getTradeTransaction());
        assertEquals(mockTransaction, result.getTradeTransaction());
        assertTrue(result.getErrors().isEmpty());
        verify(tradeTransactionMapper, times(1)).toEntity(any());
    }
}
