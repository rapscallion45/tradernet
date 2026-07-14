DELETE FROM tblRoleResources
WHERE resourceId IN (
    SELECT id FROM tblResources WHERE pathPrefix = 'passwords' OR name = 'Passwords'
);

DELETE FROM tblResources
WHERE pathPrefix = 'passwords' OR name = 'Passwords';

DROP TABLE IF EXISTS tblPasswords;
