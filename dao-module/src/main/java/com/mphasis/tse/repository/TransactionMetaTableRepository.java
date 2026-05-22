package com.mphasis.tse.repository;
import com.mphasis.tse.entity.FileLoadMetaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionMetaTableRepository
        extends JpaRepository<FileLoadMetaData, Long>,
        JpaSpecificationExecutor<FileLoadMetaData> {

    @Query("SELECT f FROM FileLoadMetaData f WHERE f.uploadTime < :cutoff AND f.status != com.mphasis.tse.enums.FileStatus.ARCHIVED AND (f.isDeleted IS NULL OR f.isDeleted = false)")
    List<FileLoadMetaData> findExpiredFilesForArchiving(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(f) FROM FileLoadMetaData f WHERE f.isDeleted IS NULL OR f.isDeleted = false")
    long countActiveFiles();

    @Query("SELECT COUNT(f) FROM FileLoadMetaData f WHERE (f.isDeleted IS NULL OR f.isDeleted = false) AND f.user.id = :userId")
    long countActiveFilesByUserId(@Param("userId") Long userId);

    @Query("SELECT f.fileId FROM FileLoadMetaData f WHERE f.user.id = :userId AND (f.isDeleted IS NULL OR f.isDeleted = false)")
    List<Long> findActiveFileIdsByUserId(@Param("userId") Long userId);

    long countByStatusIn(List<com.mphasis.tse.enums.FileStatus> statuses);
    
    @Query("SELECT DISTINCT f.user.id FROM FileLoadMetaData f WHERE f.status IN ('STARTED', 'PROCESSING')")
    List<Long> findUsersWithActiveJobs();

    List<FileLoadMetaData> findByStatusOrderByUploadTimeAsc(com.mphasis.tse.enums.FileStatus status);

    List<FileLoadMetaData> findByStatusIn(List<com.mphasis.tse.enums.FileStatus> statuses);
}

