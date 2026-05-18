package com.mphasis.tse.repository;

import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.enums.ErrorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TransactionErrorRepository extends JpaRepository<TransactionError, Long>, JpaSpecificationExecutor<TransactionError> {


    @Modifying
    @Transactional
    @Query("UPDATE TransactionError e SET e.status = :status WHERE e.transactionId IN :transactionIds")
    int updateStatusByTransactionIds(@Param("transactionIds") List<String> transactionIds,
                                     @Param("status") ErrorStatus status);



    @Query("SELECT COUNT(DISTINCT e.transactionId) FROM TransactionError e WHERE e.status = :status")
    long countDistinctByStatus(@Param("status") ErrorStatus status);


    @Query("""
           SELECT e FROM TransactionError e
           WHERE (:transactionId IS NULL OR e.transactionId = :transactionId)
             AND (:accountNumber IS NULL OR e.accountNumber = :accountNumber)
             AND (:status IS NULL OR e.status = :status)
           """)
    List<TransactionError> searchErrors(
            @Param("transactionId") String transactionId,
            @Param("accountNumber") String accountNumber,
            @Param("status") ErrorStatus status
    );
}
