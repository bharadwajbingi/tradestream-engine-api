package com.mphasis.tse.entity;

import com.mphasis.tse.enums.FileStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_meta_data",indexes = {@Index(name = "idx_filename",columnList = "filename")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileLoadMetaData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "error_count")
    private Integer errorCount;

    @Column(name = "duplicate_count")
    private Integer duplicateCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FileStatus status;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
}
