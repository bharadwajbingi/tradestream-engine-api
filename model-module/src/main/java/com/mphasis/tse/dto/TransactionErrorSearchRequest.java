package com.mphasis.tse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionErrorSearchRequest
{
    private Long fileLoadId;

    private String transactionId;

    private String accountNumber;

    private String errorField;

    private String status;

    private Integer page = 0;

    private Integer size = 20;

}
