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
    @Query("UPDATE TransactionError e SET e.status = :status WHERE e.transactionId IN :transactionIds AND e.status = com.mphasis.tse.enums.ErrorStatus.FAILED AND e.metaData.status != com.mphasis.tse.enums.FileStatus.ARCHIVED")
    int updateStatusByTransactionIds(@Param("transactionIds") List<String> transactionIds,
                                     @Param("status") ErrorStatus status);

    @Modifying
    @Transactional
    @Query("""
           UPDATE TransactionError e
           SET e.status = :status
           WHERE e.transactionId IN :transactionIds
             AND e.status = com.mphasis.tse.enums.ErrorStatus.FAILED
             AND e.metaData.status != com.mphasis.tse.enums.FileStatus.ARCHIVED
             AND ((:userId IS NULL AND e.metaData.user IS NULL) OR e.metaData.user.id = :userId)
           """)
    int updateStatusByTransactionIdsAndUserId(@Param("transactionIds") List<String> transactionIds,
                                              @Param("status") ErrorStatus status,
                                              @Param("userId") Long userId);

    long countByMetaData_FileIdAndStatus(Long fileId, ErrorStatus status);

    @Query("SELECT COUNT(e) FROM TransactionError e WHERE e.metaData.fileId = :fileId AND e.status IN :statuses")
    long countByMetaData_FileIdAndStatusIn(@Param("fileId") Long fileId,
                                           @Param("statuses") List<ErrorStatus> statuses);



    @Query("SELECT COUNT(DISTINCT e.transactionId) FROM TransactionError e WHERE e.status = :status")
    long countDistinctByStatus(@Param("status") ErrorStatus status);

    @Query("SELECT COUNT(DISTINCT e.transactionId) FROM TransactionError e WHERE e.status = :status AND e.metaData.user.id = :userId")
    long countDistinctByStatusAndUserId(@Param("status") ErrorStatus status, @Param("userId") Long userId);

    @Query("""
           SELECT e FROM TransactionError e
           WHERE (:transactionId IS NULL AND e.transactionId IS NULL OR e.transactionId = :transactionId)
             AND e.errorField = :errorField
             AND e.errorMessage = :errorMessage
             AND e.status = :status
             AND e.metaData.status != com.mphasis.tse.enums.FileStatus.ARCHIVED
           """)
    List<TransactionError> findExistingActiveError(
            @Param("transactionId") String transactionId,
            @Param("errorField") String errorField,
            @Param("errorMessage") String errorMessage,
            @Param("status") ErrorStatus status);

    @Query("""
           SELECT e FROM TransactionError e
           WHERE (:transactionId IS NULL AND e.transactionId IS NULL OR e.transactionId = :transactionId)
             AND e.errorField = :errorField
             AND e.errorMessage = :errorMessage
             AND e.status = :status
             AND e.metaData.status != com.mphasis.tse.enums.FileStatus.ARCHIVED
             AND ((:userId IS NULL AND e.metaData.user IS NULL) OR e.metaData.user.id = :userId)
           """)
    List<TransactionError> findExistingActiveErrorForUser(
            @Param("transactionId") String transactionId,
            @Param("errorField") String errorField,
            @Param("errorMessage") String errorMessage,
            @Param("status") ErrorStatus status,
            @Param("userId") Long userId);

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

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM TRANSACTION_ERROR WHERE file_id = :fileId", nativeQuery = true)
    void deleteByFileId(@Param("fileId") Long fileId);
}
