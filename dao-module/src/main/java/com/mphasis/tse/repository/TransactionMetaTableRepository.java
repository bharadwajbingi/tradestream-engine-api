package com.mphasis.tse.repository;
import com.mphasis.tse.entity.FileLoadMetaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionMetaTableRepository
        extends JpaRepository<FileLoadMetaData, Long>,
        JpaSpecificationExecutor<FileLoadMetaData> {
}

