DELETE FROM tblRoleResources
WHERE resourceId IN (
    SELECT id FROM tblResources WHERE pathPrefix IN ('signals', 'user-properties', 'health')
);

DELETE FROM tblResources WHERE pathPrefix IN ('signals', 'user-properties', 'health');
