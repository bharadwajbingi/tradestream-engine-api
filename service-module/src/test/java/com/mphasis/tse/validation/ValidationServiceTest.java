package com.mphasis.tse.validation;

import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TransactionError;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @InjectMocks
    private ValidationService validationService;

    @Mock
    private FileLoadMetaData metaData;

    private String[] baseValidFields() {
        return new String[]{
                "TXN12345",
                "20240101",
                "ACC123",
                "1",
                "CHN",
                "100",
                "001",
                "456",
                "ACTION",
                "789",
                "Y",
                "NOTE",
                "12",
                "1001",
                "PS",
                "1",
                "123.45",
                "10.00",
                "456",
                "1000.00",
                "1200.00"
        };
    }

    private FileLoadMetaData getMetaData() {
        return metaData;
    }

    @ParameterizedTest
    @CsvSource({
            "'', true, Mandatory field missing",
            "'INVALID', true, Must start with TXN",
            "'TXN123456789012345678901', true, Invalid field length",
            "'TXN123', false, NONE"
    })
    void testTransactionId(String txnId, boolean shouldFail, String expectedMessage) {

        String[] fields = baseValidFields();
        fields[0] = txnId;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        assertEquals(shouldFail, !errors.isEmpty());

        if (shouldFail) {
            boolean found = errors.stream()
                    .anyMatch(e -> e.getErrorMessage().contains(expectedMessage));
            assertTrue(found, "Expected error message not found");
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', true, Mandatory field missing",
            "'ACC123', false, NONE",
            "'123ACC', true, Must start with ACC"
    })
    void testAccountNumber(String acc, boolean shouldFail, String expectedMessage) {

        String[] fields = baseValidFields();
        fields[2] = acc;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        assertEquals(shouldFail, !errors.isEmpty());

        if (shouldFail) {
            TransactionError error = errors.stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Error expected but not found"));
            assertTrue(error.getErrorMessage().contains(expectedMessage),
                    "Expected error message not found");
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'',Mandatory field missing",
            "20240101, NONE",
            "2024-01-01,Invalid date format",
            "abcd,Invalid date format"
    })
    void testFileHeaderDate(String date, String expectedError) {

        String[] fields = baseValidFields();
        fields[1] = date;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'   ', Mandatory field missing",
            "'1', NONE",
            "'123', NONE",
            "'abc', Must be numeric",
            "'12a', Must be numeric"
    })
    void testTransactionType(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[3] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'   ', Mandatory field missing",
            "'CHN', NONE"
    })
    void testBatchLocation(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[4] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'123', NONE",
            "'abc', Must be numeric",
            "'12a', Must be numeric"
    })
    void testBatchNumber(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[5] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'00123', NONE",
            "'abc123', Must be numeric"
    })
    void testUpdateBatchDate(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[6] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'456', NONE",
            "'abc', Must be numeric",
            "'   ', NONE"
    })
    void testRelatedFileNumber(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[7] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'ACTION', NONE",
            "'   ', Mandatory field missing"
    })
    void testActionName(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[8] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'789', NONE",
            "'abc', Must be numeric",
            "'12a', Must be numeric"
    })
    void testRelatedFileKey(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[9] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'YY', Invalid field length",
            "'Y', NONE"
    })
    void testDoNotReportFlag(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[10] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', NONE",
            "'Valid explanation', NONE",
            "'ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZAAAAA', Invalid field length"
    })
    void testExplanation(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[11] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', NONE",
            "'123', NONE",
            "'abc', Must be numeric",
            "'12X', Must be numeric"
    })
    void testMinorAssetsClass(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[12] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'1001', NONE",
            "'abc', Must be numeric",
            "'12A', Must be numeric"
    })
    void testOwningPortfolio(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[13] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'PS', NONE",
            "'', Mandatory field missing",
            "'   ', Mandatory field missing"
    })
    void testPosterInitials(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[14] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'1', NONE",
            "'12345', NONE",
            "'abc', Must be numeric",
            "'12x', Must be numeric"
    })
    void testTransactionSubtype(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[15] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'abc', Invalid decimal value",
            "'123.456', Invalid decimal scale",
            "'100', NONE",
            "'1.23', NONE"
    })
    void testCashEffect(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[16] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', NONE",
            "'123.45', NONE",
            "'abc', Invalid decimal value",
            "'12.345', Invalid decimal scale"
    })
    void testCashPaidOut(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[17] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', NONE",
            "'456', NONE",
            "'abc', Must be numeric",
            "'12x', Must be numeric"
    })
    void testBrokerNumber(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[18] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'abc', Invalid decimal value",
            "'123456789012345678.00', Precision exceeds allowed limit",
            "'1000.00', NONE",
            "'1.234', Precision exceeds allowed limit"
    })
    void testOldBalance(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[19] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'', Mandatory field missing",
            "'abc', Invalid decimal value",
            "'123456789012345678.00', Precision exceeds allowed limit",
            "'2000.00', NONE",
            "'1.234', Precision exceeds allowed limit"
    })
    void testNewBalance(String value, String expectedError) {

        String[] fields = baseValidFields();
        fields[20] = value;

        List<TransactionError> errors =
                validationService.validate(fields, getMetaData());

        if (expectedError.equals("NONE")) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            TransactionError error = errors.get(0);
            assertEquals(expectedError, error.getErrorMessage());
        }
    }
}