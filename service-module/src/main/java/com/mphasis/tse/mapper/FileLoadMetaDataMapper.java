package com.mphasis.tse.mapper;

import com.mphasis.tse.dto.FileLoadMetaDataResponse;
import com.mphasis.tse.entity.FileLoadMetaData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileLoadMetaDataMapper {

    @Mapping(source = "fileId", target = "id")
    @Mapping(target = "status", expression = "java(fileLoadMetaData.getStatus() != null ? fileLoadMetaData.getStatus().name() : null)")
    FileLoadMetaDataResponse toResponse(FileLoadMetaData fileLoadMetaData);

    List<FileLoadMetaDataResponse> toResponseList(List<FileLoadMetaData> fileLoadMetaDataList);


}