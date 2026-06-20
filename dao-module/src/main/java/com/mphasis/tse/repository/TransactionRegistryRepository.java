package com.mphasis.tse.repository;

import com.mphasis.tse.entity.TransactionRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRegistryRepository extends JpaRepository<TransactionRegistry, Long> {
    boolean existsByTransactionId(String transactionId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT r.transactionId FROM TransactionRegistry r
        WHERE r.transactionId IN :transactionIds
        AND r.user.id = :userId
        """)
    java.util.List<String> findExistingTransactionIdsForOwner(
            @org.springframework.data.repository.query.Param("transactionIds") java.util.Collection<String> transactionIds,
            @org.springframework.data.repository.query.Param("userId") Long userId);
}