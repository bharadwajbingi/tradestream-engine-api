package com.mphasis.tse.service;
import com.mphasis.tse.dto.*;
import com.mphasis.tse.entity.FileLoadMetaData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IFileService {

    List<TransactionErrorResponse> getAllErrors();
    PageResponse<TransactionErrorResponse> getAllErrorsPage(int page, int size);

    FileUploadResponse uploadFile(MultipartFile file) throws Exception;
    List<FileLoadMetaDataResponse> searchFileLoads(FileSearchRequest request);
    PageResponse<FileLoadMetaDataResponse> searchFileLoadsPage(FileSearchRequest request);
    List<TransactionErrorResponse> searchFileErrors(TransactionErrorSearchRequest request);
    PageResponse<TransactionErrorResponse> searchFileErrorsPage(TransactionErrorSearchRequest request);

    FileLoadMetaDataResponse modifyFileLoadStatus(FileLoadMetaData request);
    void deleteFileLoad(Long id);
    void archiveFileLoad(Long id);
    DashboardMetricsResponse getMetrics();

    List<FileLoadMetaData> getAllFileLoads();
    PageResponse<FileLoadMetaDataResponse> getAllFileLoadsPage(int page, int size);
    void resolveErrorManual(Long errorId);
    void ignoreErrorManual(Long errorId);
    void recalculateFileCompletion(Long fileId);
}
