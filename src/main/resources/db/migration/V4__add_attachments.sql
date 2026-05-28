<<<<<<< HEAD
-- Table pour stocker les métadonnées des fichiers joints (Screenshots, Logs, etc.)
CREATE TABLE IF NOT EXISTS attachments (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    bug_id BIGINT,
    test_step_id BIGINT,
    created_at TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (bug_id) REFERENCES bugs(id),
    FOREIGN KEY (test_step_id) REFERENCES tests(id)
=======
-- Table pour stocker les métadonnées des fichiers joints (Screenshots, Logs, etc.)
CREATE TABLE IF NOT EXISTS attachments (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    bug_id BIGINT,
    test_step_id BIGINT,
    created_at TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (bug_id) REFERENCES bugs(id),
    FOREIGN KEY (test_step_id) REFERENCES test_steps(id)
>>>>>>> 600760be4eddc08aedfb158f3a1521a71faeebf0
);