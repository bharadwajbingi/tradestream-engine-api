package com.mphasis.tse.service;

import com.mphasis.tse.dto.TradeExportProjection;
import com.mphasis.tse.repository.ExportJobRepository;
import com.mphasis.tse.repository.TradeArchiveRepository;
import com.mphasis.tse.repository.TransactionMainTableRepository;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ExportService — the service that routes streaming queries
 * for the async CSV export pipeline.
 *
 * Key behaviours verified:
 *  - When fileId is provided, query by file (not by date range)
 *  - When userId is present, scope query to that user
 *  - When neither fileId nor userId, query all (admin path)
 *  - Stream consumer is always called (even for empty results)
 *  - Archive queries follow the same routing logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExportService — streaming query routing")
class ExportServiceTest {

    @Mock
    private TransactionMainTableRepository mainRepo;

    @Mock
    private TradeArchiveRepository archiveRepo;

    @Mock
    private TransactionMetaTableRepository metaRepo;

    @Mock
    private ExportJobRepository exportJobRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private ExportService exportService;

    private static final String START_DATE = "2024-01-01";
    private static final String END_DATE   = "2024-12-31";
    private static final Long   FILE_ID    = 42L;
    private static final Long   USER_ID    = 99L;

    // -----------------------------------------------------------------------
    // streamActiveTransactions — routing by fileId
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("when fileId and userId provided — queries by fileId + userId")
    void streamActive_withFileIdAndUserId_queriesByFileIdAndUser() {
        when(mainRepo.streamByFileIdAndUserId(FILE_ID, USER_ID))
                .thenReturn(Stream.empty());

        AtomicBoolean consumerCalled = new AtomicBoolean(false);
        exportService.streamActiveTransactions(
                START_DATE, END_DATE, FILE_ID, Optional.of(USER_ID),
                stream -> consumerCalled.set(true));

        assertTrue(consumerCalled.get(), "Stream consumer must be called");
        verify(mainRepo).streamByFileIdAndUserId(FILE_ID, USER_ID);
        verify(mainRepo, never()).streamByFileHeaderDateBetweenAndUserId(any(), any(), any());
    }

    @Test
    @DisplayName("when fileId provided but no userId — queries by fileId only")
    void streamActive_withFileIdNoUserId_queriesByFileIdOnly() {
        when(mainRepo.streamByFileId(FILE_ID)).thenReturn(Stream.empty());

        exportService.streamActiveTransactions(
                START_DATE, END_DATE, FILE_ID, Optional.empty(),
                stream -> {});

        verify(mainRepo).streamByFileId(FILE_ID);
        verify(mainRepo, never()).streamByFileIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("when no fileId but userId provided — queries by date range + userId")
    void streamActive_noFileId_withUserId_queriesByDateAndUser() {
        when(mainRepo.streamByFileHeaderDateBetweenAndUserId(START_DATE, END_DATE, USER_ID))
                .thenReturn(Stream.empty());

        exportService.streamActiveTransactions(
                START_DATE, END_DATE, null, Optional.of(USER_ID),
                stream -> {});

        verify(mainRepo).streamByFileHeaderDateBetweenAndUserId(START_DATE, END_DATE, USER_ID);
        verify(mainRepo, never()).streamByFileHeaderDateBetween(any(), any());
    }

    @Test
    @DisplayName("when no fileId and no userId — queries all by date range")
    void streamActive_noFileId_noUserId_queriesAllByDate() {
        when(mainRepo.streamByFileHeaderDateBetween(START_DATE, END_DATE))
                .thenReturn(Stream.empty());

        exportService.streamActiveTransactions(
                START_DATE, END_DATE, null, Optional.empty(),
                stream -> {});

        verify(mainRepo).streamByFileHeaderDateBetween(START_DATE, END_DATE);
        verify(mainRepo, never()).streamByFileHeaderDateBetweenAndUserId(any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // streamActiveTransactions — consumer receives the stream
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("stream consumer receives the stream returned by repository")
    void streamActive_consumerReceivesStream() {
        TradeExportProjection mockRow = mock(TradeExportProjection.class);
        when(mainRepo.streamByFileHeaderDateBetween(START_DATE, END_DATE))
                .thenReturn(Stream.of(mockRow));

        List<TradeExportProjection> captured = new java.util.ArrayList<>();
        exportService.streamActiveTransactions(
                START_DATE, END_DATE, null, Optional.empty(),
                stream -> stream.forEach(captured::add));

        assertEquals(1, captured.size());
        assertSame(mockRow, captured.get(0));
    }

    // -----------------------------------------------------------------------
    // streamArchivedTransactions — routing
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("archived: when fileId provided — queries archive by fileId")
    void streamArchived_withFileId_queriesArchiveByFileId() {
        when(archiveRepo.streamByFileId(FILE_ID)).thenReturn(Stream.empty());

        exportService.streamArchivedTransactions(
                START_DATE, END_DATE, FILE_ID, Optional.of(USER_ID),
                stream -> {});

        verify(archiveRepo).streamByFileId(FILE_ID);
        verify(archiveRepo, never()).streamByFileHeaderDateBetween(any(), any());
    }

    @Test
    @DisplayName("archived: no fileId + userId + non-empty fileIds — queries by fileIds and date")
    void streamArchived_noFileId_withUserId_withFiles_queriesByFileIdsAndDate() {
        List<Long> fileIds = List.of(1L, 2L, 3L);
        when(metaRepo.findActiveFileIdsByUserId(USER_ID)).thenReturn(fileIds);
        when(archiveRepo.streamByFileIdInAndFileHeaderDateBetween(fileIds, START_DATE, END_DATE))
                .thenReturn(Stream.empty());

        exportService.streamArchivedTransactions(
                START_DATE, END_DATE, null, Optional.of(USER_ID),
                stream -> {});

        verify(archiveRepo).streamByFileIdInAndFileHeaderDateBetween(fileIds, START_DATE, END_DATE);
    }

    @Test
    @DisplayName("archived: no fileId + userId + empty fileIds — consumer gets empty stream")
    void streamArchived_noFileId_withUserId_noFiles_consumerGetsEmptyStream() {
        when(metaRepo.findActiveFileIdsByUserId(USER_ID)).thenReturn(List.of());

        AtomicBoolean consumerCalled = new AtomicBoolean(false);
        exportService.streamArchivedTransactions(
                START_DATE, END_DATE, null, Optional.of(USER_ID),
                stream -> consumerCalled.set(true));

        assertTrue(consumerCalled.get(), "Consumer must be called even with empty stream");
        verify(archiveRepo, never()).streamByFileIdInAndFileHeaderDateBetween(any(), any(), any());
    }

    @Test
    @DisplayName("archived: no fileId and no userId — queries all archive by date")
    void streamArchived_noFileId_noUserId_queriesAllByDate() {
        when(archiveRepo.streamByFileHeaderDateBetween(START_DATE, END_DATE))
                .thenReturn(Stream.empty());

        exportService.streamArchivedTransactions(
                START_DATE, END_DATE, null, Optional.empty(),
                stream -> {});

        verify(archiveRepo).streamByFileHeaderDateBetween(START_DATE, END_DATE);
    }
}
