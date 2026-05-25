package com.mphasis.tse.repository;

import com.mphasis.tse.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, String> {
    List<ExportJob> findByDownloadedTrueAndDownloadedAtBefore(LocalDateTime time);
    List<ExportJob> findByUserIdOrderByCreatedAtDesc(Long userId);
}
