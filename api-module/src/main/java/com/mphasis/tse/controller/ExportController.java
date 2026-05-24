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
    public void exportTransactions(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "fileId", required = false) Long fileId,
            @AuthenticationPrincipal User principal,
            HttpServletResponse response) throws IOException {

        log.info("Streaming active transactions export for user: {}", principal != null ? principal.getEmail() : "anonymous");

        List<TradeTransaction> activeList;
        if (fileId != null) {
            assertFileAccess(fileId, principal);
            activeList = currentUserId(principal)
                    .map(userId -> transactionMainTableRepository.findByFileIdAndUserId(fileId, userId))
                    .orElseGet(() -> transactionMainTableRepository.findByFileId(fileId));
        } else {
            String cleanStart = (startDate != null && !startDate.trim().isEmpty()) ? startDate.replace("-", "").trim() : null;
            String cleanEnd = (endDate != null && !endDate.trim().isEmpty()) ? endDate.replace("-", "").trim() : null;
            Optional<Long> userId = currentUserId(principal);
            if (userId.isPresent()) {
                activeList = transactionMainTableRepository.findByFileHeaderDateBetweenAndUserId(cleanStart, cleanEnd, userId.get());
            } else {
                activeList = transactionMainTableRepository.findByFileHeaderDateBetween(cleanStart, cleanEnd);
            }
        }

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        String filename = "active_transactions_" + System.currentTimeMillis() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(TRANSACTION_HEADERS).build())) {

            for (TradeTransaction t : activeList) {
                csvPrinter.printRecord(
                        t.getTransactionId(),
                        t.getRecordTrackingId(),
                        t.getFileHeaderDate(),
                        t.getAccountNumber(),
                        t.getTransactionType(),
                        t.getBatchLocation(),
                        t.getBatchNumber(),
                        t.getUpdateBatchDate(),
                        t.getRelatedFileNumber(),
                        t.getActionName(),
                        t.getRelatedFileKey(),
                        t.getDoNotReportFlag(),
                        t.getExplanation(),
                        t.getMinorAssetsClass(),
                        t.getOwningPortfolio(),
                        t.getPosterInitials(),
                        t.getTransactionSubtype(),
                        t.getCashEffect() != null ? t.getCashEffect().toPlainString() : "0.00",
                        t.getCashPaidOut() != null ? t.getCashPaidOut().toPlainString() : "0.00",
                        t.getBrokerNumber(),
                        t.getOldBalance() != null ? t.getOldBalance().toPlainString() : "0.00",
                        t.getNewBalance() != null ? t.getNewBalance().toPlainString() : "0.00",
                        t.getRowNumber(),
                        t.getMetaData() != null ? t.getMetaData().getFileId() : null,
                        "FALSE"
                );
            }
            csvPrinter.flush();
        }
    }

    @GetMapping("/transactions/archive/export")
    public void exportArchivedTransactions(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "fileId", required = false) Long fileId,
            @AuthenticationPrincipal User principal,
            HttpServletResponse response) throws IOException {

        log.info("Streaming archived transactions export for user: {}", principal != null ? principal.getEmail() : "anonymous");

        List<TradeArchive> archiveList;
        if (fileId != null) {
            assertFileAccess(fileId, principal);
            archiveList = tradeArchiveRepository.findByFileId(fileId);
        } else {
            String cleanStart = (startDate != null && !startDate.trim().isEmpty()) ? startDate.replace("-", "").trim() : null;
            String cleanEnd = (endDate != null && !endDate.trim().isEmpty()) ? endDate.replace("-", "").trim() : null;
            Optional<Long> userId = currentUserId(principal);
            if (userId.isPresent()) {
                List<Long> fileIds = transactionMetaTableRepository.findActiveFileIdsByUserId(userId.get());
                if (!fileIds.isEmpty()) {
                    archiveList = tradeArchiveRepository.findByFileIdInAndFileHeaderDateBetween(fileIds, cleanStart, cleanEnd);
                } else {
                    archiveList = new ArrayList<>();
                }
            } else {
                archiveList = tradeArchiveRepository.findByFileHeaderDateBetween(cleanStart, cleanEnd);
            }
        }

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        String filename = "archived_transactions_" + System.currentTimeMillis() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(TRANSACTION_HEADERS).build())) {

            for (TradeArchive a : archiveList) {
                csvPrinter.printRecord(
                        a.getTransactionId(),
                        a.getRecordTrackingId(),
                        a.getFileHeaderDate(),
                        a.getAccountNumber(),
                        a.getTransactionType(),
                        a.getBatchLocation(),
                        a.getBatchNumber(),
                        a.getUpdateBatchDate(),
                        a.getRelatedFileNumber(),
                        a.getActionName(),
                        a.getRelatedFileKey(),
                        a.getDoNotReportFlag(),
                        a.getExplanation(),
                        a.getMinorAssetsClass(),
                        a.getOwningPortfolio(),
                        a.getPosterInitials(),
                        a.getTransactionSubtype(),
                        a.getCashEffect() != null ? a.getCashEffect().toPlainString() : "0.00",
                        a.getCashPaidOut() != null ? a.getCashPaidOut().toPlainString() : "0.00",
                        a.getBrokerNumber(),
                        a.getOldBalance() != null ? a.getOldBalance().toPlainString() : "0.00",
                        a.getNewBalance() != null ? a.getNewBalance().toPlainString() : "0.00",
                        null,
                        a.getFileId(),
                        "TRUE"
                );
            }
            csvPrinter.flush();
        }
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
