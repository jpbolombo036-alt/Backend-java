CREATE TABLE IF NOT EXISTS bloc_notes (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    content TEXT NOT NULL,
    application_id BIGINT,
    session_id BIGINT,
    test_id BIGINT,
    status VARCHAR(50),
    created_by BIGINT,
    created_by_username VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
