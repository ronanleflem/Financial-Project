package finance.project.api.migrations;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;

class RunAuditFlywayMigrationTest {

    @Test
    void migrationCreatesTablesAndIndexes() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:run_audit;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        ds.setUser("sa");
        ds.setPassword("");

        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/run-audit-migration")
                .load()
                .migrate();

        try (Connection conn = ds.getConnection()) {
            assertTableExists(conn, "RUN_REQUEST");
            assertTableExists(conn, "RUN_EXECUTION");
            assertTableExists(conn, "RUN_ERROR");
            assertTableExists(conn, "RUN_ARTIFACT");

            assertIndexExists(conn, "RUN_REQUEST", "IDX_RUN_REQUEST_REQUEST_ID");
            assertIndexExists(conn, "RUN_EXECUTION", "IDX_RUN_EXEC_REQUEST_ID");
            assertIndexExists(conn, "RUN_ERROR", "IDX_RUN_ERROR_REQUEST_ID");
            assertIndexExists(conn, "RUN_ARTIFACT", "IDX_RUN_ARTIFACT_REQUEST_ID");
        }
    }

    private void assertTableExists(Connection conn, String tableName) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?"
        )) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                assertTrue(count > 0, "Expected table to exist: " + tableName);
            }
        }
    }

    private void assertIndexExists(Connection conn, String tableName, String indexName) throws Exception {
        ArrayList<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = ?"
        )) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
            }
        }
        boolean found = names.stream().anyMatch(n -> n != null && n.equalsIgnoreCase(indexName));
        assertTrue(found, "Expected index to exist: " + tableName + "." + indexName);
    }
}
