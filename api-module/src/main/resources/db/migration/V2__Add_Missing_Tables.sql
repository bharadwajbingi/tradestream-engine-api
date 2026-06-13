-- V2: Create tables that may be missing from databases that were previously managed by ddl-auto: update
-- These tables are defined in V1 but may not exist if Flyway baselined an incomplete schema.

CREATE TABLE IF NOT EXISTS deleted_trade_transaction (
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

CREATE TABLE IF NOT EXISTS deleted_transaction_error (
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

-- Indexes for deleted_trade_transaction
CREATE INDEX IF NOT EXISTS idx_deleted_trade_transaction_file_id
    ON deleted_trade_transaction(file_id);

CREATE INDEX IF NOT EXISTS idx_deleted_trade_transaction_tracking
    ON deleted_trade_transaction(record_tracking_id);

-- Indexes for deleted_transaction_error
CREATE INDEX IF NOT EXISTS idx_deleted_transaction_error_file_id
    ON deleted_transaction_error(file_id);

CREATE INDEX IF NOT EXISTS idx_deleted_transaction_error_tracking
    ON deleted_transaction_error(record_tracking_id);
