CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    password VARCHAR(255),
    auth_provider VARCHAR(50) DEFAULT 'LOCAL',
    totp_secret VARCHAR(255),
    totp_enabled BOOLEAN DEFAULT FALSE
);

CREATE TABLE file_meta_data (
    file_id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000),
    user_id BIGINT REFERENCES users(id),
    upload_time TIMESTAMPTZ DEFAULT now(),
    total_records INTEGER,
    success_count INTEGER DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    duplicate_count INTEGER DEFAULT 0,
    status VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    processing_time_ms BIGINT
);

CREATE INDEX idx_filename ON file_meta_data(filename);

CREATE TABLE trade_transaction (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(255),
    record_tracking_id VARCHAR(64),
    file_header_date VARCHAR(255),
    account_number VARCHAR(255),
    transaction_type INTEGER,
    batch_location VARCHAR(255),
    batch_number INTEGER,
    update_batch_date VARCHAR(255),
    related_file_number INTEGER,
    action_name VARCHAR(255),
    related_file_key BIGINT,
    do_not_report_flag VARCHAR(255),
    explanation VARCHAR(255),
    minor_assets_class INTEGER,
    owning_portfolio INTEGER,
    poster_initials VARCHAR(255),
    transaction_subtype INTEGER,
    cash_effect NUMERIC(19, 2),
    cash_paid_out NUMERIC(19, 2),
    broker_number INTEGER,
    old_balance NUMERIC(19, 2),
    new_balance NUMERIC(19, 2),
    row_number INTEGER,
    file_id BIGINT REFERENCES file_meta_data(file_id)
);

CREATE TABLE trade_archive (
    id INTEGER PRIMARY KEY,
    transaction_id VARCHAR(255),
    record_tracking_id VARCHAR(64),
    file_header_date VARCHAR(255),
    account_number VARCHAR(255),
    transaction_type INTEGER,
    batch_location VARCHAR(255),
    batch_number INTEGER,
    update_batch_date VARCHAR(255),
    related_file_number INTEGER,
    action_name VARCHAR(255),
    related_file_key BIGINT,
    do_not_report_flag VARCHAR(255),
    explanation VARCHAR(255),
    minor_assets_class INTEGER,
    owning_portfolio INTEGER,
    poster_initials VARCHAR(255),
    transaction_subtype INTEGER,
    cash_effect NUMERIC(19, 2),
    cash_paid_out NUMERIC(19, 2),
    broker_number INTEGER,
    old_balance NUMERIC(19, 2),
    new_balance NUMERIC(19, 2),
    file_id BIGINT
);

CREATE TABLE transaction_error (
    error_id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(255),
    record_tracking_id VARCHAR(64),
    account_number VARCHAR(255),
    error_field VARCHAR(255),
    error_message VARCHAR(255),
    status VARCHAR(50),
    created_time TIMESTAMP,
    row_number INTEGER,
    file_id BIGINT REFERENCES file_meta_data(file_id)
);

CREATE INDEX idx_transaction_error ON transaction_error(transaction_id, error_field);

CREATE TABLE export_job (
    id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    status VARCHAR(255),
    s3_url VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    downloaded BOOLEAN DEFAULT FALSE,
    downloaded_at TIMESTAMP,
    export_type VARCHAR(255)
);

CREATE TABLE deleted_trade_transaction (
    id INTEGER PRIMARY KEY,
    transaction_id VARCHAR(255),
    record_tracking_id VARCHAR(64),
    file_header_date VARCHAR(255),
    account_number VARCHAR(255),
    transaction_type INTEGER,
    batch_location VARCHAR(255),
    batch_number INTEGER,
    update_batch_date VARCHAR(255),
    related_file_number INTEGER,
    action_name VARCHAR(255),
    related_file_key BIGINT,
    do_not_report_flag VARCHAR(255),
    explanation VARCHAR(255),
    minor_assets_class INTEGER,
    owning_portfolio INTEGER,
    poster_initials VARCHAR(255),
    transaction_subtype INTEGER,
    cash_effect NUMERIC(19, 2),
    cash_paid_out NUMERIC(19, 2),
    broker_number INTEGER,
    old_balance NUMERIC(19, 2),
    new_balance NUMERIC(19, 2),
    row_number INTEGER,
    file_id BIGINT,
    deleted_at TIMESTAMP
);

CREATE TABLE deleted_transaction_error (
    error_id BIGINT PRIMARY KEY,
    transaction_id VARCHAR(255),
    record_tracking_id VARCHAR(64),
    account_number VARCHAR(255),
    error_field VARCHAR(255),
    error_message VARCHAR(255),
    status VARCHAR(50),
    created_time TIMESTAMP,
    row_number INTEGER,
    file_id BIGINT,
    deleted_at TIMESTAMP
);

CREATE TABLE transaction_registry (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(255) NOT NULL,
    user_id BIGINT REFERENCES users(id),
    created_time TIMESTAMP
);

-- Performance indexes
CREATE INDEX idx_trade_transaction_file_id ON trade_transaction(file_id);
CREATE INDEX idx_trade_transaction_txn_id ON trade_transaction(transaction_id);
CREATE INDEX idx_trade_transaction_txn_id_file_id ON trade_transaction(transaction_id, file_id);
CREATE INDEX idx_file_meta_data_status ON file_meta_data(status);
CREATE INDEX idx_file_meta_data_user_id ON file_meta_data(user_id);
CREATE INDEX idx_file_meta_data_user_status ON file_meta_data(user_id, status);
CREATE INDEX idx_transaction_error_file_id ON transaction_error(file_id);
CREATE INDEX idx_transaction_error_status ON transaction_error(status);
CREATE INDEX idx_trade_archive_file_id ON trade_archive(file_id);
CREATE INDEX idx_transaction_registry_txn_id ON transaction_registry(transaction_id);
CREATE INDEX idx_transaction_registry_user_id ON transaction_registry(user_id);
CREATE INDEX idx_export_job_user_id ON export_job(user_id);
CREATE INDEX idx_deleted_trade_transaction_file_id ON deleted_trade_transaction(file_id);
CREATE INDEX idx_deleted_transaction_error_file_id ON deleted_transaction_error(file_id);
