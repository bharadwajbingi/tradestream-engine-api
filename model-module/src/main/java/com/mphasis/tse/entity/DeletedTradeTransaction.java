package com.mphasis.tse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "deleted_trade_transaction")
public class DeletedTradeTransaction {

    @Id
    @Column(name = "id")
    private Integer id; // Keep the original transaction ID

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "record_tracking_id", length = 64)
    private String recordTrackingId;

    @Column(name = "file_header_date")
    private String fileHeaderDate;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "transaction_type")
    private Integer transactionType;

    @Column(name = "batch_location")
    private String batchLocation;

    @Column(name = "batch_number")
    private Integer batchNumber;

    @Column(name = "update_batch_date")
    private String updateBatchDate;

    @Column(name = "related_file_number")
    private Integer relatedFileNumber;

    @Column(name = "action_name")
    private String actionName;

    @Column(name = "related_file_key")
    private Long relatedFileKey;

    @Column(name = "do_not_report_flag")
    private String doNotReportFlag;

    @Column(name = "explanation")
    private String explanation;

    @Column(name = "minor_assets_class")
    private Integer minorAssetsClass;

    @Column(name = "owning_portfolio")
    private Integer owningPortfolio;

    @Column(name = "poster_initials")
    private String posterInitials;

    @Column(name = "transaction_subtype")
    private Integer transactionSubtype;

    @Column(name = "cash_effect")
    private BigDecimal cashEffect;

    @Column(name = "cash_paid_out")
    private BigDecimal cashPaidOut;

    @Column(name = "broker_number")
    private Integer brokerNumber;

    @Column(name = "old_balance")
    private BigDecimal oldBalance;

    @Column(name = "new_balance")
    private BigDecimal newBalance;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
