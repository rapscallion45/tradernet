CREATE INDEX IF NOT EXISTS idx_tblResources_path_method
    ON tblResources (pathPrefix, httpMethod);
