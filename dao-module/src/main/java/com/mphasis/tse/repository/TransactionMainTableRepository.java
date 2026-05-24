package com.mphasis.tse.repository;
import com.mphasis.tse.entity.TradeTransaction;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionMainTableRepository
        extends JpaRepository<TradeTransaction, Long> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM trade_transaction WHERE file_id = :fileId", nativeQuery = true)
    void deleteByFileId(@Param("fileId") Long fileId);

    boolean existsByTransactionId(String transactionId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TradeTransaction t WHERE t.transactionId = :transactionId AND t.metaData.user.id = :userId")
    boolean existsByTransactionIdAndUserId(@Param("transactionId") String transactionId, @Param("userId") Long userId);

    @Query("""
           SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
           FROM TradeTransaction t
           WHERE t.transactionId = :transactionId
             AND ((:userId IS NULL AND t.metaData.user IS NULL) OR t.metaData.user.id = :userId)
           """)
    boolean existsByTransactionIdForOwner(@Param("transactionId") String transactionId,
                                          @Param("userId") Long userId);

    @Query("""
           SELECT t.transactionId
           FROM TradeTransaction t
           WHERE t.transactionId IN :transactionIds
             AND ((:userId IS NULL AND t.metaData.user IS NULL) OR t.metaData.user.id = :userId)
           """)
    java.util.List<String> findExistingTransactionIdsForOwner(
            @Param("transactionIds") java.util.Collection<String> transactionIds,
            @Param("userId") Long userId);


    @Query("SELECT COUNT(t) FROM TradeTransaction t WHERE t.metaData.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM TradeTransaction t WHERE t.metaData.fileId = :fileId")
    java.util.List<TradeTransaction> findByFileId(@Param("fileId") Long fileId);

    @org.springframework.data.jpa.repository.QueryHints(value = @jakarta.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "1000"))
    @Query("SELECT t FROM TradeTransaction t WHERE t.metaData.fileId = :fileId")
    java.util.stream.Stream<com.mphasis.tse.dto.TradeExportProjection> streamByFileId(@Param("fileId") Long fileId);

    @Query("SELECT t FROM TradeTransaction t WHERE t.metaData.fileId = :fileId AND t.metaData.user.id = :userId")
    java.util.List<TradeTransaction> findByFileIdAndUserId(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @org.springframework.data.jpa.repository.QueryHints(value = @jakarta.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "1000"))
    @Query("SELECT t FROM TradeTransaction t WHERE t.metaData.fileId = :fileId AND t.metaData.user.id = :userId")
    java.util.stream.Stream<com.mphasis.tse.dto.TradeExportProjection> streamByFileIdAndUserId(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @Query("SELECT t FROM TradeTransaction t WHERE (:start IS NULL OR t.fileHeaderDate >= :start) AND (:end IS NULL OR t.fileHeaderDate <= :end)")
    java.util.List<TradeTransaction> findByFileHeaderDateBetween(@Param("start") String start, @Param("end") String end);

    @org.springframework.data.jpa.repository.QueryHints(value = @jakarta.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "1000"))
    @Query("SELECT t FROM TradeTransaction t WHERE (:start IS NULL OR t.fileHeaderDate >= :start) AND (:end IS NULL OR t.fileHeaderDate <= :end)")
    java.util.stream.Stream<com.mphasis.tse.dto.TradeExportProjection> streamByFileHeaderDateBetween(@Param("start") String start, @Param("end") String end);

    @Query("SELECT t FROM TradeTransaction t WHERE t.metaData.user.id = :userId AND (:start IS NULL OR t.fileHeaderDate >= :start) AND (:end IS NULL OR t.fileHeaderDate <= :end)")
    java.util.List<TradeTransaction> findByFileHeaderDateBetweenAndUserId(@Param("start") String start, @Param("end") String end, @Param("userId") Long userId);

    @org.springframework.data.jpa.repository.QueryHints(value = @jakarta.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "1000"))
    @Query("SELECT t FROM TradeTransaction t WHERE t.metaData.user.id = :userId AND (:start IS NULL OR t.fileHeaderDate >= :start) AND (:end IS NULL OR t.fileHeaderDate <= :end)")
    java.util.stream.Stream<com.mphasis.tse.dto.TradeExportProjection> streamByFileHeaderDateBetweenAndUserId(@Param("start") String start, @Param("end") String end, @Param("userId") Long userId);
}
