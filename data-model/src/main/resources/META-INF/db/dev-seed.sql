INSERT INTO tblRoles (name)
SELECT 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM tblRoles WHERE name = 'ADMIN');

INSERT INTO tblUsers (
    id,
    username,
    password_hash,
    type,
    accountExpiry,
    emailAddress,
    passwordNoExpire,
    lastLogin,
    incorrectLoginAttempts,
    bypassLockout,
    changePasswordNextLogin,
    fullName,
    bypassDocumentSecurity,
    isExternalIdentity
)
SELECT
    1,
    'admin',
    'ChangeMe',
    0,
    NULL,
    'admin@tradernet.local',
    FALSE,
    NULL,
    0,
    TRUE,
    TRUE,
    'Administrator',
    FALSE,
    FALSE
WHERE NOT EXISTS (SELECT 1 FROM tblUsers WHERE LOWER(username) = 'admin');

INSERT INTO tblUserRoles (userId, roleId)
SELECT u.id, r.id
FROM tblUsers u
JOIN tblRoles r ON r.name = 'ADMIN'
WHERE LOWER(u.username) = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM tblUserRoles ur WHERE ur.userId = u.id AND ur.roleId = r.id
  );
