-- Add update_date column to apk_files (PostgreSQL compatible)
ALTER TABLE apk_files ADD COLUMN IF NOT EXISTS update_date TIMESTAMP;
