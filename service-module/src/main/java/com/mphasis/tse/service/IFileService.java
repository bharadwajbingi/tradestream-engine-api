package com.mphasis.tse.service;
import com.mphasis.tse.dto.*;
import com.mphasis.tse.entity.FileLoadMetaData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IFileService {

    List<TransactionErrorResponse> getAllErrors();

    FileUploadResponse uploadFile(MultipartFile file) throws Exception;
    List<FileLoadMetaDataResponse> searchFileLoads(FileSearchRequest request);
    List<TransactionErrorResponse> searchFileErrors(TransactionErrorSearchRequest request);

    FileLoadMetaDataResponse modifyFileLoadStatus(FileLoadMetaData request);
    void deleteFileLoad(Long id);
    void archiveFileLoad(Long id);
    DashboardMetricsResponse getMetrics();

    List<FileLoadMetaData> getAllFileLoads();
}
