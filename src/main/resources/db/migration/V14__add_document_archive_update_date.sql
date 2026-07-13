-- Add update_date column to document_archives (PostgreSQL compatible)
ALTER TABLE document_archives ADD COLUMN IF NOT EXISTS update_date TIMESTAMP;
