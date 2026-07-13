-- Create auth_logs table for authentication debugging
CREATE TABLE IF NOT EXISTS auth_logs (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100),
    success BOOLEAN NOT NULL,
    error_reason VARCHAR(500),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_auth_logs_username ON auth_logs(username);
CREATE INDEX IF NOT EXISTS idx_auth_logs_created_at ON auth_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_auth_logs_success ON auth_logs(success);
