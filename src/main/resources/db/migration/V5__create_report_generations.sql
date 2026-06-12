CREATE TABLE IF NOT EXISTS report_generations (
    id BIGSERIAL PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50),
    generated_at TIMESTAMP,
    generated_by BIGINT,
    generated_by_username VARCHAR(255),
    content TEXT
);
