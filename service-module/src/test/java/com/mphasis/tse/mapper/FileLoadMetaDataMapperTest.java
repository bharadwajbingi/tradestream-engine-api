package com.mphasis.tse.mapper;

import com.mphasis.tse.dto.FileLoadMetaDataResponse;
import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.enums.FileStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class FileLoadMetaDataMapperTest {
    private final FileLoadMetaDataMapper mapper =
            Mappers.getMapper(FileLoadMetaDataMapper.class);
    @Test
    void testToResponse_success() {
        FileLoadMetaData entity = new FileLoadMetaData();
        entity.setFileId(100L);
        entity.setStatus(FileStatus.COMPLETED); // enum
        FileLoadMetaDataResponse response = mapper.toResponse(entity);
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("COMPLETED", response.getStatus());
    }
    @Test
    void testToResponse_nullStatus() {
        FileLoadMetaData entity = new FileLoadMetaData();
        entity.setFileId(200L);
        entity.setStatus(null);
        FileLoadMetaDataResponse response = mapper.toResponse(entity);
        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertNull(response.getStatus()); // important branch
    }
    @Test
    void testToResponseList() {
        FileLoadMetaData e1 = new FileLoadMetaData();
        e1.setFileId(1L);
        e1.setStatus(FileStatus.COMPLETED);
        FileLoadMetaData e2 = new FileLoadMetaData();
        e2.setFileId(2L);
        e2.setStatus(FileStatus.FAILED);
        List<FileLoadMetaDataResponse> result =
                mapper.toResponseList(Arrays.asList(e1, e2));
        assertEquals(2, result.size());
        assertEquals("COMPLETED", result.get(0).getStatus());
        assertEquals("FAILED", result.get(1).getStatus());
    }

    @Test
    void testToResponse_withOptionalFields() {
        FileLoadMetaData entity = new FileLoadMetaData();
        entity.setFileId(300L);
        entity.setTotalRecords(50);
        entity.setSuccessCount(40);
        entity.setErrorCount(10);
        entity.setStatus(FileStatus.COMPLETED);
        FileLoadMetaDataResponse response = mapper.toResponse(entity);
        assertNotNull(response);
        assertEquals(50, response.getTotalRecords());
        assertEquals(40, response.getSuccessCount());
        assertEquals(10, response.getErrorCount());
    }

    @Test
    void testToResponse_nullEntity() {
        FileLoadMetaDataResponse response = mapper.toResponse(null);
        assertNull(response);
    }

    @Test
    void testToResponseList_nullInput() {
        List<FileLoadMetaDataResponse> result =
                mapper.toResponseList(null);
        assertNull(result);
    }
}
