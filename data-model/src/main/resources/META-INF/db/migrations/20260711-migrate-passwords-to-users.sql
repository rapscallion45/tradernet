ALTER TABLE tblUsers ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

DO $$
BEGIN
    IF to_regclass('tblpasswords') IS NOT NULL THEN
        UPDATE tblUsers
        SET password_hash = (
            SELECT p.passwordHash
            FROM tblPasswords p
            WHERE p.userId = tblUsers.id
              AND p.passwordHash IS NOT NULL
              AND TRIM(p.passwordHash) <> ''
              AND (
                  p.passwordHash LIKE '$2a$%'
                  OR p.passwordHash LIKE '$2b$%'
                  OR p.passwordHash LIKE '$2y$%'
              )
            ORDER BY p.lastChanged DESC, p.id DESC
            LIMIT 1
        )
        WHERE (password_hash IS NULL OR TRIM(password_hash) = '')
          AND EXISTS (
              SELECT 1
              FROM tblPasswords p
              WHERE p.userId = tblUsers.id
                AND p.passwordHash IS NOT NULL
                AND TRIM(p.passwordHash) <> ''
                AND (
                    p.passwordHash LIKE '$2a$%'
                    OR p.passwordHash LIKE '$2b$%'
                    OR p.passwordHash LIKE '$2y$%'
                )
          );
    END IF;
END $$;
