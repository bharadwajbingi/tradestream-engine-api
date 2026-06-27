package com.mphasis.tse;

import com.mphasis.tse.config.TradeBatchConfig;
import com.mphasis.tse.config.processor.TradeRecordProcessor;
import com.mphasis.tse.config.writer.TradeItemWriter;
import com.mphasis.tse.entity.*;
import com.mphasis.tse.enums.ErrorStatus;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.mapper.TradeTransactionMapper;
import com.mphasis.tse.repository.TransactionErrorRepository;
import com.mphasis.tse.repository.TransactionMainTableRepository;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import com.mphasis.tse.repository.TransactionRegistryRepository;
import com.mphasis.tse.validation.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test covering the full read → process → write pipeline
 * with mocked repositories.
 *
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6
 */
@ExtendWith(MockitoExtension.class)
public class BatchPipelineIntegrationTest {

    @Mock
    private ValidationService validationService;
    @Mock
    private TradeTransactionMapper tradeTransactionMapper;
    @Mock
    private TransactionMainTableRepository mainTableRepository;
    @Mock
    private TransactionMetaTableRepository metaTableRepository;
    @Mock
    private TransactionErrorRepository errorRepository;
    @Mock
    private TransactionRegistryRepository registryRepository;

    @Mock
    private StepExecution stepExecution;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private ExecutionContext executionContext;

    @Captor
    private ArgumentCaptor<List<TradeTransaction>> transactionCaptor;
    @Captor
    private ArgumentCaptor<List<TransactionError>> errorCaptor;
    @Captor
    private ArgumentCaptor<List<TransactionRegistry>> registryCaptor;

    @TempDir
    Path tempDir;

    private TradeRecordProcessor processor;
    private TradeItemWriter writer;
    private TradeBatchConfig batchConfig;

    private FileLoadMetaData metaData;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("owner@example.com");

        metaData = new FileLoadMetaData();
        metaData.setFileId(1L);
        metaData.setFilename("trades.csv");
        metaData.setStatus(FileStatus.PROCESSING);
        metaData.setUser(testUser);

        lenient().when(metaTableRepository.findById(1L)).thenReturn(Optional.of(metaData));
        lenient().when(mainTableRepository.findExistingTransactionIdsForOwner(any(), eq(10L)))
                .thenReturn(Collections.emptyList());

        processor = new TradeRecordProcessor(
                validationService,
                tradeTransactionMapper,
                metaTableRepository,
                1L
        );

        writer = new TradeItemWriter(
                mainTableRepository,
                errorRepository,
                registryRepository,
                metaTableRepository
        );

        batchConfig = new TradeBatchConfig(10, null, processor, writer);
    }

    // ========================================================================
    // Test 1: Reader produces non-null String array from CSV
    // Validates: Requirement 8.1
    // ========================================================================

    @Test
    @DisplayName("Reader: CSV file with header + 1 data row returns non-null String array with correct element count")
    void testReader_producesNonNullStringArrayFromCsv() throws Exception {
        // Create a temporary CSV file with header + 1 valid data row (21 columns)
        File tempFile = tempDir.resolve("test_reader.csv").toFile();
        Files.write(tempFile.toPath(), List.of(
                "transactionId,fileHeaderDate,accountNumber,transactionType,batchLocation,batchNumber,updateBatchDate,relatedFileNumber,actionName,relatedFileKey,doNotReportFlag,explanation,minorAssetsClass,owningPortfolio,posterInitials,transactionSubtype,cashEffect,cashPaidOut,brokerNumber,oldBalance,newBalance",
                "TXN00101,20260520,ACC0001,1,NY,1001,20260520,1234,BUY,998877,N,Purchased Apple Shares,5,501,JD,10,15000.00,15000.00,55,100000.00,115000.00"
        ));

        var reader = batchConfig.transactionReader(tempFile.getAbsolutePath());
        reader.open(new ExecutionContext());

        String[] row = reader.read();

        assertNotNull(row, "Reader should return a non-null String array");
        // The reader adds a row number as the last element (21 data columns + 1 row number = 22)
        assertEquals(22, row.length, "String array should have 22 elements (21 columns + row number)");

        reader.close();
    }

    // ========================================================================
    // Test 2: Processor produces valid TradeWrapper with matching transactionId
    // Validates: Requirement 8.2
    // ========================================================================

    @Test
    @DisplayName("Processor: valid row produces TradeWrapper with non-null TradeTransaction and matching transactionId")
    void testProcessor_producesValidTradeWrapperWithMatchingTransactionId() throws Exception {
        // Create temp CSV and read a row
        File tempFile = tempDir.resolve("test_processor.csv").toFile();
        Files.write(tempFile.toPath(), List.of(
                "transactionId,fileHeaderDate,accountNumber,transactionType,batchLocation,batchNumber,updateBatchDate,relatedFileNumber,actionName,relatedFileKey,doNotReportFlag,explanation,minorAssetsClass,owningPortfolio,posterInitials,transactionSubtype,cashEffect,cashPaidOut,brokerNumber,oldBalance,newBalance",
                "TXN00201,20260520,ACC0002,2,LON,1001,20260520,1235,SELL,998878,N,Sold Microsoft Shares,5,502,AB,20,-8500.00,0.00,56,45000.00,36500.00"
        ));

        var reader = batchConfig.transactionReader(tempFile.getAbsolutePath());
        reader.open(new ExecutionContext());
        String[] row = reader.read();
        reader.close();

        // Mock validation to return no errors
        when(validationService.validate(any(), any())).thenReturn(Collections.emptyList());

        // Mock mapper to return a TradeTransaction with matching transactionId
        TradeTransaction mappedTxn = new TradeTransaction();
        mappedTxn.setTransactionId("TXN00201");
        mappedTxn.setAccountNumber("ACC0002");
        when(tradeTransactionMapper.toEntity(any())).thenReturn(mappedTxn);

        // Process the row
        TradeWrapper wrapper = processor.process(row);

        assertNotNull(wrapper, "Processor should return a non-null TradeWrapper");
        assertNotNull(wrapper.getTradeTransaction(), "TradeWrapper should have a non-null tradeTransaction");
        assertEquals("TXN00201", wrapper.getTradeTransaction().getTransactionId(),
                "TradeTransaction transactionId should match the input CSV value");
        assertTrue(wrapper.getErrors() == null || wrapper.getErrors().isEmpty(),
                "TradeWrapper should have null or empty errors list for valid input");
    }

    // ========================================================================
    // Test 3: Writer calls saveAll on TransactionMainTableRepository with correct count
    // Validates: Requirement 8.3
    // ========================================================================

    @Test
    @DisplayName("Writer: saveAll called on TransactionMainTableRepository with correct number of valid records")
    void testWriter_savesValidRecordsToMainRepository() {
        // Create 2 valid TradeWrappers
        TradeTransaction txn1 = new TradeTransaction();
        txn1.setTransactionId("TXN301");
        txn1.setAccountNumber("ACC301");
        txn1.setMetaData(metaData);

        TradeTransaction txn2 = new TradeTransaction();
        txn2.setTransactionId("TXN302");
        txn2.setAccountNumber("ACC302");
        txn2.setMetaData(metaData);

        TradeWrapper wrapper1 = new TradeWrapper();
        wrapper1.setTradeTransaction(txn1);

        TradeWrapper wrapper2 = new TradeWrapper();
        wrapper2.setTradeTransaction(txn2);

        Chunk<TradeWrapper> chunk = new Chunk<>();
        chunk.add(wrapper1);
        chunk.add(wrapper2);

        // Set up StepExecution mock
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getInt("successCount", 0)).thenReturn(0);
        when(executionContext.getInt("errorCount", 0)).thenReturn(0);
        when(executionContext.getInt("duplicateCount", 0)).thenReturn(0);
        writer.setStepExecution(stepExecution);

        // Write the chunk
        writer.write(chunk);

        // Verify saveAll called exactly once on main repository with 2 records
        verify(mainTableRepository, times(1)).saveAll(transactionCaptor.capture());
        List<TradeTransaction> savedTransactions = transactionCaptor.getValue();
        assertEquals(2, savedTransactions.size(),
                "saveAll should be called with a list of size equal to the number of valid records");
    }

    // ========================================================================
    // Test 4: Writer calls saveAll on TransactionRegistryRepository with correct transactionIds
    // Validates: Requirement 8.4
    // ========================================================================

    @Test
    @DisplayName("Writer: saveAll called on TransactionRegistryRepository with correct transactionIds")
    void testWriter_savesRegistryEntriesWithCorrectTransactionIds() {
        TradeTransaction txn1 = new TradeTransaction();
        txn1.setTransactionId("TXN401");
        txn1.setAccountNumber("ACC401");
        txn1.setMetaData(metaData);

        TradeTransaction txn2 = new TradeTransaction();
        txn2.setTransactionId("TXN402");
        txn2.setAccountNumber("ACC402");
        txn2.setMetaData(metaData);

        TradeWrapper wrapper1 = new TradeWrapper();
        wrapper1.setTradeTransaction(txn1);

        TradeWrapper wrapper2 = new TradeWrapper();
        wrapper2.setTradeTransaction(txn2);

        Chunk<TradeWrapper> chunk = new Chunk<>();
        chunk.add(wrapper1);
        chunk.add(wrapper2);

        // Set up StepExecution mock
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getInt("successCount", 0)).thenReturn(0);
        when(executionContext.getInt("errorCount", 0)).thenReturn(0);
        when(executionContext.getInt("duplicateCount", 0)).thenReturn(0);
        writer.setStepExecution(stepExecution);

        // Write the chunk
        writer.write(chunk);

        // Verify saveAll called on registry repository with correct transactionIds
        verify(registryRepository, times(1)).saveAll(registryCaptor.capture());
        List<TransactionRegistry> savedRegistries = registryCaptor.getValue();
        assertEquals(2, savedRegistries.size(),
                "Registry should have one entry per successfully persisted TradeTransaction");
        assertEquals("TXN401", savedRegistries.get(0).getTransactionId());
        assertEquals("TXN402", savedRegistries.get(1).getTransactionId());
    }

    // ========================================================================
    // Test 5: Writer does NOT call saveAll on TransactionErrorRepository when all records valid
    // Validates: Requirement 8.5
    // ========================================================================

    @Test
    @DisplayName("Writer: saveAll NOT called on TransactionErrorRepository when all records pass validation")
    void testWriter_noErrorSaveWhenAllRecordsValid() {
        TradeTransaction txn = new TradeTransaction();
        txn.setTransactionId("TXN501");
        txn.setAccountNumber("ACC501");
        txn.setMetaData(metaData);

        TradeWrapper wrapper = new TradeWrapper();
        wrapper.setTradeTransaction(txn);

        Chunk<TradeWrapper> chunk = new Chunk<>();
        chunk.add(wrapper);

        // Set up StepExecution mock
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getInt("successCount", 0)).thenReturn(0);
        when(executionContext.getInt("errorCount", 0)).thenReturn(0);
        when(executionContext.getInt("duplicateCount", 0)).thenReturn(0);
        writer.setStepExecution(stepExecution);

        // Write the chunk
        writer.write(chunk);

        // Verify saveAll is NEVER called on error repository
        verify(errorRepository, never()).saveAll(anyList());
    }

    // ========================================================================
    // Test 6: Writer saves error records and excludes errored record from main save
    // Validates: Requirement 8.6
    // ========================================================================

    @Test
    @DisplayName("Writer: error records saved to TransactionErrorRepository and valid records still saved to main repo")
    void testWriter_errorRecordsSavedAndValidRecordsStillPersisted() {
        // Valid record
        TradeTransaction validTxn = new TradeTransaction();
        validTxn.setTransactionId("TXN601");
        validTxn.setAccountNumber("ACC601");
        validTxn.setMetaData(metaData);

        TradeWrapper validWrapper = new TradeWrapper();
        validWrapper.setTradeTransaction(validTxn);

        // Error record (wrapper with errors and no tradeTransaction)
        TransactionError error = TransactionError.builder()
                .transactionId("TXN602")
                .errorField("accountNumber")
                .errorMessage("Must start with ACC")
                .status(ErrorStatus.FAILED)
                .metaData(metaData)
                .createdTime(LocalDateTime.now())
                .rowNumber(3)
                .build();

        TradeWrapper errorWrapper = new TradeWrapper();
        errorWrapper.setErrors(List.of(error));

        Chunk<TradeWrapper> chunk = new Chunk<>();
        chunk.add(validWrapper);
        chunk.add(errorWrapper);

        // Set up StepExecution mock
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getInt("successCount", 0)).thenReturn(0);
        when(executionContext.getInt("errorCount", 0)).thenReturn(0);
        when(executionContext.getInt("duplicateCount", 0)).thenReturn(0);
        writer.setStepExecution(stepExecution);

        // Mock findExistingActiveErrorForUser to return empty (no fingerprint match)
        when(errorRepository.findExistingActiveErrorForUser(
                eq("TXN602"), eq("accountNumber"), eq("Must start with ACC"),
                eq(ErrorStatus.FAILED), eq(10L)))
                .thenReturn(Collections.emptyList());

        // Write the chunk
        writer.write(chunk);

        // Verify error records saved to error repository
        verify(errorRepository, times(1)).saveAll(errorCaptor.capture());
        List<TransactionError> savedErrors = errorCaptor.getValue();
        assertEquals(1, savedErrors.size(), "Should save exactly 1 error record");
        assertEquals("TXN602", savedErrors.get(0).getTransactionId());

        // Verify valid records still saved to main repository (only the valid one)
        verify(mainTableRepository, times(1)).saveAll(transactionCaptor.capture());
        List<TradeTransaction> savedTransactions = transactionCaptor.getValue();
        assertEquals(1, savedTransactions.size(),
                "Only valid records should be saved to main repository");
        assertEquals("TXN601", savedTransactions.get(0).getTransactionId());
    }
}
