package com.mphasis.tse.repository;

import com.mphasis.tse.entity.DeletedTransactionError;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DeletedTransactionErrorRepository extends JpaRepository<DeletedTransactionError, Long> {

    @Modifying
    @Transactional
    @Query(
            value = "INSERT INTO deleted_transaction_error " +
                    "(error_id, transaction_id, record_tracking_id, account_number, error_field, error_message, " +
                    " status, created_time, row_number, file_id, deleted_at) " +
                    "SELECT error_id, transaction_id, record_tracking_id, account_number, error_field, error_message, " +
                    "       status, created_time, row_number, file_id, :deletedAt " +
                    "FROM TRANSACTION_ERROR WHERE file_id = :fileId",
            nativeQuery = true
    )
    void moveByFileId(@Param("fileId") Long fileId, @Param("deletedAt") LocalDateTime deletedAt);
}
