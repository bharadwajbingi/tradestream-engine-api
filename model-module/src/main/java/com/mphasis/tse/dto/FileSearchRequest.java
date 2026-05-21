package com.mphasis.tse.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileSearchRequest {
    private Long fileId;
    private String filename;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer page = 0;
    private Integer size = 20;

}

