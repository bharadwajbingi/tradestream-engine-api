package com.mphasis.tse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {

    private Long totalFiles;
    private Long successRecords;
    private Long errorRecords;

}