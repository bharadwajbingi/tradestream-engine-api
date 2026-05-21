package com.mphasis.tse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("status")
    private String status;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("statusCode")
    private Integer statusCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private T data;

    public ApiResponse(String status, Integer code, String message, T data) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.success = code != null && code >= 200 && code < 300;
        this.code = code;
        this.statusCode = code;
        this.message = message;
        this.data = data;
    }
}
