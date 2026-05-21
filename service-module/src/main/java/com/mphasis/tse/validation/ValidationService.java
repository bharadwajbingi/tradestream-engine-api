package com.mphasis.tse.validation;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.enums.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ValidationService {

    private static final String FIELD_TRANSACTION_ID      = "transactionId";
    private static final String FIELD_FILE_HEADER_DATE    = "fileHeaderDate";
    private static final String FIELD_ACCOUNT_NUMBER      = "accountNumber";
    private static final String FIELD_TRANSACTION_TYPE    = "transactionType";
    private static final String FIELD_BATCH_LOCATION      = "batchLocation";
    private static final String FIELD_BATCH_NUMBER        = "batchNumber";
    private static final String FIELD_UPDATE_BATCH_DATE   = "updateBatchDate";
    private static final String FIELD_RELATED_FILE_NUMBER = "relatedFileNumber";
    private static final String FIELD_ACTION_NAME         = "actionName";
    private static final String FIELD_RELATED_FILE_KEY    = "relatedFileKey";
    private static final String FIELD_DO_NOT_REPORT_FLAG  = "doNotReportFlag";
    private static final String FIELD_EXPLANATION         = "explanation";
    private static final String FIELD_MINOR_ASSETS_CLASS  = "minorAssetsClass";
    private static final String FIELD_OWNING_PORTFOLIO    = "owningPortfolio";
    private static final String FIELD_POSTER_INITIALS     = "posterInitials";
    private static final String FIELD_TRANSACTION_SUBTYPE = "transactionSubtype";
    private static final String FIELD_CASH_EFFECT         = "cashEffect";
    private static final String FIELD_CASH_PAID_OUT       = "cashPaidOut";
    private static final String FIELD_BROKER_NUMBER       = "brokerNumber";
    private static final String FIELD_OLD_BALANCE         = "oldBalance";
    private static final String FIELD_NEW_BALANCE         = "newBalance";


    private static final String ERROR_MANDATORY = "Mandatory field missing";
    private static final String ERROR_NUMERIC = "Must be numeric";
    private static final String ERROR_DATE_FORMAT = "Invalid date format";
    private static final String ERROR_FIELD_LENGTH = "Invalid field length";
    private static final String ERROR_DECIMAL = "Invalid decimal value";
    private static final String ERROR_PRECISION = "Precision exceeds allowed limit";
    private static final String ERROR_DECIMAL_SCALE = "Invalid decimal scale";


    private static final int    MAX_TRANSACTION_ID_LENGTH = 20;
    private static final int    MAX_EXPLANATION_LENGTH    = 255;
    private static final int    DO_NOT_REPORT_FLAG_LENGTH = 1;
    private static final int    MAX_BALANCE_PRECISION     = 17;
    private static final int    MAX_BALANCE_SCALE         = 2;
    private static final int    MAX_DECIMAL_SCALE         = 2;
    private static final String DATE_PATTERN              = "yyyyMMdd";
    private static final String TRANSACTION_ID_REGEX      = "TXN[a-zA-Z0-9]+";
    private static final String ACCOUNT_NUMBER_REGEX      = "ACC[a-zA-Z0-9]+";
    private static final String NUMERIC_REGEX             = "-?\\d+";



    public List<TransactionError> validate(String[] fields,
                                           FileLoadMetaData metaData) {
        List<TransactionError> errors = new ArrayList<>();

        String transactionId      = fields[0];
        String fileHeaderDate     = fields[1];
        String accountNumber      = fields[2];
        String transactionType    = fields[3];
        String batchLocation      = fields[4];
        String batchNumber        = fields[5];
        String updateBatchDate    = fields[6];
        String relatedFileNumber  = fields[7];
        String actionName         = fields[8];
        String relatedFileKey     = fields[9];
        String doNotReportFlag    = fields[10];
        String explanation        = fields[11];
        String minorAssetsClass   = fields[12];
        String owningPortfolio    = fields[13];
        String posterInitials     = fields[14];
        String transactionSubtype = fields[15];
        String cashEffect         = fields[16];
        String cashPaidOut        = fields[17];
        String brokerNumber       = fields[18];
        String oldBalance         = fields[19];
        String newBalance         = fields[20];


        validateTransactionId(transactionId, accountNumber, metaData, errors);
        validateFileHeaderDate(fileHeaderDate, transactionId, accountNumber, metaData, errors);
        validateAccountNumber(accountNumber, transactionId, metaData, errors);
        validateTransactionType(transactionType, transactionId, accountNumber, metaData, errors);
        validateBatchLocation(batchLocation, transactionId, accountNumber, metaData, errors);
        validateBatchNumber(batchNumber, transactionId, accountNumber, metaData, errors);
        validateUpdateBatchDate(updateBatchDate, transactionId, accountNumber, metaData, errors);
        validateRelatedFileNumber(relatedFileNumber, transactionId, accountNumber, metaData, errors);
        validateActionName(actionName, transactionId, accountNumber, metaData, errors);
        validateRelatedFileKey(relatedFileKey, transactionId, accountNumber, metaData, errors);
        validateDoNotReportFlag(doNotReportFlag, transactionId, accountNumber, metaData, errors);
        validateExplanation(explanation, transactionId, accountNumber, metaData, errors);
        validateMinorAssetsClass(minorAssetsClass, transactionId, accountNumber, metaData, errors);
        validateOwningPortfolio(owningPortfolio, transactionId, accountNumber, metaData, errors);
        validatePosterInitials(posterInitials, transactionId, accountNumber, metaData, errors);
        validateTransactionSubtype(transactionSubtype, transactionId, accountNumber, metaData, errors);
        validateCashEffect(cashEffect, transactionId, accountNumber, metaData, errors);
        validateCashPaidOut(cashPaidOut, transactionId, accountNumber, metaData, errors);
        validateBrokerNumber(brokerNumber, transactionId, accountNumber, metaData, errors);
        validateOldBalance(oldBalance, transactionId, accountNumber, metaData, errors);
        validateNewBalance(newBalance, transactionId, accountNumber, metaData, errors);

        return errors;
    }



    private void validateTransactionId(String val,
                                       String accountNumber,
                                       FileLoadMetaData metaData,
                                       List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, val, accountNumber,
                    FIELD_TRANSACTION_ID, ERROR_MANDATORY));
            return;
        }
        if (val.length() > MAX_TRANSACTION_ID_LENGTH) {
            errors.add(buildError(metaData, val, accountNumber,
                    FIELD_TRANSACTION_ID, ERROR_FIELD_LENGTH));
        }
        if (!val.matches(TRANSACTION_ID_REGEX)) {
            errors.add(buildError(metaData, val, accountNumber,
                    FIELD_TRANSACTION_ID,
                    "Must start with TXN and be alphanumeric"));
        }
    }

    private void validateFileHeaderDate(String val,
                                        String transactionId,
                                        String accountNumber,
                                        FileLoadMetaData metaData,
                                        List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_FILE_HEADER_DATE, ERROR_MANDATORY));
            return;
        }
        try {
            LocalDate.parse(val, DateTimeFormatter.ofPattern(DATE_PATTERN));
        } catch (DateTimeParseException e) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_FILE_HEADER_DATE, ERROR_DATE_FORMAT));
        }
    }


    private void validateAccountNumber(String val,
                                       String transactionId,
                                       FileLoadMetaData metaData,
                                       List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, val,
                    FIELD_ACCOUNT_NUMBER, ERROR_MANDATORY));
            return;
        }
        if (!val.matches(ACCOUNT_NUMBER_REGEX)) {
            errors.add(buildError(metaData, transactionId, val,
                    FIELD_ACCOUNT_NUMBER, "Must start with ACC"));
        }
    }

    private void validateTransactionType(String val,
                                         String transactionId,
                                         String accountNumber,
                                         FileLoadMetaData metaData,
                                         List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_TRANSACTION_TYPE, ERROR_MANDATORY));
            return;
        }
        if (!isNumeric(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_TRANSACTION_TYPE, ERROR_NUMERIC));
        }
    }

    private void validateBatchLocation(String val,
                                       String transactionId,
                                       String accountNumber,
                                       FileLoadMetaData metaData,
                                       List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_BATCH_LOCATION, ERROR_MANDATORY));
        }
    }

    private void validateBatchNumber(String val,
                                     String transactionId,
                                     String accountNumber,
                                     FileLoadMetaData metaData,
                                     List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_BATCH_NUMBER, ERROR_MANDATORY));
            return;
        }
        if (!isNumeric(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_BATCH_NUMBER, ERROR_NUMERIC));
        }
    }

    private void validateUpdateBatchDate(String val,
                                         String transactionId,
                                         String accountNumber,
                                         FileLoadMetaData metaData,
                                         List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_UPDATE_BATCH_DATE, ERROR_MANDATORY));
            return;
        }
        if (!isNumeric(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_UPDATE_BATCH_DATE, ERROR_NUMERIC));
        }
    }

    private void validateRelatedFileNumber(String val,
                                           String transactionId,
                                           String accountNumber,
                                           FileLoadMetaData metaData,
                                           List<TransactionError> errors) {
        if (StringUtils.hasText(val) && !isNumeric(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_RELATED_FILE_NUMBER, ERROR_NUMERIC));
        }
    }

    private void validateActionName(String val,
                                    String transactionId,
                                    String accountNumber,
                                    FileLoadMetaData metaData,
                                    List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_ACTION_NAME, ERROR_MANDATORY));
        }
    }

    private void validateRelatedFileKey(String val,
                                        String transactionId,
                                        String accountNumber,
                                        FileLoadMetaData metaData,
                                        List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_RELATED_FILE_KEY, ERROR_MANDATORY));
            return;
        }
        if (!isNumeric(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_RELATED_FILE_KEY, ERROR_NUMERIC));
        }
    }

    private void validateDoNotReportFlag(String val,
                                         String transactionId,
                                         String accountNumber,
                                         FileLoadMetaData metaData,
                                         List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_DO_NOT_REPORT_FLAG, ERROR_MANDATORY));
            return;
        }
        if (val.length() != DO_NOT_REPORT_FLAG_LENGTH) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_DO_NOT_REPORT_FLAG, ERROR_FIELD_LENGTH));
        }
    }

    private void validateExplanation(String val,
                                     String transactionId,
                                     String accountNumber,
                                     FileLoadMetaData metaData,
                                     List<TransactionError> errors) {
        if (StringUtils.hasText(val) && val.length() > MAX_EXPLANATION_LENGTH) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_EXPLANATION, ERROR_FIELD_LENGTH));
        }
    }

    private void validateMinorAssetsClass(String val,
                                          String transactionId,
                                          String accountNumber,
                                          FileLoadMetaData metaData,
                                          List<TransactionError> errors) {
        if (StringUtils.hasText(val) && !isNumeric(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_MINOR_ASSETS_CLASS, ERROR_NUMERIC));
        }
    }

    private void validateOwningPortfolio(String val,
                                         String transactionId,
                                         String accountNumber,
                                         FileLoadMetaData metaData,
                                         List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_OWNING_PORTFOLIO, ERROR_MANDATORY));
            return;
        }
        if (!isNumeric(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_OWNING_PORTFOLIO, ERROR_NUMERIC));
        }
    }

    private void validatePosterInitials(String val,
                                        String transactionId,
                                        String accountNumber,
                                        FileLoadMetaData metaData,
                                        List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_POSTER_INITIALS, ERROR_MANDATORY));
        }
    }

    private void validateTransactionSubtype(String val,
                                            String transactionId,
                                            String accountNumber,
                                            FileLoadMetaData metaData,
                                            List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_TRANSACTION_SUBTYPE, ERROR_MANDATORY));
            return;
        }
        if (!isNumeric(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_TRANSACTION_SUBTYPE, ERROR_NUMERIC));
        }
    }

    private void validateCashEffect(String val,
                                    String transactionId,
                                    String accountNumber,
                                    FileLoadMetaData metaData,
                                    List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_CASH_EFFECT, ERROR_MANDATORY));
            return;
        }
        if (!isDecimal(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_CASH_EFFECT, ERROR_DECIMAL));
            return;
        }
        if (!isValidScale(val, MAX_DECIMAL_SCALE)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_CASH_EFFECT, ERROR_DECIMAL_SCALE));
        }
    }

    private void validateCashPaidOut(String val,
                                     String transactionId,
                                     String accountNumber,
                                     FileLoadMetaData metaData,
                                     List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) return;
        if (!isDecimal(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_CASH_PAID_OUT, ERROR_DECIMAL));
            return;
        }
        if (!isValidScale(val, MAX_DECIMAL_SCALE)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_CASH_PAID_OUT, ERROR_DECIMAL_SCALE));
        }
    }

    private void validateBrokerNumber(String val,
                                      String transactionId,
                                      String accountNumber,
                                      FileLoadMetaData metaData,
                                      List<TransactionError> errors) {
        if (StringUtils.hasText(val) && !isNumeric(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_BROKER_NUMBER, ERROR_NUMERIC));
        }
    }

    private void validateOldBalance(String val,
                                    String transactionId,
                                    String accountNumber,
                                    FileLoadMetaData metaData,
                                    List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_OLD_BALANCE, ERROR_MANDATORY));
            return;
        }
        if (!isDecimal(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_OLD_BALANCE, ERROR_DECIMAL));
            return;
        }
        if (!isValidPrecisionAndScale(val, MAX_BALANCE_PRECISION, MAX_BALANCE_SCALE)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_OLD_BALANCE, ERROR_PRECISION));
        }
    }

    private void validateNewBalance(String val,
                                    String transactionId,
                                    String accountNumber,
                                    FileLoadMetaData metaData,
                                    List<TransactionError> errors) {
        if (!StringUtils.hasText(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_NEW_BALANCE, ERROR_MANDATORY));
            return;
        }
        if (!isDecimal(val)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_NEW_BALANCE, ERROR_DECIMAL));
            return;
        }
        if (!isValidPrecisionAndScale(val, MAX_BALANCE_PRECISION, MAX_BALANCE_SCALE)) {
            errors.add(buildError(metaData, transactionId, accountNumber,
                    FIELD_NEW_BALANCE, ERROR_PRECISION));
        }
    }


    private TransactionError buildError(FileLoadMetaData metaData,
                                        String transactionId,
                                        String accountNumber,
                                        String errorField,
                                        String errorMessage) {
        TransactionError error = new TransactionError();
        error.setMetaData(metaData);
        error.setTransactionId(transactionId);
        error.setAccountNumber(accountNumber);
        error.setErrorField(errorField);
        error.setErrorMessage(errorMessage);
        error.setStatus(ErrorStatus.FAILED);
        error.setCreatedTime(LocalDateTime.now());
        return error;
    }


    private boolean isNumeric(String val) {
        return StringUtils.hasText(val)
                && val.trim().matches(NUMERIC_REGEX);
    }

    private boolean isDecimal(String val) {
        try {
            new BigDecimal(val.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidScale(String val, int maxScale) {
        try {
            return new BigDecimal(val.trim()).scale() <= maxScale;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidPrecisionAndScale(String val,
                                             int maxPrecision,
                                             int maxScale) {
        try {
            BigDecimal bd = new BigDecimal(val.trim());
            return bd.precision() <= maxPrecision
                    && bd.scale() <= maxScale;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean validateNumber(String val) {
        return isNumeric((val));
    }
}