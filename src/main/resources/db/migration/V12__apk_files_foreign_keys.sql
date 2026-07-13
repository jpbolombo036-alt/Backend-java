-- Add referential integrity to apk_files (PostgreSQL compatible)
-- NOT VALID is used so the migration always succeeds even if orphan rows exist;
-- new inserts/updates are enforced. Once data is cleaned, run VALIDATE CONSTRAINT.

ALTER TABLE apk_files
    ADD CONSTRAINT fk_apk_application
    FOREIGN KEY (application_id)
    REFERENCES applications (id)
    ON DELETE SET NULL
    NOT VALID;

ALTER TABLE apk_files
    ADD CONSTRAINT fk_apk_uploaded_by
    FOREIGN KEY (uploaded_by)
    REFERENCES users (id)
    ON DELETE SET NULL
    NOT VALID;
