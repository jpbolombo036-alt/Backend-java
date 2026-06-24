CREATE TABLE IF NOT EXISTS attendances (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    agent_username VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    check_in_time TIME,
    check_out_time TIME,
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    reason TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_agent_date UNIQUE (agent_id, date)
);

CREATE INDEX IF NOT EXISTS idx_attendances_agent_id ON attendances(agent_id);
CREATE INDEX IF NOT EXISTS idx_attendances_date ON attendances(date);
CREATE INDEX IF NOT EXISTS idx_attendances_status ON attendances(status);
