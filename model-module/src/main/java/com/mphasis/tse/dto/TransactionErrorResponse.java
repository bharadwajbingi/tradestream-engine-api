package com.mphasis.tse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionErrorResponse {



    private Long errorId;
    private String transactionId;
    private String accountNumber;
    private String errorField;
    private String errorMessage;
    private String status;
    private Long fileId;
    private Integer rowNumber;
    private String filename;
    private java.time.LocalDateTime createdTime;



}
