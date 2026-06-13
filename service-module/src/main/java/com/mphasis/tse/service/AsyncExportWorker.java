package com.mphasis.tse.service;

import com.mphasis.tse.entity.ExportJob;
import com.mphasis.tse.repository.ExportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncExportWorker {

    private final ExportService exportService;
    private final ExportJobRepository exportJobRepository;
    private final S3Service s3Service;

    private static final String[] TRANSACTION_HEADERS = {
            "Transaction ID", "Record Tracking ID", "File Header Date", "Account Number", "Transaction Type",
            "Batch Location", "Batch Number", "Update Batch Date", "Related File Number", "Action Name",
            "Related File Key", "Do Not Report Flag", "Explanation", "Minor Assets Class", "Owning Portfolio",
            "Poster Initials", "Transaction Subtype", "Cash Effect", "Cash Paid Out", "Broker Number",
            "Old Balance", "New Balance", "Row Number", "File ID", "Is Archived"
    };

    @Async
    public void processActiveTransactionsExport(String jobId, String startDate, String endDate, Long fileId, Optional<Long> userId) {
        processExport(jobId, "active", startDate, endDate, fileId, userId);
    }

    @Async
    public void processArchivedTransactionsExport(String jobId, String startDate, String endDate, Long fileId, Optional<Long> userId) {
        processExport(jobId, "archived", startDate, endDate, fileId, userId);
    }

    private void processExport(String jobId, String type, String startDate, String endDate, Long fileId, Optional<Long> userId) {
        ExportJob job = exportJobRepository.findById(jobId).orElseThrow();
        job.setStatus("PROCESSING");
        job.setUpdatedAt(LocalDateTime.now());
        exportJobRepository.save(job);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("export_" + jobId, ".csv");
            try (Writer writer = new FileWriter(tempFile);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(TRANSACTION_HEADERS).build())) {

                if ("active".equals(type)) {
                    exportService.streamActiveTransactions(startDate, endDate, fileId, userId, stream -> {
                        stream.forEach(t -> {
                            try {
                                csvPrinter.printRecord(
                                        t.getTransactionId(), t.getRecordTrackingId(), t.getFileHeaderDate(), t.getAccountNumber(), t.getTransactionType(),
                                        t.getBatchLocation(), t.getBatchNumber(), t.getUpdateBatchDate(), t.getRelatedFileNumber(), t.getActionName(),
                                        t.getRelatedFileKey(), t.getDoNotReportFlag(), t.getExplanation(), t.getMinorAssetsClass(), t.getOwningPortfolio(),
                                        t.getPosterInitials(), t.getTransactionSubtype(),
                                        t.getCashEffect() != null ? t.getCashEffect().toPlainString() : "0.00",
                                        t.getCashPaidOut() != null ? t.getCashPaidOut().toPlainString() : "0.00",
                                        t.getBrokerNumber(),
                                        t.getOldBalance() != null ? t.getOldBalance().toPlainString() : "0.00",
                                        t.getNewBalance() != null ? t.getNewBalance().toPlainString() : "0.00",
                                        t.getRowNumber(), t.getFileId(), "FALSE"
                                );
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    });
                } else {
                    exportService.streamArchivedTransactions(startDate, endDate, fileId, userId, stream -> {
                        stream.forEach(a -> {
                            try {
                                csvPrinter.printRecord(
                                        a.getTransactionId(), a.getRecordTrackingId(), a.getFileHeaderDate(), a.getAccountNumber(), a.getTransactionType(),
                                        a.getBatchLocation(), a.getBatchNumber(), a.getUpdateBatchDate(), a.getRelatedFileNumber(), a.getActionName(),
                                        a.getRelatedFileKey(), a.getDoNotReportFlag(), a.getExplanation(), a.getMinorAssetsClass(), a.getOwningPortfolio(),
                                        a.getPosterInitials(), a.getTransactionSubtype(),
                                        a.getCashEffect() != null ? a.getCashEffect().toPlainString() : "0.00",
                                        a.getCashPaidOut() != null ? a.getCashPaidOut().toPlainString() : "0.00",
                                        a.getBrokerNumber(),
                                        a.getOldBalance() != null ? a.getOldBalance().toPlainString() : "0.00",
                                        a.getNewBalance() != null ? a.getNewBalance().toPlainString() : "0.00",
                                        a.getRowNumber(), a.getFileId(), "TRUE"
                                );
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    });
                }
                csvPrinter.flush();
            }

            // Upload to S3
            String s3Key = "exports/" + userId.orElse(0L) + "/" + type + "_" + jobId + ".csv";
            s3Service.uploadFile(s3Key, tempFile);

            job.setStatus("COMPLETED");
            job.setS3Url(s3Key);
            job.setUpdatedAt(LocalDateTime.now());
            exportJobRepository.save(job);

        } catch (Exception e) {
            log.error("Failed to process export job {}", jobId, e);
            job.setStatus("FAILED");
            
            // Extract a concise message and truncate to 255 chars just in case (even though DB is TEXT)
            String safeMsg = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
            if (safeMsg.length() > 250) {
                safeMsg = safeMsg.substring(0, 247) + "...";
            }
            job.setErrorMessage("Export failed: " + safeMsg);
            
            job.setUpdatedAt(LocalDateTime.now());
            exportJobRepository.save(job);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
