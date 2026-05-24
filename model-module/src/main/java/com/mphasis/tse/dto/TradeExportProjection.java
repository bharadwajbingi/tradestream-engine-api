package com.mphasis.tse.dto;

import org.springframework.beans.factory.annotation.Value;
import java.math.BigDecimal;

public interface TradeExportProjection {
    String getTransactionId();
    String getRecordTrackingId();
    String getFileHeaderDate();
    String getAccountNumber();
    String getTransactionType();
    String getBatchLocation();
    String getBatchNumber();
    String getUpdateBatchDate();
    String getRelatedFileNumber();
    String getActionName();
    String getRelatedFileKey();
    String getDoNotReportFlag();
    String getExplanation();
    String getMinorAssetsClass();
    String getOwningPortfolio();
    String getPosterInitials();
    String getTransactionSubtype();
    BigDecimal getCashEffect();
    BigDecimal getCashPaidOut();
    String getBrokerNumber();
    BigDecimal getOldBalance();
    BigDecimal getNewBalance();
    Long getRowNumber();

    // Resolves fileId from metaData for TradeTransaction, or directly for TradeArchive
    @Value("#{target.metaData != null ? target.metaData.fileId : target.fileId}")
    Long getFileId();
}
