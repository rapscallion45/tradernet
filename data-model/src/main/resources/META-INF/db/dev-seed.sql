INSERT INTO tblRoles (name)
SELECT 'ALL Rights'
WHERE NOT EXISTS (SELECT 1 FROM tblRoles WHERE name = 'ALL Rights');

INSERT INTO tblRoles (name)
SELECT 'Admin Rights'
WHERE NOT EXISTS (SELECT 1 FROM tblRoles WHERE name = 'Admin Rights');

INSERT INTO tblRoles (name)
SELECT 'Standard Rights'
WHERE NOT EXISTS (SELECT 1 FROM tblRoles WHERE name = 'Standard Rights');
