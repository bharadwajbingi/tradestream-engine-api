package com.mphasis.tse.validation;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TransactionError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parameterized tests for ValidationService covering all 21 fields.
 * Each test modifies only one field from the shared valid baseline.
 *
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 6.10, 6.11, 6.12
 */
@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @InjectMocks
    private ValidationService validationService;

    @Mock
    private FileLoadMetaData metaData;

    /**
     * Shared valid 21-field baseline array.
     * All fields are valid so that modifying one field isolates the error to that field.
     */
    private static String[] baselineFields() {
        return new String[]{
                "TXN12345",        // 0: transactionId
                "20240101",        // 1: fileHeaderDate
                "ACC123",          // 2: accountNumber
                "1",               // 3: transactionType
                "NYC",             // 4: batchLocation
                "100",             // 5: batchNumber
                "20240101",        // 6: updateBatchDate
                "5",               // 7: relatedFileNumber
                "BUY",             // 8: actionName
                "12345",           // 9: relatedFileKey
                "Y",               // 10: doNotReportFlag
                "Test explanation", // 11: explanation
                "3",               // 12: minorAssetsClass
                "1001",            // 13: owningPortfolio
                "JD",              // 14: posterInitials
                "2",               // 15: transactionSubtype
                "123.45",          // 16: cashEffect
                "50.00",           // 17: cashPaidOut
                "7",               // 18: brokerNumber
                "1000.00",         // 19: oldBalance
                "1123.45"          // 20: newBalance
        };
    }

    // ==================== Requirement 6.1: Valid baseline produces no errors ====================

    @Test
    void validBaseline_producesNoErrors() {
        String[] fields = baselineFields();

        List<TransactionError> errors = validationService.validate(fields, metaData);

        assertThat(errors).isEmpty();
    }

    // ==================== Requirement 6.2: transactionId (index 0) ====================

    static Stream<Arguments> transactionIdCases() {
        return Stream.of(
                Arguments.of("", "Mandatory field missing"),
                Arguments.of("   ", "Mandatory field missing"),
                Arguments.of("INVALID", "Must start with TXN and be alphanumeric"),
                Arguments.of("TXN123456789012345678", "Invalid field length"),
                Arguments.of("TXN12345", null)
        );
    }

    @ParameterizedTest
    @MethodSource("transactionIdCases")
    void testTransactionId(String value, String expectedError) {
        String[] fields = baselineFields();
        fields[0] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals("transactionId")
                    && e.getErrorMessage().equals(expectedError));
        }
    }

    // ==================== Requirement 6.3: accountNumber (index 2) ====================

    static Stream<Arguments> accountNumberCases() {
        return Stream.of(
                Arguments.of("", "Mandatory field missing"),
                Arguments.of("   ", "Mandatory field missing"),
                Arguments.of("123ACC", "Must start with ACC"),
                Arguments.of("INVALID", "Must start with ACC"),
                Arguments.of("ACC123", null)
        );
    }

    @ParameterizedTest
    @MethodSource("accountNumberCases")
    void testAccountNumber(String value, String expectedError) {
        String[] fields = baselineFields();
        fields[2] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals("accountNumber")
                    && e.getErrorMessage().equals(expectedError));
        }
    }

    // ==================== Requirement 6.4: fileHeaderDate (index 1) ====================

    static Stream<Arguments> fileHeaderDateCases() {
        return Stream.of(
                Arguments.of("", "Mandatory field missing"),
                Arguments.of("   ", "Mandatory field missing"),
                Arguments.of("2024-01-01", "Invalid date format"),
                Arguments.of("abcdefgh", "Invalid date format"),
                Arguments.of("13001301", "Invalid date format"),
                Arguments.of("20240101", null)
        );
    }

    @ParameterizedTest
    @MethodSource("fileHeaderDateCases")
    void testFileHeaderDate(String value, String expectedError) {
        String[] fields = baselineFields();
        fields[1] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals("fileHeaderDate")
                    && e.getErrorMessage().equals(expectedError));
        }
    }

    // ==================== Requirement 6.5: Mandatory numeric fields ====================

    static Stream<Arguments> mandatoryNumericFieldCases() {
        return Stream.of(
                // transactionType (index 3)
                Arguments.of(3, "", "Mandatory field missing", "transactionType"),
                Arguments.of(3, "   ", "Mandatory field missing", "transactionType"),
                Arguments.of(3, "abc", "Must be numeric", "transactionType"),
                Arguments.of(3, "12a", "Must be numeric", "transactionType"),
                Arguments.of(3, "1", null, "transactionType"),
                // batchNumber (index 5)
                Arguments.of(5, "", "Mandatory field missing", "batchNumber"),
                Arguments.of(5, "   ", "Mandatory field missing", "batchNumber"),
                Arguments.of(5, "abc", "Must be numeric", "batchNumber"),
                Arguments.of(5, "100", null, "batchNumber"),
                // updateBatchDate (index 6)
                Arguments.of(6, "", "Mandatory field missing", "updateBatchDate"),
                Arguments.of(6, "   ", "Mandatory field missing", "updateBatchDate"),
                Arguments.of(6, "abc", "Must be numeric", "updateBatchDate"),
                Arguments.of(6, "20240101", null, "updateBatchDate"),
                // relatedFileKey (index 9)
                Arguments.of(9, "", "Mandatory field missing", "relatedFileKey"),
                Arguments.of(9, "   ", "Mandatory field missing", "relatedFileKey"),
                Arguments.of(9, "abc", "Must be numeric", "relatedFileKey"),
                Arguments.of(9, "12345", null, "relatedFileKey"),
                // owningPortfolio (index 13)
                Arguments.of(13, "", "Mandatory field missing", "owningPortfolio"),
                Arguments.of(13, "   ", "Mandatory field missing", "owningPortfolio"),
                Arguments.of(13, "abc", "Must be numeric", "owningPortfolio"),
                Arguments.of(13, "1001", null, "owningPortfolio"),
                // transactionSubtype (index 15)
                Arguments.of(15, "", "Mandatory field missing", "transactionSubtype"),
                Arguments.of(15, "   ", "Mandatory field missing", "transactionSubtype"),
                Arguments.of(15, "abc", "Must be numeric", "transactionSubtype"),
                Arguments.of(15, "2", null, "transactionSubtype")
        );
    }

    @ParameterizedTest
    @MethodSource("mandatoryNumericFieldCases")
    void testMandatoryNumericFields(int index, String value, String expectedError, String fieldName) {
        String[] fields = baselineFields();
        fields[index] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals(fieldName)
                    && e.getErrorMessage().equals(expectedError));
        }
    }

    // ==================== Requirement 6.6: Mandatory string fields ====================

    static Stream<Arguments> mandatoryStringFieldCases() {
        return Stream.of(
                // batchLocation (index 4)
                Arguments.of(4, "", "Mandatory field missing", "batchLocation"),
                Arguments.of(4, "   ", "Mandatory field missing", "batchLocation"),
                Arguments.of(4, "NYC", null, "batchLocation"),
                // actionName (index 8)
                Arguments.of(8, "", "Mandatory field missing", "actionName"),
                Arguments.of(8, "   ", "Mandatory field missing", "actionName"),
                Arguments.of(8, "BUY", null, "actionName"),
                // posterInitials (index 14)
                Arguments.of(14, "", "Mandatory field missing", "posterInitials"),
                Arguments.of(14, "   ", "Mandatory field missing", "posterInitials"),
                Arguments.of(14, "JD", null, "posterInitials")
        );
    }

    @ParameterizedTest
    @MethodSource("mandatoryStringFieldCases")
    void testMandatoryStringFields(int index, String value, String expectedError, String fieldName) {
        String[] fields = baselineFields();
        fields[index] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals(fieldName)
                    && e.getErrorMessage().equals(expectedError));
        }
    }

    // ==================== Requirement 6.7: doNotReportFlag (index 10) ====================

    static Stream<Arguments> doNotReportFlagCases() {
        return Stream.of(
                Arguments.of("", "Mandatory field missing"),
                Arguments.of("   ", "Mandatory field missing"),
                Arguments.of("YY", "Invalid field length"),
                Arguments.of("ABC", "Invalid field length"),
                Arguments.of("Y", null),
                Arguments.of("N", null)
        );
    }

    @ParameterizedTest
    @MethodSource("doNotReportFlagCases")
    void testDoNotReportFlag(String value, String expectedError) {
        String[] fields = baselineFields();
        fields[10] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals("doNotReportFlag")
                    && e.getErrorMessage().equals(expectedError));
        }
    }

    // ==================== Requirement 6.8: explanation (index 11) ====================

    static Stream<Arguments> explanationCases() {
        return Stream.of(
                Arguments.of("A".repeat(256), "Invalid field length"),
                Arguments.of("A".repeat(300), "Invalid field length"),
                Arguments.of("Test explanation", null),
                Arguments.of("A".repeat(255), null),
                Arguments.of("", null)
        );
    }

    @ParameterizedTest
    @MethodSource("explanationCases")
    void testExplanation(String value, String expectedError) {
        String[] fields = baselineFields();
        fields[11] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals("explanation")
                    && e.getErrorMessage().equals(expectedError));
        }
    }

    // ==================== Requirement 6.9: Optional numeric fields ====================

    static Stream<Arguments> optionalNumericFieldCases() {
        return Stream.of(
                // relatedFileNumber (index 7)
                Arguments.of(7, "abc", "Must be numeric", "relatedFileNumber"),
                Arguments.of(7, "12x", "Must be numeric", "relatedFileNumber"),
                Arguments.of(7, "5", null, "relatedFileNumber"),
                Arguments.of(7, "", null, "relatedFileNumber"),
                Arguments.of(7, "   ", null, "relatedFileNumber"),
                // minorAssetsClass (index 12)
                Arguments.of(12, "abc", "Must be numeric", "minorAssetsClass"),
                Arguments.of(12, "12X", "Must be numeric", "minorAssetsClass"),
                Arguments.of(12, "3", null, "minorAssetsClass"),
                Arguments.of(12, "", null, "minorAssetsClass"),
                Arguments.of(12, "   ", null, "minorAssetsClass"),
                // brokerNumber (index 18)
                Arguments.of(18, "abc", "Must be numeric", "brokerNumber"),
                Arguments.of(18, "12x", "Must be numeric", "brokerNumber"),
                Arguments.of(18, "7", null, "brokerNumber"),
                Arguments.of(18, "", null, "brokerNumber"),
                Arguments.of(18, "   ", null, "brokerNumber")
        );
    }

    @ParameterizedTest
    @MethodSource("optionalNumericFieldCases")
    void testOptionalNumericFields(int index, String value, String expectedError, String fieldName) {
        String[] fields = baselineFields();
        fields[index] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals(fieldName)
                    && e.getErrorMessage().equals(expectedError));
        }
    }

    // ==================== Requirement 6.10: cashEffect (index 16) ====================

    static Stream<Arguments> cashEffectCases() {
        return Stream.of(
                Arguments.of("", "Mandatory field missing"),
                Arguments.of("   ", "Mandatory field missing"),
                Arguments.of("abc", "Invalid decimal value"),
                Arguments.of("12.3.4", "Invalid decimal value"),
                Arguments.of("123.456", "Invalid decimal scale"),
                Arguments.of("1.999", "Invalid decimal scale"),
                Arguments.of("123.45", null),
                Arguments.of("100", null),
                Arguments.of("0.5", null)
        );
    }

    @ParameterizedTest
    @MethodSource("cashEffectCases")
    void testCashEffect(String value, String expectedError) {
        String[] fields = baselineFields();
        fields[16] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals("cashEffect")
                    && e.getErrorMessage().equals(expectedError));
        }
    }

    // ==================== Requirement 6.11: cashPaidOut (index 17) ====================

    static Stream<Arguments> cashPaidOutCases() {
        return Stream.of(
                Arguments.of("abc", "Invalid decimal value"),
                Arguments.of("12.3.4", "Invalid decimal value"),
                Arguments.of("12.345", "Invalid decimal scale"),
                Arguments.of("1.999", "Invalid decimal scale"),
                Arguments.of("50.00", null),
                Arguments.of("100", null),
                Arguments.of("", null),
                Arguments.of("   ", null)
        );
    }

    @ParameterizedTest
    @MethodSource("cashPaidOutCases")
    void testCashPaidOut(String value, String expectedError) {
        String[] fields = baselineFields();
        fields[17] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals("cashPaidOut")
                    && e.getErrorMessage().equals(expectedError));
        }
    }

    // ==================== Requirement 6.12: Balance fields (indices 19, 20) ====================

    static Stream<Arguments> balanceFieldCases() {
        return Stream.of(
                // oldBalance (index 19)
                Arguments.of(19, "", "Mandatory field missing", "oldBalance"),
                Arguments.of(19, "   ", "Mandatory field missing", "oldBalance"),
                Arguments.of(19, "abc", "Invalid decimal value", "oldBalance"),
                Arguments.of(19, "12.3.4", "Invalid decimal value", "oldBalance"),
                Arguments.of(19, "123456789012345678.00", "Precision exceeds allowed limit", "oldBalance"),
                Arguments.of(19, "1.234", "Precision exceeds allowed limit", "oldBalance"),
                Arguments.of(19, "1000.00", null, "oldBalance"),
                Arguments.of(19, "99999999999999999", null, "oldBalance"),
                // newBalance (index 20)
                Arguments.of(20, "", "Mandatory field missing", "newBalance"),
                Arguments.of(20, "   ", "Mandatory field missing", "newBalance"),
                Arguments.of(20, "abc", "Invalid decimal value", "newBalance"),
                Arguments.of(20, "12.3.4", "Invalid decimal value", "newBalance"),
                Arguments.of(20, "123456789012345678.00", "Precision exceeds allowed limit", "newBalance"),
                Arguments.of(20, "1.234", "Precision exceeds allowed limit", "newBalance"),
                Arguments.of(20, "1123.45", null, "newBalance"),
                Arguments.of(20, "99999999999999999", null, "newBalance")
        );
    }

    @ParameterizedTest
    @MethodSource("balanceFieldCases")
    void testBalanceFields(int index, String value, String expectedError, String fieldName) {
        String[] fields = baselineFields();
        fields[index] = value;

        List<TransactionError> errors = validationService.validate(fields, metaData);

        if (expectedError == null) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getErrorField().equals(fieldName)
                    && e.getErrorMessage().equals(expectedError));
        }
    }
}
