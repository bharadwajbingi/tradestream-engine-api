package com.mphasis.tse.impl;

import com.mphasis.tse.controller.IFileController;
import com.mphasis.tse.dto.*;
import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.mapper.FileLoadMetaDataMapper;
import com.mphasis.tse.dto.FileLoadMetaDataResponse;
import com.mphasis.tse.mapper.TransactionErrorMapper;
import com.mphasis.tse.service.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FileControllerImpl implements IFileController {

    private final IFileService fileService;
    private final TransactionErrorMapper transactionErrorMapper;
    private final FileLoadMetaDataMapper fileLoadMetaDataMapper;

    @Override
    public ResponseEntity<ApiResponse<List<TransactionErrorResponse>>> getAllErrors() {
        log.debug("GET /file-loads/errors");
        List<TransactionErrorResponse> errors = fileService.getAllErrors();
        return ResponseEntity.ok(
                new ApiResponse<>(
                        HttpStatus.OK.name(),
                        HttpStatus.OK.value(),
                        "All errors retrieved successfully",
                        errors
                )
        );

    }

    @Override
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboardMetrics() {

        log.debug("GET /file/metrics");

        DashboardMetricsResponse metrics = fileService.getMetrics();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        HttpStatus.OK.name(),
                        HttpStatus.OK.value(),
                        "Dashboard metrics retrieved successfully",
                        metrics
                )
        );
    }


    @Override
    public ResponseEntity<ApiResponse<List<FileLoadMetaDataResponse>>> searchFiles(
             FileSearchRequest request) {

        log.info("Search Files started");

        List<FileLoadMetaDataResponse> response = fileService.searchFileLoads(request);

        log.info("Search Files finished, results={}", response.size());
        return ResponseEntity.ok(
                new ApiResponse<>(HttpStatus.OK.name(),
                        HttpStatus.OK.value(),
                        "Files retrieved successfully",
                        response)
        );
    }


    @Override
    public ResponseEntity<ApiResponse<List<TransactionErrorResponse>>> searchFileErrors(
             TransactionErrorSearchRequest request) {
        log.info("Search File Errors started");

        List<TransactionErrorResponse> response = fileService.searchFileErrors(request);
        log.info("Search File Errors finished, results={}", response.size());
        return ResponseEntity.ok(
                new ApiResponse<>(
                        HttpStatus.OK.name(),
                        HttpStatus.OK.value(),
                        "File errors retrieved successfully",
                        response
                )
        );
    }



    @Override
    public ResponseEntity<ApiResponse<FileLoadMetaDataResponse>> modifyFileLoadStatus(
             FileLoadMetaData request) {

        log.info("Modify request received for id={}, newStatus={}",
                request.getFileId(), request.getStatus());

        FileLoadMetaDataResponse response = fileService.modifyFileLoadStatus(request);

        log.info("Modify completed for id={}, finalStatus={}",
                response.getId(), response.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(
                HttpStatus.OK.name(),
                HttpStatus.OK.value(),
                "File status updated successfully",
                response
        ));
    }




    @Override
    public ResponseEntity<ApiResponse<Void>> deleteFileLoad(Long id) {

        log.info("Delete request received for id={}", id);
        fileService.deleteFileLoad(id);

        log.info("Delete completed for id={}", id);

        return ResponseEntity.ok(
                new ApiResponse<>(HttpStatus.OK.name(),
                        HttpStatus.OK.value(),
                        "File deleted successfully",
                        null)
        );
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> archiveFileLoad( Long id) {
        log.info("Archive request received for id={}", id);
        fileService.archiveFileLoad(id);
        log.info("Archive completed for id={}",id );
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.name(), HttpStatus.OK.value(), "File archived successfully",null));
    }


    @Override
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file) throws Exception {

        FileUploadResponse response = fileService.uploadFile(file);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new ApiResponse<>(
                        HttpStatus.ACCEPTED.name(),
                        HttpStatus.ACCEPTED.value(),
                        "Ok",
                        response
                )
        );
    }

    @Operation(summary = "Get all except Deleted and Archived")
    @GetMapping("/getAll")
    @Override
    public ResponseEntity<ApiResponse<List<FileLoadMetaDataResponse>>> getAllFileLoads() {

        log.info("Get all SUCCESS file loads started");

        List<FileLoadMetaData> result = fileService.getAllFileLoads();
        List<FileLoadMetaDataResponse> response =
                fileLoadMetaDataMapper.toResponseList(result);

        log.info("Get all SUCCESS file loads finished, count={}", result.size());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        HttpStatus.OK.name(),
                        HttpStatus.OK.value(),
                        "All SUCCESS file loads retrieved successfully",
                        response
                )
        );
    }


}
