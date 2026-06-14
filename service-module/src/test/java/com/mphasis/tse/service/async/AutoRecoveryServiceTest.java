package com.mphasis.tse.service.async;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoRecoveryService — reverts stuck STARTED/PROCESSING files to PENDING")
class AutoRecoveryServiceTest {

    @Mock
    private TransactionMetaTableRepository repository;

    @InjectMocks
    private AutoRecoveryService service;

    @Test
    @DisplayName("no stuck jobs — does nothing")
    void noStuckFiles_doesNotSave() {
        when(repository.findByStatusIn(any())).thenReturn(Collections.emptyList());

        service.recoverOnStartup();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("reverts STARTED file to PENDING")
    void stuckStarted_revertsToPending() {
        FileLoadMetaData stuck = new FileLoadMetaData();
        stuck.setFileId(1L);
        stuck.setFilename("trades.csv");
        stuck.setStatus(FileStatus.STARTED);

        when(repository.findByStatusIn(any())).thenReturn(List.of(stuck));

        service.recoverOnStartup();

        assertEquals(FileStatus.PENDING, stuck.getStatus());
        verify(repository).save(stuck);
    }

    @Test
    @DisplayName("reverts multiple stuck files")
    void multipleStuck_revertsAll() {
        FileLoadMetaData f1 = new FileLoadMetaData();
        f1.setFileId(1L);
        f1.setFilename("a.csv");
        f1.setStatus(FileStatus.STARTED);

        FileLoadMetaData f2 = new FileLoadMetaData();
        f2.setFileId(2L);
        f2.setFilename("b.csv");
        f2.setStatus(FileStatus.PROCESSING);

        when(repository.findByStatusIn(any())).thenReturn(List.of(f1, f2));

        service.recoverOnStartup();

        assertEquals(FileStatus.PENDING, f1.getStatus());
        assertEquals(FileStatus.PENDING, f2.getStatus());
        verify(repository, times(2)).save(any());
    }
}
