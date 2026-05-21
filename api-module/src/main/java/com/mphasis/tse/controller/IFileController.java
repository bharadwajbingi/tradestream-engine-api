package com.mphasis.tse.controller;

import com.mphasis.tse.dto.*;
import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.dto.FileLoadMetaDataResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/file")
public interface IFileController {

    @Operation(summary = "to get all errors")
    @GetMapping("/errors")
    ResponseEntity<ApiResponse<List<TransactionErrorResponse>>> getAllErrors();

    @GetMapping("/errors/page")
    ResponseEntity<ApiResponse<PageResponse<TransactionErrorResponse>>> getAllErrorsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @PostMapping("/search")
    ResponseEntity<ApiResponse<List<FileLoadMetaDataResponse>>> searchFiles(
            @RequestBody FileSearchRequest request);

    @PostMapping("/search/page")
    ResponseEntity<ApiResponse<PageResponse<FileLoadMetaDataResponse>>> searchFilesPage(
            @RequestBody FileSearchRequest request);

    @PostMapping("/search-errors")
    ResponseEntity<ApiResponse<List<TransactionErrorResponse>>> searchFileErrors(
            @RequestBody TransactionErrorSearchRequest request);

    @PostMapping("/search-errors/page")
    ResponseEntity<ApiResponse<PageResponse<TransactionErrorResponse>>> searchFileErrorsPage(
            @RequestBody TransactionErrorSearchRequest request);

    @PutMapping("/modify")
    ResponseEntity<ApiResponse<FileLoadMetaDataResponse>> modifyFileLoadStatus(
            @RequestBody FileLoadMetaData request);

    @DeleteMapping("/delete/{id}")
    ResponseEntity<ApiResponse<Void>> deleteFileLoad(
            @PathVariable Long id);

    @PostMapping("/archive/{id}")
    ResponseEntity<ApiResponse<Void>> archiveFileLoad(
            @PathVariable Long id);

    @Operation(summary = "Upload CSV File for processing")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file) throws Exception;

    @Operation(summary="Get all except Deleted and Archived")
    @GetMapping("/getAll")
    ResponseEntity<ApiResponse<List<FileLoadMetaDataResponse>>> getAllFileLoads();

    @GetMapping("/getAll/page")
    ResponseEntity<ApiResponse<PageResponse<FileLoadMetaDataResponse>>> getAllFileLoadsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @GetMapping("/metrics")
    ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboardMetrics();

    @PostMapping("/errors/{id}/resolve")
    ResponseEntity<ApiResponse<Void>> resolveError(@PathVariable("id") Long id);

    @PostMapping("/errors/{id}/ignore")
    ResponseEntity<ApiResponse<Void>> ignoreError(@PathVariable("id") Long id);
}
