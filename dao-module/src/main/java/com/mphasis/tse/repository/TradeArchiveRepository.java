
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
                    "SELECT * FROM trade_transaction WHERE file_id = :fileId",
            nativeQuery = true
    )
    void archiveByFileId(@Param("fileId") Long fileId);
}