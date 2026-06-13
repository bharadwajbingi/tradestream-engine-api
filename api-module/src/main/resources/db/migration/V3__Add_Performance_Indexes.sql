-- V3: Add performance indexes for frequently queried columns

-- Index on trade_transaction(file_id) for deleteByFileId, findByFileId, streaming queries
CREATE INDEX IF NOT EXISTS idx_trade_transaction_file_id
    ON trade_transaction(file_id);

-- Index on trade_transaction(transaction_id) for duplicate checks
CREATE INDEX IF NOT EXISTS idx_trade_transaction_txn_id
    ON trade_transaction(transaction_id);

-- Composite index for owner-scoped duplicate check queries
CREATE INDEX IF NOT EXISTS idx_trade_transaction_txn_id_file_id
    ON trade_transaction(transaction_id, file_id);

-- Index on file_meta_data(status) for scheduler polling (PENDING files)
CREATE INDEX IF NOT EXISTS idx_file_meta_data_status
    ON file_meta_data(status);

-- Index on file_meta_data(user_id) for ownership queries
CREATE INDEX IF NOT EXISTS idx_file_meta_data_user_id
    ON file_meta_data(user_id);

-- Composite index for user + status queries (scheduler active jobs check)
CREATE INDEX IF NOT EXISTS idx_file_meta_data_user_status
    ON file_meta_data(user_id, status);

-- Index on transaction_error(file_id) for countByMetaData_FileId queries
CREATE INDEX IF NOT EXISTS idx_transaction_error_file_id
    ON transaction_error(file_id);

-- Index on transaction_error(status) for countDistinctByStatus queries
CREATE INDEX IF NOT EXISTS idx_transaction_error_status
    ON transaction_error(status);

-- Index on trade_archive(file_id) for archive queries
CREATE INDEX IF NOT EXISTS idx_trade_archive_file_id
    ON trade_archive(file_id);

-- Index on transaction_registry(transaction_id) for duplicate lookups
CREATE INDEX IF NOT EXISTS idx_transaction_registry_txn_id
    ON transaction_registry(transaction_id);

-- Index on transaction_registry(user_id) for user-scoped queries
CREATE INDEX IF NOT EXISTS idx_transaction_registry_user_id
    ON transaction_registry(user_id);

-- Index on export_job(user_id) for user job listing
CREATE INDEX IF NOT EXISTS idx_export_job_user_id
    ON export_job(user_id);
