package com.mphasis.tse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private Long fileId;
    private String fileName;
    private String status;
    private String message;
}


