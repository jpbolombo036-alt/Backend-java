CREATE TABLE IF NOT EXISTS document_archives (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    description TEXT,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    tags TEXT,
    author VARCHAR(255),
    uploaded_by BIGINT NOT NULL,
    uploaded_by_username VARCHAR(100),
    upload_date TIMESTAMP,
    download_count INTEGER DEFAULT 0
);
