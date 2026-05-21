package com.mphasis.tse.config.writer;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TradeTransaction;
import com.mphasis.tse.entity.TradeWrapper;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.entity.User;
import com.mphasis.tse.enums.ErrorStatus;
import com.mphasis.tse.repository.TransactionErrorRepository;
import com.mphasis.tse.repository.TransactionMainTableRepository;
import com.mphasis.tse.repository.TransactionRegistryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeItemWriterTest {
    @Mock
    private TransactionMainTableRepository tradeTransactionRepository;
    @Mock
    private TransactionErrorRepository transactionErrorRepository;
    @Mock
    private TransactionRegistryRepository transactionRegistryRepository;
    @Mock
    private StepExecution stepExecution;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private ExecutionContext executionContext;
    @InjectMocks
    private TradeItemWriter writer;

@Test
void testWrite_withSuccessfulTransactions() {

    TradeTransaction txn = new TradeTransaction();
    txn.setTransactionId("TXN123");
    txn.setAccountNumber("ACC1001");
    txn.setTransactionType(1);
    txn.setMetaData(metaDataForUser(42L));
    TradeWrapper wrapper = new TradeWrapper();
    wrapper.setTradeTransaction(txn);
    Chunk<TradeWrapper> chunk = new Chunk<>();
    chunk.add(wrapper);

    when(stepExecution.getJobExecution()).thenReturn(jobExecution);
    when(jobExecution.getExecutionContext()).thenReturn(executionContext);
    when(executionContext.getInt("successCount", 0)).thenReturn(0);
    when(executionContext.getInt("errorCount", 0)).thenReturn(0);
    writer.setStepExecution(stepExecution);

    writer.write(chunk);

    verify(tradeTransactionRepository).saveAll(anyList());
    verify(transactionRegistryRepository).saveAll(anyList());
    verify(transactionErrorRepository, never()).saveAll(anyList());
    verify(executionContext).putInt("successCount", 1);
}

    @Test
    void testWrite_withErrorsOnly() {

        TransactionError error = new TransactionError();

        error.setErrorMessage("Invalid Transaction");
        TradeWrapper wrapper = new TradeWrapper();
        wrapper.setErrors(List.of(error));
        Chunk<TradeWrapper> chunk = new Chunk<>();
        chunk.add(wrapper);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getInt("successCount", 0)).thenReturn(0);
        when(executionContext.getInt("errorCount", 0)).thenReturn(0);
        writer.setStepExecution(stepExecution);

        writer.write(chunk);

        verify(transactionErrorRepository).saveAll(anyList());
        verify(tradeTransactionRepository, never()).saveAll(anyList());
        verify(transactionRegistryRepository, never()).saveAll(anyList());
        verify(executionContext).putInt("errorCount", 1);
    }

    @Test
    void testWrite_withSuccessAndErrors() {

        TradeTransaction txn = new TradeTransaction();
        txn.setTransactionId("TXN1");
        txn.setMetaData(metaDataForUser(42L));
        TransactionError error = new TransactionError();
        error.setMetaData(metaDataForUser(42L));
        TradeWrapper wrapper = new TradeWrapper();
        wrapper.setTradeTransaction(txn);
        wrapper.setErrors(List.of(error));
        Chunk<TradeWrapper> chunk = new Chunk<>();
        chunk.add(wrapper);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getInt("successCount", 0)).thenReturn(0);
        when(executionContext.getInt("errorCount", 0)).thenReturn(0);
        writer.setStepExecution(stepExecution);

        writer.write(chunk);

        verify(tradeTransactionRepository).saveAll(anyList());
        verify(transactionRegistryRepository).saveAll(anyList());
        verify(transactionErrorRepository).saveAll(anyList());
        verify(transactionErrorRepository)
                .updateStatusByTransactionIdsAndUserId(anyList(), eq(ErrorStatus.RESOLVED), eq(42L));
        verify(executionContext).putInt("successCount", 1);
        verify(executionContext).putInt("errorCount", 1);
    }

    @Test
    void testWrite_withEmptyChunk() {

        Chunk<TradeWrapper> chunk = new Chunk<>();
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getInt("successCount", 0)).thenReturn(0);
        when(executionContext.getInt("errorCount", 0)).thenReturn(0);
        writer.setStepExecution(stepExecution);

        writer.write(chunk);

        verifyNoInteractions(tradeTransactionRepository);
        verifyNoInteractions(transactionErrorRepository);
        verifyNoInteractions(transactionRegistryRepository);
    }

    @Test
    void testWrite_withNullFields() {
        TradeWrapper wrapper = new TradeWrapper();
        Chunk<TradeWrapper> chunk = new Chunk<>();
        chunk.add(wrapper);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getInt("successCount", 0)).thenReturn(0);
        when(executionContext.getInt("errorCount", 0)).thenReturn(0);
        writer.setStepExecution(stepExecution);
        writer.write(chunk);
        verifyNoInteractions(tradeTransactionRepository);
        verifyNoInteractions(transactionErrorRepository);
    }

    @Test
    void testWrite_withNullErrors() {
        TradeTransaction txn = new TradeTransaction();
        txn.setTransactionId("TXN1");
        txn.setMetaData(metaDataForUser(42L));
        TradeWrapper wrapper = new TradeWrapper();
        wrapper.setTradeTransaction(txn);
        wrapper.setErrors(null);
        Chunk<TradeWrapper> chunk = new Chunk<>();
        chunk.add(wrapper);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getInt("successCount", 0)).thenReturn(0);
        when(executionContext.getInt("errorCount", 0)).thenReturn(0);
        writer.setStepExecution(stepExecution);
        writer.write(chunk);

        verify(tradeTransactionRepository).saveAll(anyList());

        verify(transactionErrorRepository, never()).saveAll(anyList());
        verify(executionContext).putInt("errorCount", 0);
    }

    @Test
    void testWrite_withEmptyErrorsList() {
        TradeTransaction txn = new TradeTransaction();
        txn.setTransactionId("TXN1");
        txn.setMetaData(metaDataForUser(42L));
        TradeWrapper wrapper = new TradeWrapper();
        wrapper.setTradeTransaction(txn);
        wrapper.setErrors(Collections.emptyList());
        Chunk<TradeWrapper> chunk = new Chunk<>();
        chunk.add(wrapper);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getInt("successCount", 0)).thenReturn(0);
        when(executionContext.getInt("errorCount", 0)).thenReturn(0);
        writer.setStepExecution(stepExecution);
        writer.write(chunk);
        verify(tradeTransactionRepository).saveAll(anyList());
        verify(transactionErrorRepository, never()).saveAll(anyList());
        verify(executionContext).putInt("errorCount", 0);
    }

    private FileLoadMetaData metaDataForUser(Long userId) {
        FileLoadMetaData metaData = new FileLoadMetaData();
        User user = new User();
        user.setId(userId);
        metaData.setUser(user);
        return metaData;
    }

}
