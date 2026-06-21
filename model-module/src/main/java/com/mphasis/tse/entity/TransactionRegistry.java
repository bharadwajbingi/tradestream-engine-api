package com.mphasis.tse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_registry")
@Getter
@Setter
@NoArgsConstructor
public class TransactionRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "created_time")
    private LocalDateTime createdTime;

    public TransactionRegistry(String transactionId) {
        this.transactionId = transactionId;
    }

    public TransactionRegistry(String transactionId, User user) {
        this.transactionId = transactionId;
        this.user = user;
    }
}
