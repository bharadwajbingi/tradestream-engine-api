CREATE TABLE IF NOT EXISTS deleted_trade_transaction (
    id INTEGER PRIMARY KEY,
    transaction_id VARCHAR(255),
    record_tracking_id VARCHAR(64),
    file_header_date VARCHAR(32),
    account_number VARCHAR(255),
    transaction_type INTEGER,
    batch_location VARCHAR(255),
    batch_number INTEGER,
    update_batch_date VARCHAR(32),
    related_file_number INTEGER,
    action_name VARCHAR(255),
    related_file_key BIGINT,
    do_not_report_flag VARCHAR(8),
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

CREATE INDEX IF NOT EXISTS idx_deleted_trade_transaction_file_id
    ON deleted_trade_transaction(file_id);

CREATE INDEX IF NOT EXISTS idx_deleted_trade_transaction_tracking
    ON deleted_trade_transaction(record_tracking_id);

CREATE TABLE IF NOT EXISTS deleted_transaction_error (
    error_id BIGINT PRIMARY KEY,
    transaction_id VARCHAR(255),
    record_tracking_id VARCHAR(64),
    account_number VARCHAR(255),
    error_field VARCHAR(255),
    error_message VARCHAR(500),
    status VARCHAR(40),
    created_time TIMESTAMP,
    row_number INTEGER,
    file_id BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_deleted_transaction_error_file_id
    ON deleted_transaction_error(file_id);

CREATE INDEX IF NOT EXISTS idx_deleted_transaction_error_tracking
    ON deleted_transaction_error(record_tracking_id);
