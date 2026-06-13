package com.mphasis.tse.entity;

import com.mphasis.tse.enums.ErrorStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "deleted_transaction_error")
public class DeletedTransactionError {

    @Id
    @Column(name = "error_id")
    private Long errorId; // Keep the original error ID

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "record_tracking_id", length = 64)
    private String recordTrackingId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "error_field")
    private String errorField;

    @Column(name = "error_message")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ErrorStatus status;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
