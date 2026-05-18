package com.mphasis.tse.mapper;

import com.mphasis.tse.entity.TradeTransaction;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class TradeTransactionMapperTest {

    private final TradeTransactionMapper mapper =
            Mappers.getMapper(TradeTransactionMapper.class);
    @Test
    void testToEntity_success() {

        String[] row = {

                "TXN001", "20240101", "ACC123",
                "1", "LOC1", "100",
                "20240102", "200",
                "ACTION", "300",
                "Y", "Explanation",
                "2", "3",
                "AB", "4",
                "1000.50", "500.25",
                "10", "2000.75", "3000.90"

        };

        TradeTransaction result = mapper.toEntity(row);

        assertNotNull(result);
        assertEquals("TXN001", result.getTransactionId());
        assertEquals("20240101", result.getFileHeaderDate());
        assertEquals("ACC123", result.getAccountNumber());
        assertEquals(1, result.getTransactionType());
        assertEquals(100, result.getBatchNumber());
        assertEquals(200, result.getRelatedFileNumber());
        assertEquals(300L, result.getRelatedFileKey());
        assertEquals("Y", result.getDoNotReportFlag());
        assertEquals(new BigDecimal("1000.50"), result.getCashEffect());
        assertEquals(new BigDecimal("3000.90"), result.getNewBalance());

    }

    @Test
    void testToEntity_invalidNumberFormat() {
        String[] row = new String[21];
        row[3] = "INVALID";
        assertThrows(NumberFormatException.class, () -> {
            mapper.toEntity(row);
        });
    }

}
 