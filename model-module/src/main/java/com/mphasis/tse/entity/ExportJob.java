package com.mphasis.tse.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Long userId;

    private String status; // PENDING, PROCESSING, COMPLETED, FAILED

    private String s3Url;

    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    private boolean downloaded = false;
    private LocalDateTime downloadedAt;
    
    private String exportType; // e.g. "MAIN", "ARCHIVE"
}
