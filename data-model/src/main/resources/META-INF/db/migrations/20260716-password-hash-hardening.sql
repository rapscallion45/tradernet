ALTER TABLE tblUsers ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

UPDATE tblUsers
SET changePasswordNextLogin = TRUE
WHERE password_hash IS NULL OR TRIM(password_hash) = '';
