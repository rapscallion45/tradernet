BEGIN;

UPDATE tblResources
SET httpMethod = CASE
    WHEN httpMethod IS NULL OR TRIM(httpMethod) = '' THEN '*'
    ELSE UPPER(TRIM(httpMethod))
END;

DO $$
BEGIN
    IF EXISTS (SELECT name FROM tblRoles GROUP BY name HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Cannot enforce role identity: duplicate role names exist';
    END IF;
    IF EXISTS (SELECT name FROM tblGroups GROUP BY name HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Cannot enforce group identity: duplicate group names exist';
    END IF;
    IF EXISTS (SELECT name FROM tblResources GROUP BY name HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Cannot enforce resource identity: duplicate resource names exist';
    END IF;
    IF EXISTS (
        SELECT pathPrefix, httpMethod
        FROM tblResources
        GROUP BY pathPrefix, httpMethod
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce resource policy identity: duplicate path/method rules exist';
    END IF;
END $$;

ALTER TABLE tblRoles ALTER COLUMN name SET NOT NULL;
ALTER TABLE tblGroups ALTER COLUMN name SET NOT NULL;
ALTER TABLE tblResources ALTER COLUMN name SET NOT NULL;
ALTER TABLE tblResources ALTER COLUMN pathPrefix SET NOT NULL;
ALTER TABLE tblResources ALTER COLUMN httpMethod SET DEFAULT '*';
ALTER TABLE tblResources ALTER COLUMN httpMethod SET NOT NULL;
ALTER TABLE tblResources ADD CONSTRAINT ck_tblResources_httpMethod_canonical
    CHECK (httpMethod = '*' OR httpMethod = UPPER(TRIM(httpMethod)));

CREATE UNIQUE INDEX IF NOT EXISTS ux_tblRoles_name ON tblRoles (name);
CREATE UNIQUE INDEX IF NOT EXISTS ux_tblGroups_name ON tblGroups (name);
CREATE UNIQUE INDEX IF NOT EXISTS ux_tblResources_name ON tblResources (name);
CREATE UNIQUE INDEX IF NOT EXISTS ux_tblResources_path_method
    ON tblResources (pathPrefix, httpMethod);

COMMIT;
