BEGIN;

ALTER TABLE tblUsers ADD COLUMN IF NOT EXISTS username_normalized VARCHAR(100);
UPDATE tblUsers
SET username_normalized = LOWER(NORMALIZE(TRIM(username), NFKC))
WHERE username_normalized IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT username_normalized
        FROM tblUsers
        GROUP BY username_normalized
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce canonical usernames: case-insensitive duplicate users exist';
    END IF;
END $$;

ALTER TABLE tblUsers ALTER COLUMN username_normalized SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_tblUsers_username_normalized
    ON tblUsers (username_normalized);

DELETE FROM tblAuthSessions;
ALTER TABLE tblAuthSessions ADD COLUMN IF NOT EXISTS createdAt TIMESTAMP;
ALTER TABLE tblAuthSessions ADD COLUMN IF NOT EXISTS lastAccessedAt TIMESTAMP;
UPDATE tblAuthSessions
SET createdAt = CURRENT_TIMESTAMP,
    lastAccessedAt = CURRENT_TIMESTAMP
WHERE createdAt IS NULL OR lastAccessedAt IS NULL;
ALTER TABLE tblAuthSessions ALTER COLUMN createdAt SET NOT NULL;
ALTER TABLE tblAuthSessions ALTER COLUMN lastAccessedAt SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tblAuthSessions_lastAccessedAt
    ON tblAuthSessions (lastAccessedAt);
CREATE INDEX IF NOT EXISTS idx_tblAuthSessions_userId
    ON tblAuthSessions (userId);

DROP TABLE IF EXISTS tblPasswordResetSessions;
CREATE TABLE tblPasswordResetSessions (
    userId BIGINT PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    expiresAt TIMESTAMP NOT NULL,
    CONSTRAINT fk_tblPasswordResetSessions_user FOREIGN KEY (userId) REFERENCES tblUsers(id)
);
CREATE INDEX idx_tblPasswordResetSessions_expiresAt
    ON tblPasswordResetSessions (expiresAt);

CREATE TABLE IF NOT EXISTS tblAuthenticationRateLimits (
    bucket_key VARCHAR(96) PRIMARY KEY,
    windowStartedAt TIMESTAMP NOT NULL,
    attemptCount INTEGER NOT NULL,
    blockedUntil TIMESTAMP NULL,
    expiresAt TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tblAuthenticationRateLimits_expiresAt
    ON tblAuthenticationRateLimits (expiresAt);

COMMIT;
