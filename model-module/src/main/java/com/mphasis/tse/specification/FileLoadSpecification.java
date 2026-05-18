package com.mphasis.tse.specification;

import com.mphasis.tse.entity.FileLoadMetaData;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;


public class FileLoadSpecification {

    public static Specification<FileLoadMetaData> hasFileId(Long fileId) {
        return (root, query, cb) ->
                cb.equal(root.get("fileId"), fileId);
    }

    public static Specification<FileLoadMetaData> hasFilename(String filename) {
        return (root, query, cb) ->
                cb.equal(
                        cb.lower(root.get("filename")),
                        filename.toLowerCase()
                );
    }

    public static Specification<FileLoadMetaData> hasStatus(String status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<FileLoadMetaData> hasUploadTimeBetween(
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, cb) ->
                cb.between(root.get("uploadTime"), startDate, endDate);
    }


    public static Specification<FileLoadMetaData> hasErrorStatus(String errorStatus) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Object, Object> errorJoin = root.join("errors", JoinType.LEFT);
            return cb.equal(errorJoin.get("status"), errorStatus);
        };
    }

    public static Specification<FileLoadMetaData> hasErrorMessage(String errorMessage) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Object, Object> errorJoin = root.join("errors", JoinType.LEFT);
            return cb.like(
                    cb.lower(errorJoin.get("errorMessage")),
                    "%" + errorMessage.toLowerCase() + "%"
            );
        };
    }
}
