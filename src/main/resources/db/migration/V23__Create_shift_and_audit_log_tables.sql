CREATE TABLE IF NOT EXISTS shifts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    initial_cash NUMERIC(12, 2) NOT NULL,
    final_cash NUMERIC(12, 2),
    status VARCHAR(10) NOT NULL,
    CONSTRAINT chk_shifts_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE INDEX IF NOT EXISTS idx_shifts_user_id_start_time ON shifts (user_id, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_shifts_status ON shifts (status);

