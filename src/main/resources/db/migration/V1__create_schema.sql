-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    hashed_password VARCHAR(255) NOT NULL,
    role VARCHAR(50),
    is_active BOOLEAN,
    profile_photo VARCHAR(1000),
    created_at TIMESTAMP
);

-- Applications table
CREATE TABLE IF NOT EXISTS applications (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    description TEXT,
    version VARCHAR(50),
    environnement VARCHAR(50),
    date_creation TIMESTAMP,
    created_by BIGINT
);

-- Comptes table
CREATE TABLE IF NOT EXISTS comptes (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT,
    username VARCHAR(100) NOT NULL,
    code TEXT,
    role VARCHAR(50),
    commentaire TEXT,
    created_by BIGINT,
    FOREIGN KEY (application_id) REFERENCES applications(id)
);

-- Habilitations table
CREATE TABLE IF NOT EXISTS habilitations (
    id BIGSERIAL PRIMARY KEY,
    compte_id BIGINT,
    permission VARCHAR(100) NOT NULL,
    FOREIGN KEY (compte_id) REFERENCES comptes(id)
);

-- Logs table
CREATE TABLE IF NOT EXISTS logs (
    id BIGSERIAL PRIMARY KEY,
    action TEXT NOT NULL,
    date TIMESTAMP
);

-- Settings table
CREATE TABLE IF NOT EXISTS settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL
);

-- Test sessions table
CREATE TABLE IF NOT EXISTS test_sessions (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(200) NOT NULL,
    description TEXT,
    application_id BIGINT,
    environnement VARCHAR(50),
    version VARCHAR(50),
    nom_document VARCHAR(200),
    date_creation TIMESTAMP,
    statut VARCHAR(50),
    created_by BIGINT,
    FOREIGN KEY (application_id) REFERENCES applications(id)
);

-- Tests table
CREATE TABLE IF NOT EXISTS tests (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT,
    application_id BIGINT,
    application_nom VARCHAR(100),
    version VARCHAR(50),
    environnement VARCHAR(50),
    fonction VARCHAR(200) NOT NULL,
    precondition TEXT,
    etapes TEXT,
    resultat_attendu TEXT,
    resultat_obtenu TEXT,
    statut VARCHAR(50),
    commentaires TEXT,
    created_by BIGINT,
    FOREIGN KEY (session_id) REFERENCES test_sessions(id),
    FOREIGN KEY (application_id) REFERENCES applications(id)
);

-- Todos table
CREATE TABLE IF NOT EXISTS todos (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(20) DEFAULT 'normal',
    due_date VARCHAR(20),
    created_at TIMESTAMP,
    created_by BIGINT
);
