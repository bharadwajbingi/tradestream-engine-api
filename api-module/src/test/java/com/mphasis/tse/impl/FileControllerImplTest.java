package com.mphasis.tse.impl;

import com.mphasis.tse.dto.*;
import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.mapper.FileLoadMetaDataMapper;
import com.mphasis.tse.mapper.TransactionErrorMapper;
import com.mphasis.tse.service.IFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileControllerImplTest {

    @Mock
    private IFileService fileService;

    @Mock
    private TransactionErrorMapper transactionErrorMapper;

    @Mock
    private FileLoadMetaDataMapper fileLoadMetaDataMapper;

    @InjectMocks
    private FileControllerImpl controller;

    @BeforeEach
    void setup() {

    }

    @Test
    void getAllErrors_success() {
        List<TransactionErrorResponse> errors =
                List.of(new TransactionErrorResponse());

        when(fileService.getAllErrors()).thenReturn(errors);

        ResponseEntity<ApiResponse<List<TransactionErrorResponse>>> response =
                controller.getAllErrors();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
        assertEquals(errors, response.getBody().getData());
        verify(fileService).getAllErrors();
    }

    @Test
    void searchFiles_success() {
        FileSearchRequest request = new FileSearchRequest();
        List<FileLoadMetaDataResponse> result =
                List.of(new FileLoadMetaDataResponse());

        when(fileService.searchFileLoads(request)).thenReturn(result);

        ResponseEntity<ApiResponse<List<FileLoadMetaDataResponse>>> response =
                controller.searchFiles(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(result, response.getBody().getData());
        verify(fileService).searchFileLoads(request);
    }

    @Test
    void searchFileErrors_success() {
        TransactionErrorSearchRequest request =
                new TransactionErrorSearchRequest();

        List<TransactionErrorResponse> result =
                List.of(new TransactionErrorResponse());

        when(fileService.searchFileErrors(request)).thenReturn(result);

        ResponseEntity<ApiResponse<List<TransactionErrorResponse>>> response =
                controller.searchFileErrors(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(result, response.getBody().getData());
        verify(fileService).searchFileErrors(request);
    }

    @Test
    void modifyFileLoadStatus_success() {
        FileLoadMetaData request = new FileLoadMetaData();
        request.setFileId(1L);
        request.setStatus(FileStatus.PROCESSING);

        FileLoadMetaDataResponse serviceResponse =
                new FileLoadMetaDataResponse();
        serviceResponse.setId(1L);
        serviceResponse.setStatus(String.valueOf(FileStatus.COMPLETED));

        when(fileService.modifyFileLoadStatus(request))
                .thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<FileLoadMetaDataResponse>> response =
                controller.modifyFileLoadStatus(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody().getData());
        verify(fileService).modifyFileLoadStatus(request);
    }

    @Test
    void deleteFileLoad_success() {
        Long id = 10L;

        doNothing().when(fileService).deleteFileLoad(id);

        ResponseEntity<ApiResponse<Void>> response =
                controller.deleteFileLoad(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody().getData());
        verify(fileService).deleteFileLoad(id);
    }

    @Test
    void archiveFileLoad_success() {
        Long id = 20L;

        doNothing().when(fileService).archiveFileLoad(id);

        ResponseEntity<ApiResponse<Void>> response =
                controller.archiveFileLoad(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody().getData());
        verify(fileService).archiveFileLoad(id);
    }

    @Test
    void uploadFile_success() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt",
                        "text/plain", "content".getBytes());

        FileUploadResponse uploadResponse = new FileUploadResponse();

        when(fileService.uploadFile(file))
                .thenReturn(uploadResponse);

        ResponseEntity<ApiResponse<FileUploadResponse>> response =
                controller.uploadFile(file);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(uploadResponse, response.getBody().getData());
        verify(fileService).uploadFile(file);
    }

    @Test
    void getDashboardMetrics_success() {

        DashboardMetricsResponse metricsResponse =
                new DashboardMetricsResponse();

        when(fileService.getMetrics()).thenReturn(metricsResponse);

        ResponseEntity<ApiResponse<DashboardMetricsResponse>> response =
                controller.getDashboardMetrics();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals("OK", response.getBody().getStatus());
        assertEquals(
                "Dashboard metrics retrieved successfully",
                response.getBody().getMessage()
        );
        assertEquals(metricsResponse, response.getBody().getData());
        verify(fileService).getMetrics();
    }

    @Test
    void getAllFileLoads_success() {
        List<FileLoadMetaData> entities =
                List.of(new FileLoadMetaData());

        List<FileLoadMetaDataResponse> responses =
                List.of(new FileLoadMetaDataResponse());

        when(fileService.getAllFileLoads()).thenReturn(entities);
        when(fileLoadMetaDataMapper.toResponseList(entities))
                .thenReturn(responses);

        ResponseEntity<ApiResponse<List<FileLoadMetaDataResponse>>> response =
                controller.getAllFileLoads();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responses, response.getBody().getData());

        verify(fileService).getAllFileLoads();
        verify(fileLoadMetaDataMapper).toResponseList(entities);
    }
}