package com.mphasis.tse.controller;

import com.mphasis.tse.entity.TradeArchive;
import com.mphasis.tse.entity.TradeTransaction;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.entity.User;
import com.mphasis.tse.repository.TradeArchiveRepository;
import com.mphasis.tse.repository.TransactionMainTableRepository;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import com.mphasis.tse.repository.TransactionErrorRepository;
import com.mphasis.tse.dto.TransactionErrorSearchRequest;
import com.mphasis.tse.enums.ErrorStatus;
import com.mphasis.tse.specification.TransactionErrorSpecification;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.security.Principal;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExportController {

    private final TransactionMainTableRepository transactionMainTableRepository;
    private final TradeArchiveRepository tradeArchiveRepository;
    private final TransactionMetaTableRepository transactionMetaTableRepository;
    private final TransactionErrorRepository transactionErrorRepository;
    private final com.mphasis.tse.service.ExportService exportService;
    private final com.mphasis.tse.service.AsyncExportWorker asyncExportWorker;
    private final com.mphasis.tse.repository.ExportJobRepository exportJobRepository;
    private final com.mphasis.tse.service.S3Service s3Service;

    private static final String[] TRANSACTION_HEADERS = {
            "Transaction ID", "Record Tracking ID", "File Header Date", "Account Number", "Transaction Type",
            "Batch Location", "Batch Number", "Update Batch Date", "Related File Number",
            "Action Name", "Related File Key", "Do Not Report Flag", "Explanation",
            "Minor Assets Class", "Owning Portfolio", "Poster Initials", "Transaction Subtype",
            "Cash Effect", "Cash Paid Out", "Broker Number", "Old Balance", "New Balance",
            "Row Number", "File ID", "Is Archived"
    };

    private static final String[] ERROR_HEADERS = {
            "Error ID", "Transaction ID", "Account Number",
            "Error Field", "Error Message", "Status", "Created Time", "Row Number", "File ID"
    };

    @GetMapping("/file/download/{fileId}")
    public void downloadOriginalFile(
            @PathVariable("fileId") Long fileId,
            @AuthenticationPrincipal User principal,
            HttpServletResponse response) throws IOException {

        log.info("Streaming original uploaded file for fileId: {}", fileId);

        com.mphasis.tse.entity.FileLoadMetaData metaData = transactionMetaTableRepository.findById(fileId)
                .orElseThrow(() -> new com.mphasis.tse.exception.FileNotFoundException("File metadata not found for id " + fileId));

        assertFileAccess(fileId, principal);

        String filePathStr = metaData.getFilePath();
        if (filePathStr == null) {
            throw new com.mphasis.tse.exception.FileNotFoundException("File path not set for metadata id " + fileId);
        }

        java.nio.file.Path filePath = java.nio.file.Paths.get(filePathStr);
        if (!java.nio.file.Files.exists(filePath)) {
            throw new com.mphasis.tse.exception.FileNotFoundException("Physical file not found on disk at " + filePathStr);
        }

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + metaData.getFilename() + "\"");

        java.nio.file.Files.copy(filePath, response.getOutputStream());
        response.getOutputStream().flush();
    }

    @GetMapping("/transactions/export")
    public ResponseEntity<?> exportTransactions(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "fileId", required = false) Long fileId,
            @AuthenticationPrincipal User principal,
            HttpServletResponse response) throws IOException {

        log.info("Streaming active transactions export for user: {}", principal != null ? principal.getEmail() : "anonymous");

        String cleanStart = (startDate != null && !startDate.trim().isEmpty()) ? startDate.replace("-", "").trim() : null;
        String cleanEnd = (endDate != null && !endDate.trim().isEmpty()) ? endDate.replace("-", "").trim() : null;
        Optional<Long> userId = currentUserId(principal);

        if (fileId != null) {
            assertFileAccess(fileId, principal);
        }

        com.mphasis.tse.entity.ExportJob job = new com.mphasis.tse.entity.ExportJob();
        job.setUserId(userId.orElse(null));
        job.setStatus("PENDING");
        job.setExportType("MAIN");
        exportJobRepository.save(job);

        asyncExportWorker.processActiveTransactionsExport(job.getId(), cleanStart, cleanEnd, fileId, userId);

        return ResponseEntity.accepted().body(java.util.Collections.singletonMap("jobId", job.getId()));
    }

    @GetMapping("/transactions/archive/export")
    public ResponseEntity<?> exportArchivedTransactions(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "fileId", required = false) Long fileId,
            @AuthenticationPrincipal User principal,
            HttpServletResponse response) throws IOException {

        log.info("Streaming archived transactions export for user: {}", principal != null ? principal.getEmail() : "anonymous");

        String cleanStart = (startDate != null && !startDate.trim().isEmpty()) ? startDate.replace("-", "").trim() : null;
        String cleanEnd = (endDate != null && !endDate.trim().isEmpty()) ? endDate.replace("-", "").trim() : null;
        Optional<Long> userId = currentUserId(principal);

        if (fileId != null) {
            assertFileAccess(fileId, principal);
        }

        com.mphasis.tse.entity.ExportJob job = new com.mphasis.tse.entity.ExportJob();
        job.setUserId(userId.orElse(null));
        job.setStatus("PENDING");
        job.setExportType("ARCHIVE");
        exportJobRepository.save(job);

        asyncExportWorker.processArchivedTransactionsExport(job.getId(), cleanStart, cleanEnd, fileId, userId);

        return ResponseEntity.accepted().body(java.util.Collections.singletonMap("jobId", job.getId()));
    }

    @GetMapping("/transactions/export/status/{jobId}")
    public ResponseEntity<?> getExportStatus(@PathVariable String jobId, @AuthenticationPrincipal User principal) {
        com.mphasis.tse.entity.ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Optional<Long> currentUserId = currentUserId(principal);
        if (job.getUserId() != null && currentUserId.isPresent() && !job.getUserId().equals(currentUserId.get())) {
            return ResponseEntity.status(403).body(java.util.Collections.singletonMap("error", "Access denied"));
        }

        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("status", job.getStatus());
        if ("FAILED".equals(job.getStatus())) {
            response.put("error", job.getErrorMessage());
        }
        // downloadUrl is omitted here so the client must call /download/{jobId} endpoint

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/export/jobs")
    public ResponseEntity<?> listExportJobs(@AuthenticationPrincipal User principal) {
        Optional<Long> currentUserId = currentUserId(principal);
        if (currentUserId.isEmpty()) {
            return ResponseEntity.status(401).body(java.util.Collections.singletonMap("error", "Unauthorized"));
        }
        
        List<com.mphasis.tse.entity.ExportJob> jobs = exportJobRepository.findByUserIdOrderByCreatedAtDesc(currentUserId.get());
        
        List<java.util.Map<String, Object>> response = new ArrayList<>();
        for (com.mphasis.tse.entity.ExportJob job : jobs) {
            java.util.Map<String, Object> jobMap = new java.util.HashMap<>();
            jobMap.put("id", job.getId());
            jobMap.put("status", job.getStatus());
            jobMap.put("createdAt", job.getCreatedAt());
            jobMap.put("downloaded", job.isDownloaded());
            jobMap.put("exportType", job.getExportType());
            if ("FAILED".equals(job.getStatus())) {
                jobMap.put("errorMessage", job.getErrorMessage());
            }
            response.add(jobMap);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/export/download/{jobId}")
    public ResponseEntity<?> downloadExport(@PathVariable String jobId, @AuthenticationPrincipal User principal) {
        com.mphasis.tse.entity.ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Optional<Long> currentUserId = currentUserId(principal);
        if (job.getUserId() != null && currentUserId.isPresent() && !job.getUserId().equals(currentUserId.get())) {
            return ResponseEntity.status(403).body(java.util.Collections.singletonMap("error", "Access denied"));
        }

        if (!"COMPLETED".equals(job.getStatus()) || job.getS3Url() == null) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", "Export not ready or missing"));
        }

        job.setDownloaded(true);
        job.setDownloadedAt(java.time.LocalDateTime.now());
        exportJobRepository.save(job);

        String label = "ARCHIVE".equals(job.getExportType()) ? "Archive_Table_Data" : "Trade_Data_Main_Table";
        String filename = label + "_" + java.time.LocalDate.now().toString().replace("-", "") + "_" + job.getId().substring(0,8) + ".csv";
        String s3Url = s3Service.generatePresignedUrl(job.getS3Url(), filename);
        
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("downloadUrl", s3Url);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/file/errors/export")
    public void exportAllErrors(
            @AuthenticationPrincipal User principal,
            HttpServletResponse response) throws IOException {

        log.info("Streaming all transaction errors export for user: {}", principal != null ? principal.getEmail() : "anonymous");

        Specification<TransactionError> spec = Specification.where(ownedErrorSpec(principal));
        List<TransactionError> errorsList = transactionErrorRepository.findAll(spec);

        streamErrorsToResponse(errorsList, response);
    }

    @PostMapping("/file/search-errors/export")
    public void exportSearchedErrors(
            @RequestBody TransactionErrorSearchRequest request,
            @AuthenticationPrincipal User principal,
            HttpServletResponse response) throws IOException {

        log.info("Streaming filtered transaction errors export for user: {}", principal != null ? principal.getEmail() : "anonymous");

        Specification<TransactionError> spec = Specification.where(ownedErrorSpec(principal));
        if (request.getFileLoadId() != null) {
            spec = spec.and(TransactionErrorSpecification.hasFileLoadId(request.getFileLoadId()));
        }
        if (request.getTransactionId() != null) {
            spec = spec.and(TransactionErrorSpecification.hasTransactionId(request.getTransactionId()));
        }
        if (request.getAccountNumber() != null) {
            spec = spec.and(TransactionErrorSpecification.hasAccountNumber(request.getAccountNumber()));
        }
        if (request.getErrorField() != null) {
            spec = spec.and(TransactionErrorSpecification.hasErrorField(request.getErrorField()));
        }
        if (request.getStatus() != null) {
            ErrorStatus statusEnum = ErrorStatus.valueOf(request.getStatus().toUpperCase());
            spec = spec.and(TransactionErrorSpecification.hasStatus(statusEnum));
        } else {
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.notEqual(root.get("status"), ErrorStatus.IGNORED),
                    cb.notEqual(root.get("status"), ErrorStatus.DUPLICATE)
            ));
        }

        List<TransactionError> errorsList = transactionErrorRepository.findAll(spec);
        streamErrorsToResponse(errorsList, response);
    }

    private void streamErrorsToResponse(List<TransactionError> errorsList, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        String filename = "transaction_errors_" + System.currentTimeMillis() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(ERROR_HEADERS).build())) {

            for (TransactionError e : errorsList) {
                csvPrinter.printRecord(
                        e.getErrorId(),
                        e.getTransactionId(),
                        e.getAccountNumber(),
                        e.getErrorField(),
                        e.getErrorMessage(),
                        e.getStatus() != null ? e.getStatus().name() : "",
                        e.getCreatedTime(),
                        e.getRowNumber(),
                        e.getMetaData() != null ? e.getMetaData().getFileId() : null
                );
            }
            csvPrinter.flush();
        }
    }

    private Optional<Long> currentUserId(User principal) {
        if (principal == null) {
            return Optional.empty();
        }
        return Optional.of(principal.getId());
    }

    private Specification<TransactionError> ownedErrorSpec(User principal) {
        return currentUserId(principal)
                .<Specification<TransactionError>>map(TransactionErrorSpecification::belongsToUser)
                .orElse(Specification.unrestricted());
    }

    private void assertFileAccess(Long fileId, User principal) {
        currentUserId(principal).ifPresent(userId -> {
            Long ownerId = transactionMetaTableRepository.findById(fileId)
                    .map(meta -> meta.getUser() != null ? meta.getUser().getId() : null)
                    .orElse(null);
            if (!userId.equals(ownerId)) {
                throw new AccessDeniedException("You do not have access to file id " + fileId);
            }
        });
    }
}
