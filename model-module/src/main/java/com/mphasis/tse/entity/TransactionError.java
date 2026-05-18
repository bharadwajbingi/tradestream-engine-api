package com.mphasis.tse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mphasis.tse.enums.ErrorStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TRANSACTION_ERROR",indexes = {@Index(name = "idx_transaction_error",columnList = "transaction_id, error_field")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_id", nullable = false)
    private Long errorId;

    @Column(name = "transaction_id")
    private String transactionId;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    @JsonIgnore
    private FileLoadMetaData metaData;
}
