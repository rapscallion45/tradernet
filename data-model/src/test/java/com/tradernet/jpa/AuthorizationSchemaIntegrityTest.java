package com.tradernet.jpa;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationSchemaIntegrityTest {

    @Test
    void schemaRejectsAmbiguousAuthorizationPolicyIdentity() throws Exception {
        try (var connection = DriverManager.getConnection(
            "jdbc:h2:mem:authorization-policy;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE",
            "sa",
            ""
        ); var schema = AuthorizationSchemaIntegrityTest.class.getResourceAsStream("/META-INF/db/schema.sql")) {
            RunScript.execute(connection, new InputStreamReader(schema, StandardCharsets.UTF_8));

            connection.createStatement().executeUpdate("INSERT INTO tblRoles(name) VALUES ('Operations')");
            assertThrows(
                SQLException.class,
                () -> connection.createStatement().executeUpdate("INSERT INTO tblRoles(name) VALUES ('Operations')")
            );

            connection.createStatement().executeUpdate("INSERT INTO tblGroups(name) VALUES ('Operators')");
            assertThrows(
                SQLException.class,
                () -> connection.createStatement().executeUpdate("INSERT INTO tblGroups(name) VALUES ('Operators')")
            );

            connection.createStatement().executeUpdate(
                "INSERT INTO tblResources(name, pathPrefix, httpMethod) VALUES ('Market read', 'market', 'GET')"
            );
            assertThrows(
                SQLException.class,
                () -> connection.createStatement().executeUpdate(
                    "INSERT INTO tblResources(name, pathPrefix, httpMethod) "
                        + "VALUES ('Market read', 'other-market', 'GET')"
                )
            );
            assertThrows(
                SQLException.class,
                () -> connection.createStatement().executeUpdate(
                    "INSERT INTO tblResources(name, pathPrefix, httpMethod) "
                        + "VALUES ('Duplicate market policy', 'market', 'GET')"
                )
            );
            assertThrows(
                SQLException.class,
                () -> connection.createStatement().executeUpdate(
                    "INSERT INTO tblResources(name, pathPrefix, httpMethod) "
                        + "VALUES ('Noncanonical market policy', 'other-market', ' get ')"
                )
            );
        }
    }
}
