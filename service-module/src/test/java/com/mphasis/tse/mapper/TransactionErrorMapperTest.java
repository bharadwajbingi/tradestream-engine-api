package com.mphasis.tse.mapper;

import com.mphasis.tse.dto.TransactionErrorResponse;
import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.enums.ErrorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionErrorMapperTest {

    private TransactionErrorMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(TransactionErrorMapper.class);
    }

    @Test
    void testToDto_nullEntity() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void testToDto_metaDataNull() {
        TransactionError entity = new TransactionError();
        entity.setMetaData(null);

        TransactionErrorResponse response = mapper.toDto(entity);

        assertNotNull(response);
        assertNull(response.getFileId());
    }

    @Test
    void testToDto_fileIdNull() {
        FileLoadMetaData meta = new FileLoadMetaData();
        meta.setFileId(null);

        TransactionError entity = new TransactionError();
        entity.setMetaData(meta);

        TransactionErrorResponse response = mapper.toDto(entity);

        assertNotNull(response);
        assertNull(response.getFileId());
    }

    @Test
    void testToDto_fullData_withStatus() {
        FileLoadMetaData meta = new FileLoadMetaData();
        meta.setFileId(10L);

        TransactionError entity = new TransactionError();
        entity.setMetaData(meta);
        entity.setErrorId(1L);
        entity.setTransactionId("TXN123");
        entity.setAccountNumber("ACC001");
        entity.setErrorField("field");
        entity.setErrorMessage("error");
        entity.setStatus(ErrorStatus.FAILED);

        TransactionErrorResponse response = mapper.toDto(entity);

        assertNotNull(response);
        assertEquals(10L, response.getFileId());
        assertEquals(1L, response.getErrorId());
        assertEquals("TXN123", response.getTransactionId());
        assertEquals("ACC001", response.getAccountNumber());
        assertEquals("field", response.getErrorField());
        assertEquals("error", response.getErrorMessage());
        assertEquals("FAILED", response.getStatus());
    }

    @Test
    void testToDto_statusNull() {
        FileLoadMetaData meta = new FileLoadMetaData();
        meta.setFileId(20L);

        TransactionError entity = new TransactionError();
        entity.setMetaData(meta);
        entity.setStatus(null);

        TransactionErrorResponse response = mapper.toDto(entity);

        assertNotNull(response);
        assertEquals(20L, response.getFileId());
        assertNull(response.getStatus());
    }

    @Test
    void testToDtoList_null() {
        assertNull(mapper.toDtoList(null));
    }

    @Test
    void testToDtoList_empty() {
        List<TransactionErrorResponse> list =
                mapper.toDtoList(Collections.emptyList());

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void testToDtoList_mixedData() {

        FileLoadMetaData meta1 = new FileLoadMetaData();
        meta1.setFileId(1L);

        TransactionError e1 = new TransactionError();
        e1.setMetaData(meta1);
        e1.setStatus(ErrorStatus.RESOLVED);

        TransactionError e2 = new TransactionError();
        e2.setMetaData(null);
        e2.setStatus(null);

        List<TransactionErrorResponse> list =
                mapper.toDtoList(Arrays.asList(e1, e2));

        assertEquals(2, list.size());
        assertEquals("RESOLVED", list.get(0).getStatus());
        assertNull(list.get(1).getStatus());
    }

    @Test
    void testEntityMetaDataFileId_nullTransactionError() throws Exception {

        TransactionErrorMapperImpl impl = new TransactionErrorMapperImpl();

        Method method = TransactionErrorMapperImpl.class
                .getDeclaredMethod("entityMetaDataFileId", TransactionError.class);

        method.setAccessible(true);

        Object result = method.invoke(impl, new Object[]{null});

        assertNull(result);
    }
}