package com.mphasis.tse.mapper;

import com.mphasis.tse.dto.TransactionErrorResponse;
import com.mphasis.tse.entity.TransactionError;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TransactionErrorMapper {

    @Mapping(source = "metaData.fileId", target = "fileId")
    TransactionErrorResponse toDto(TransactionError entity);

    List<TransactionErrorResponse> toDtoList(List<TransactionError> entities);
}

