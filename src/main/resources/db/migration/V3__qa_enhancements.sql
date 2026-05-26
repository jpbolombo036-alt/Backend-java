-- Création de la table des scénarios de test
CREATE TABLE IF NOT EXISTS test_cases (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    application_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(id)
);

-- Mise à jour de la table tests (renommée logiquement en test_steps dans le code)
-- On ajoute le lien vers le scénario parent
ALTER TABLE tests ADD COLUMN IF NOT EXISTS test_case_id BIGINT;
ALTER TABLE tests ADD CONSTRAINT fk_test_case FOREIGN KEY (test_case_id) REFERENCES test_cases(id);

-- Nouveau module : Gestion des Bugs (Defects)
CREATE TABLE IF NOT EXISTS bugs (
    id BIGSERIAL PRIMARY KEY,
    test_step_id BIGINT,
    title VARCHAR(255) NOT NULL,
    severity VARCHAR(50), -- CRITICAL, MAJOR, MINOR
    priority VARCHAR(50), -- HIGH, MEDIUM, LOW
    reproducibility TEXT,
    status VARCHAR(50) DEFAULT 'OPEN',
    assigned_to BIGINT,
    created_at TIMESTAMP,
    FOREIGN KEY (test_step_id) REFERENCES tests(id),
    FOREIGN KEY (assigned_to) REFERENCES users(id)
);