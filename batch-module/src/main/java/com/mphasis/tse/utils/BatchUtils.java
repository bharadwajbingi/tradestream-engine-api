package com.mphasis.tse.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BatchUtils {

    public static final String READER_NAME="transactionReader";
    public static final String STEP_NAME = "tradeProcessingStep";
    public static final String JOB_NAME = "tradeFileProcessingJob";

    public static final String DELIMITER=",";
}
