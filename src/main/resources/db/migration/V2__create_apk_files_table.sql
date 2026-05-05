-- APK Files table (PostgreSQL compatible)
CREATE TABLE IF NOT EXISTS apk_files (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    version VARCHAR(50),
    package_name VARCHAR(100),
    description TEXT,
    application_id BIGINT,
    uploaded_by BIGINT,
    upload_date TIMESTAMP,
    download_count INTEGER DEFAULT 0
);
