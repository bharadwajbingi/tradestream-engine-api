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
    @Query(
            value = "DELETE FROM trade_transaction WHERE file_id = :fileId",
            nativeQuery = true
    )
    void deleteByFileId(@Param("fileId") Long fileId);
}
