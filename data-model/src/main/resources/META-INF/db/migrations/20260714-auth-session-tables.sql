CREATE TABLE IF NOT EXISTS tblAuthSessions (
    token VARCHAR(64) PRIMARY KEY,
    userId BIGINT NOT NULL,
    expiresAt TIMESTAMP NOT NULL,
    CONSTRAINT fk_tblAuthSessions_user FOREIGN KEY (userId) REFERENCES tblUsers(id)
);

CREATE INDEX IF NOT EXISTS idx_tblAuthSessions_expiresAt ON tblAuthSessions (expiresAt);

CREATE TABLE IF NOT EXISTS tblPasswordResetSessions (
    token VARCHAR(64) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    expiresAt TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tblPasswordResetSessions_expiresAt ON tblPasswordResetSessions (expiresAt);
