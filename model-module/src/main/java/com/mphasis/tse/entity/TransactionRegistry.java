package com.mphasis.tse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_registry")
@Data
public class TransactionRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @CreationTimestamp
    @Column(name = "created_time")
    private LocalDateTime createdTime;

    public TransactionRegistry(String transactionId) {
        this.transactionId = transactionId;
    }
}
