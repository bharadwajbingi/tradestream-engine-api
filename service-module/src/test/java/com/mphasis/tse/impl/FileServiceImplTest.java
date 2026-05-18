package com.mphasis.tse.impl;

import com.mphasis.tse.dto.*;
import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.enums.ErrorStatus;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.exception.FileNotFoundException;
import com.mphasis.tse.mapper.FileLoadMetaDataMapper;
import com.mphasis.tse.mapper.TransactionErrorMapper;
import com.mphasis.tse.repository.*;
import com.mphasis.tse.service.async.AsyncProcessingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class FileServiceImplTest {

    @Mock private TransactionMetaTableRepository metaRepo;
    @Mock private TransactionErrorRepository errorRepo;
    @Mock private AsyncProcessingService asyncService;
    @Mock private TransactionMainTableRepository mainRepo;
    @Mock private TradeArchiveRepository archiveRepo;
    @Mock private TransactionErrorMapper errorMapper;
    @Mock private FileLoadMetaDataMapper metaMapper;

    @InjectMocks
    private FileServiceImpl fileService;


    @BeforeEach
    void init() throws Exception {
        Field field = FileServiceImpl.class.getDeclaredField("tempDir");
        field.setAccessible(true);
        field.set(fileService, System.getProperty("java.io.tmpdir"));
    }


    @Test
    void testGetAllErrors() {
        when(errorRepo.findAll()).thenReturn(List.of(new TransactionError()));
        when(errorMapper.toDtoList(any())).thenReturn(List.of(new TransactionErrorResponse()));

        assertEquals(1, fileService.getAllErrors().size());
    }


    @Test
    void testGetMetrics() {
        when(metaRepo.count()).thenReturn(1L);
        when(mainRepo.count()).thenReturn(2L);
        when(errorRepo.countDistinctByStatus(ErrorStatus.FAILED)).thenReturn(3L);

        DashboardMetricsResponse res = fileService.getMetrics();

        assertEquals(1, res.getTotalFiles());
        assertEquals(2, res.getSuccessRecords());
        assertEquals(3, res.getErrorRecords());
    }


    @Test
    void testModifyFileLoadStatus_success() {
        FileLoadMetaData meta = new FileLoadMetaData();
        meta.setFileId(1L);

        when(metaRepo.findById(1L)).thenReturn(Optional.of(meta));
        when(metaRepo.save(any())).thenReturn(meta);
        when(metaMapper.toResponse(any())).thenReturn(new FileLoadMetaDataResponse());

        FileLoadMetaData req = new FileLoadMetaData();
        req.setFileId(1L);
        req.setStatus(FileStatus.STARTED);

        assertNotNull(fileService.modifyFileLoadStatus(req));
    }

    @Test
    void testModifyFileLoadStatus_exception() {
        when(metaRepo.findById(anyLong())).thenReturn(Optional.empty());

        FileLoadMetaData req = new FileLoadMetaData();
        req.setFileId(99L);

        assertThrows(RuntimeException.class,
                () -> fileService.modifyFileLoadStatus(req));
    }


    @Test
    void testDeleteFileLoad_success() {
        FileLoadMetaData meta = new FileLoadMetaData();
        when(metaRepo.findById(1L)).thenReturn(Optional.of(meta));

        fileService.deleteFileLoad(1L);

        assertEquals(FileStatus.DELETED, meta.getStatus());
    }

    @Test
    void testDeleteFileLoad_exception() {
        when(metaRepo.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class,
                () -> fileService.deleteFileLoad(1L));
    }


    @Test
    void testArchiveFileLoad_success() {
        FileLoadMetaData meta = new FileLoadMetaData();
        when(metaRepo.findById(1L)).thenReturn(Optional.of(meta));

        fileService.archiveFileLoad(1L);

        verify(archiveRepo).archiveByFileId(1L);
        verify(mainRepo).deleteByFileId(1L);
        assertEquals(FileStatus.ARCHIVED, meta.getStatus());
    }

    @Test
    void testArchiveFileLoad_exception() {
        when(metaRepo.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> fileService.archiveFileLoad(1L));
    }


    @Test
    void testUploadFile_success() throws Exception {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("file.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));

        FileLoadMetaData meta = new FileLoadMetaData();
        meta.setFileId(1L);

        when(metaRepo.save(any())).thenReturn(meta);

        FileUploadResponse res = fileService.uploadFile(file);

        assertEquals("STARTED", res.getStatus());
        verify(asyncService).process(any(), any());
    }

    @Test
    void testUploadFile_nullFile() {
        assertThrows(RuntimeException.class,
                () -> fileService.uploadFile(null));
    }

    @Test
    void testUploadFile_emptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> fileService.uploadFile(file));
    }

    @Test
    void testUploadFile_invalidExtension() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("file.txt");

        assertThrows(RuntimeException.class,
                () -> fileService.uploadFile(file));
    }

    @Test
    void testUploadFile_filenameNull() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> fileService.uploadFile(file));
    }

    @Test
    void testUploadFile_uppercaseCSV() throws Exception {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("file.CSV");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));

        FileLoadMetaData meta = new FileLoadMetaData();
        meta.setFileId(1L);

        when(metaRepo.save(any())).thenReturn(meta);

        FileUploadResponse res = fileService.uploadFile(file);

        assertEquals("STARTED", res.getStatus());
    }


    @Test
    void testSearchFileErrors_allBranches() {
        TransactionErrorSearchRequest req = new TransactionErrorSearchRequest();
        req.setStatus("FAILED");
        req.setAccountNumber("123");
        req.setErrorField("field");
        req.setTransactionId("tx");
        req.setFileLoadId(1L);

        when(errorRepo.findAll(
                ArgumentMatchers.<Specification<TransactionError>>any()
        )).thenReturn(List.of(new TransactionError()));

        when(errorMapper.toDtoList(any()))
                .thenReturn(List.of(new TransactionErrorResponse()));

        assertEquals(1, fileService.searchFileErrors(req).size());
    }

    @Test
    void testSearchFileErrors_noFilters() {
        TransactionErrorSearchRequest req = new TransactionErrorSearchRequest();

        when(errorRepo.findAll(
                ArgumentMatchers.<Specification<TransactionError>>any()
        )).thenReturn(List.of(new TransactionError()));

        when(errorMapper.toDtoList(any()))
                .thenReturn(List.of(new TransactionErrorResponse()));

        assertEquals(1, fileService.searchFileErrors(req).size());
    }

    @Test
    void testSearchFileErrors_invalidStatus() {
        TransactionErrorSearchRequest req = new TransactionErrorSearchRequest();
        req.setStatus("INVALID");

        assertThrows(IllegalArgumentException.class,
                () -> fileService.searchFileErrors(req));
    }


    @Test
    void testSearchFileLoads_allBranches() {
        FileSearchRequest req = new FileSearchRequest();
        req.setFileId(1L);
        req.setFilename("file.csv");
        req.setStatus("STARTED");
        req.setStartDate(LocalDateTime.now());
        req.setEndDate(LocalDateTime.now());

        when(metaRepo.findAll(
                ArgumentMatchers.<Specification<FileLoadMetaData>>any()
        )).thenReturn(List.of(new FileLoadMetaData()));

        when(metaMapper.toResponseList(any()))
                .thenReturn(List.of(new FileLoadMetaDataResponse()));

        assertEquals(1, fileService.searchFileLoads(req).size());
    }


    @Test
    void testSearchFileLoads_startDateOnly() {
        FileSearchRequest req = new FileSearchRequest();
        req.setStartDate(LocalDateTime.now());
        req.setEndDate(null);

        when(metaRepo.findAll(
                ArgumentMatchers.<Specification<FileLoadMetaData>>any()
        )).thenReturn(List.of(new FileLoadMetaData()));

        when(metaMapper.toResponseList(any()))
                .thenReturn(List.of(new FileLoadMetaDataResponse()));

        assertEquals(1, fileService.searchFileLoads(req).size());
    }

    @Test
    void testSearchFileLoads_endDateOnly() {
        FileSearchRequest req = new FileSearchRequest();
        req.setStartDate(null);
        req.setEndDate(LocalDateTime.now());

        when(metaRepo.findAll(
                ArgumentMatchers.<Specification<FileLoadMetaData>>any()
        )).thenReturn(List.of(new FileLoadMetaData()));

        when(metaMapper.toResponseList(any()))
                .thenReturn(List.of(new FileLoadMetaDataResponse()));

        assertEquals(1, fileService.searchFileLoads(req).size());
    }


    @Test
    void testGetAllFileLoads_filtering() {
        FileLoadMetaData f1 = new FileLoadMetaData();
        f1.setStatus(FileStatus.STARTED);

        FileLoadMetaData f2 = new FileLoadMetaData();
        f2.setStatus(FileStatus.DELETED);

        FileLoadMetaData f3 = new FileLoadMetaData();
        f3.setStatus(FileStatus.ARCHIVED);

        when(metaRepo.findAll()).thenReturn(List.of(f1, f2, f3));

        assertEquals(1, fileService.getAllFileLoads().size());
    }

    @Test
    void testGetAllFileLoads_allFiltered() {
        FileLoadMetaData f1 = new FileLoadMetaData();
        f1.setStatus(FileStatus.DELETED);

        FileLoadMetaData f2 = new FileLoadMetaData();
        f2.setStatus(FileStatus.ARCHIVED);

        when(metaRepo.findAll()).thenReturn(List.of(f1, f2));

        assertTrue(fileService.getAllFileLoads().isEmpty());
    }
}