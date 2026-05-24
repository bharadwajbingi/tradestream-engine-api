package com.mphasis.tse.specification;

import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.enums.ErrorStatus;
import org.springframework.data.jpa.domain.Specification;

public class TransactionErrorSpecification {

    private TransactionErrorSpecification(){}

    public static Specification<TransactionError> hasFileLoadId(Long fileLoadId) {
        return (root, query, cb) ->
                cb.equal(root.get("metaData").get("fileId"), fileLoadId);
    }

    public static Specification<TransactionError> belongsToUser(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get("metaData").get("user").get("id"), userId);
    }

    public static Specification<TransactionError> hasTransactionId(String transactionId) {
        return (root, query, cb) ->
                cb.equal(
                        cb.lower(root.get("transactionId")),
                        transactionId.toLowerCase()
                );
    }

    public static Specification<TransactionError> hasAccountNumber(String accountNumber) {
        return (root, query, cb) ->
                cb.equal(
                        cb.lower(root.get("accountNumber")),
                        accountNumber.toLowerCase()
                );
    }

    public static Specification<TransactionError> hasErrorField(String errorField) {
        return (root, query, cb) ->
                cb.equal(
                        cb.lower(root.get("errorField")),
                        errorField.toLowerCase()
                );
    }

    public static Specification<TransactionError> hasStatus(ErrorStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<TransactionError> hasGlobalSearchTerm(String searchTerm) {
        return (root, query, cb) -> {
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("transactionId")), pattern),
                    cb.like(cb.lower(root.get("accountNumber")), pattern),
                    cb.like(cb.lower(root.get("errorMessage")), pattern),
                    cb.like(cb.lower(root.get("errorField")), pattern),
                    cb.like(cb.lower(root.get("metaData").get("filename")), pattern)
            );
        };
    }
}
