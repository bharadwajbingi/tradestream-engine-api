
package com.mphasis.tse.repository;
import com.mphasis.tse.entity.TradeArchive;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeArchiveRepository extends JpaRepository<TradeArchive, Long> {

    @Modifying
    @Transactional
    @Query(
            value = "INSERT INTO trade_archive " +
                    "(id, transaction_id, record_tracking_id, file_header_date, account_number, transaction_type, " +
                    " batch_location, batch_number, update_batch_date, related_file_number, " +
                    " action_name, related_file_key, do_not_report_flag, explanation, " +
                    " minor_assets_class, owning_portfolio, poster_initials, transaction_subtype, " +
                    " cash_effect, cash_paid_out, broker_number, old_balance, new_balance, file_id) " +
                    "SELECT id, transaction_id, record_tracking_id, file_header_date, account_number, transaction_type, " +
                    "       batch_location, batch_number, update_batch_date, related_file_number, " +
                    "       action_name, related_file_key, do_not_report_flag, explanation, " +
                    "       minor_assets_class, owning_portfolio, poster_initials, transaction_subtype, " +
                    "       cash_effect, cash_paid_out, broker_number, old_balance, new_balance, file_id " +
                    "FROM trade_transaction WHERE file_id = :fileId",
            nativeQuery = true
    )
    void archiveByFileId(@Param("fileId") Long fileId);

    @Query("SELECT a FROM TradeArchive a WHERE (:start IS NULL OR a.fileHeaderDate >= :start) AND (:end IS NULL OR a.fileHeaderDate <= :end)")
    java.util.List<TradeArchive> findByFileHeaderDateBetween(@Param("start") String start, @Param("end") String end);

    @Query("SELECT a FROM TradeArchive a WHERE a.fileId = :fileId")
    java.util.List<TradeArchive> findByFileId(@Param("fileId") Long fileId);

    @Query("SELECT a FROM TradeArchive a WHERE a.fileId IN :fileIds")
    java.util.List<TradeArchive> findByFileIdIn(@Param("fileIds") java.util.List<Long> fileIds);

    long countByFileIdIn(java.util.List<Long> fileIds);

    @Query("SELECT a FROM TradeArchive a WHERE a.fileId IN :fileIds AND (:start IS NULL OR a.fileHeaderDate >= :start) AND (:end IS NULL OR a.fileHeaderDate <= :end)")
    java.util.List<TradeArchive> findByFileIdInAndFileHeaderDateBetween(@Param("fileIds") java.util.List<Long> fileIds,
                                                                        @Param("start") String start,
                                                                        @Param("end") String end);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM trade_archive WHERE file_id = :fileId", nativeQuery = true)
    void deleteByFileId(@Param("fileId") Long fileId);
}
