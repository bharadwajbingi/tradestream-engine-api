package com.mphasis.tse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileLoadMetaDataResponse {
    private Long id;
    private Long fileId;
    private Long userId;

    private String filename;
    private LocalDateTime uploadTime;
    private int totalRecords;
    private int successCount;
    private int errorCount;
    private int duplicateCount;
    private String status;
    private Long processingTimeMs;

}
