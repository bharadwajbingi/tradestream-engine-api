package com.mphasis.tse.repository;

import com.mphasis.tse.entity.TransactionRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRegistryRepository extends JpaRepository<TransactionRegistry, Long> {
    boolean existsByTransactionId(String transactionId);


}