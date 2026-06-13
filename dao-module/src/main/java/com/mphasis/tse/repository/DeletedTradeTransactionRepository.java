package com.mphasis.tse.repository;

import com.mphasis.tse.entity.DeletedTradeTransaction;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DeletedTradeTransactionRepository extends JpaRepository<DeletedTradeTransaction, Long> {

    @Modifying
    @Transactional
    @Query(
            value = "INSERT INTO deleted_trade_transaction " +
                    "(id, transaction_id, record_tracking_id, file_header_date, account_number, transaction_type, " +
                    " batch_location, batch_number, update_batch_date, related_file_number, " +
                    " action_name, related_file_key, do_not_report_flag, explanation, " +
                    " minor_assets_class, owning_portfolio, poster_initials, transaction_subtype, " +
                    " cash_effect, cash_paid_out, broker_number, old_balance, new_balance, file_id, deleted_at) " +
                    "SELECT id, transaction_id, record_tracking_id, file_header_date, account_number, transaction_type, " +
                    "       batch_location, batch_number, update_batch_date, related_file_number, " +
                    "       action_name, related_file_key, do_not_report_flag, explanation, " +
                    "       minor_assets_class, owning_portfolio, poster_initials, transaction_subtype, " +
                    "       cash_effect, cash_paid_out, broker_number, old_balance, new_balance, file_id, :deletedAt " +
                    "FROM trade_transaction WHERE file_id = :fileId",
            nativeQuery = true
    )
    void moveByFileId(@Param("fileId") Long fileId, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying
    @Transactional
    @Query(
            value = "INSERT INTO deleted_trade_transaction " +
                    "(id, transaction_id, record_tracking_id, file_header_date, account_number, transaction_type, " +
                    " batch_location, batch_number, update_batch_date, related_file_number, " +
                    " action_name, related_file_key, do_not_report_flag, explanation, " +
                    " minor_assets_class, owning_portfolio, poster_initials, transaction_subtype, " +
                    " cash_effect, cash_paid_out, broker_number, old_balance, new_balance, file_id, deleted_at) " +
                    "SELECT id, transaction_id, record_tracking_id, file_header_date, account_number, transaction_type, " +
                    "       batch_location, batch_number, update_batch_date, related_file_number, " +
                    "       action_name, related_file_key, do_not_report_flag, explanation, " +
                    "       minor_assets_class, owning_portfolio, poster_initials, transaction_subtype, " +
                    "       cash_effect, cash_paid_out, broker_number, old_balance, new_balance, file_id, :deletedAt " +
                    "FROM trade_archive WHERE file_id = :fileId",
            nativeQuery = true
    )
    void moveFromArchiveByFileId(@Param("fileId") Long fileId, @Param("deletedAt") LocalDateTime deletedAt);
}
