package com.mphasis.tse.config.processor;

import com.mphasis.tse.config.listener.JobListener;
import com.mphasis.tse.entity.*;
import com.mphasis.tse.mapper.TradeTransactionMapper;
import com.mphasis.tse.repository.TransactionRegistryRepository;
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
    private TransactionRegistryRepository transactionRegistryRepository;
    @Mock
    private JobListener jobListener;
    @InjectMocks
    private TradeRecordProcessor processor;
    private String[] row;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        row = new String[21];
        row[0] = "TXN123";
        row[2] = "ACC123";
        when(jobListener.getFileLoadMetaData()).thenReturn(new FileLoadMetaData());
        when(jobListener.getSeenTransactionIds()).thenReturn(new HashSet<>());
    }

    @Test
    void testDuplicateInRegistry() {
        when(transactionRegistryRepository.existsByTransactionId("TXN123"))
                .thenReturn(true);
        TradeWrapper result = processor.process(row);
        assertNotNull(result.getErrors());
        assertFalse(result.getErrors().isEmpty());
        assertEquals("Duplicate in registry", result.getErrors().get(0).getErrorMessage());
    }

    @Test
    void testDuplicateInCurrentBatch() {
        Set<String> seenIds = new HashSet<>();
        seenIds.add("TXN123");
        when(jobListener.getSeenTransactionIds()).thenReturn(seenIds);
        when(transactionRegistryRepository.existsByTransactionId("TXN123"))
                .thenReturn(false);
        TradeWrapper result = processor.process(row);
        assertNotNull(result.getErrors());
        assertFalse(result.getErrors().isEmpty());
        assertEquals("Duplicate in current batch", result.getErrors().get(0).getErrorMessage());
    }

    @Test
    void testValidationErrors() {
        when(transactionRegistryRepository.existsByTransactionId("TXN123"))
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
        when(transactionRegistryRepository.existsByTransactionId("TXN123"))
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