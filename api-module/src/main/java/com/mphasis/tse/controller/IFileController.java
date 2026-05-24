package com.mphasis.tse.controller;

import com.mphasis.tse.dto.*;
import com.mphasis.tse.entity.FileLoadMetaData;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/file")
public interface IFileController {

    @Operation(summary = "Get all validation errors", description = "Retrieves a list of all trade validation errors quarantined under this user account.")
    @GetMapping("/errors")
    ResponseEntity<ApiResponse<List<TransactionErrorResponse>>> getAllErrors();

    @Operation(summary = "Get all validation errors (Paginated)", description = "Retrieves a paginated list of all trade validation errors quarantined under this user account.")
    @GetMapping("/errors/page")
    ResponseEntity<ApiResponse<PageResponse<TransactionErrorResponse>>> getAllErrorsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @Operation(summary = "Search and filter files", description = "Queries and filters file metadata records uploaded by the user matching the search parameters.")
    @PostMapping("/search")
    ResponseEntity<ApiResponse<List<FileLoadMetaDataResponse>>> searchFiles(
            @RequestBody FileSearchRequest request);

    @Operation(summary = "Search and filter files (Paginated)", description = "Queries and filters file metadata records uploaded by the user matching the search parameters, paginated.")
    @PostMapping("/search/page")
    ResponseEntity<ApiResponse<PageResponse<FileLoadMetaDataResponse>>> searchFilesPage(
            @RequestBody FileSearchRequest request);

    @Operation(summary = "Search validation errors", description = "Queries and filters quarantined validation errors matching search parameters.")
    @PostMapping("/search-errors")
    ResponseEntity<ApiResponse<List<TransactionErrorResponse>>> searchFileErrors(
            @RequestBody TransactionErrorSearchRequest request);

    @Operation(summary = "Search validation errors (Paginated)", description = "Queries and filters quarantined validation errors matching search parameters, paginated.")
    @PostMapping("/search-errors/page")
    ResponseEntity<ApiResponse<PageResponse<TransactionErrorResponse>>> searchFileErrorsPage(
            @RequestBody TransactionErrorSearchRequest request);

    @Operation(summary = "Modify file load status", description = "Updates a file metadata status manually in the system.")
    @PutMapping("/modify")
    ResponseEntity<ApiResponse<FileLoadMetaDataResponse>> modifyFileLoadStatus(
            @RequestBody FileLoadMetaData request);

    @Operation(summary = "Soft delete file load", description = "Performs a complete soft delete cascade, moving active/archived trades and errors of the target file ID into backup tables, and updating file metadata status to DELETED.")
    @DeleteMapping("/delete/{id}")
    ResponseEntity<ApiResponse<Void>> deleteFileLoad(
            @PathVariable Long id);

    @Operation(summary = "Archive file load", description = "Moves all successfully parsed trade transactions under the target file ID from the active table into history archive tables.")
    @PostMapping("/archive/{id}")
    ResponseEntity<ApiResponse<Void>> archiveFileLoad(
            @PathVariable Long id);

    @Operation(summary = "Upload CSV trade file (Asynchronous Ingestion)", description = "Uploads a trade CSV file of up to 1GB, persists it to disk instantly, and queues it in a database-backed execution engine. Processing starts asynchronously.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file) throws Exception;

    @Operation(summary = "Get all active file loads", description = "Retrieves active non-deleted, non-archived file loads uploaded by the authenticated user.")
    @GetMapping("/getAll")
    ResponseEntity<ApiResponse<List<FileLoadMetaDataResponse>>> getAllFileLoads();

    @Operation(summary = "Get active file loads (Paginated)", description = "Retrieves a paginated list of active non-deleted, non-archived file loads uploaded by the authenticated user.")
    @GetMapping("/getAll/page")
    ResponseEntity<ApiResponse<PageResponse<FileLoadMetaDataResponse>>> getAllFileLoadsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @Operation(summary = "Get user dashboard metrics", description = "Returns overall counts of active file uploads, successful trade records, and cumulative unresolved quarantined errors.")
    @GetMapping("/metrics")
    ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboardMetrics();

    @Operation(summary = "Manually resolve error", description = "Sets a quarantined validation error status to RESOLVED. Recalculates file load state.")
    @PostMapping("/errors/{id}/resolve")
    ResponseEntity<ApiResponse<Void>> resolveError(@PathVariable("id") Long id);

    @Operation(summary = "Manually ignore error", description = "Ignores a quarantined INVALID_TRANSACTION_ID formatting error.")
    @PostMapping("/errors/{id}/ignore")
    ResponseEntity<ApiResponse<Void>> ignoreError(@PathVariable("id") Long id);
}
