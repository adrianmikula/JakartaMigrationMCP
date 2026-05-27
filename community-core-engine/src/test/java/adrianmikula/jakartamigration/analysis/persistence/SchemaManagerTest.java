package adrianmikula.jakartamigration.analysis.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for the custom JDBC-based schema manager.
 * Replaces LiquibaseScopeInitializationTest with simpler, more reliable tests.
 */
class SchemaManagerTest {

    @TempDir
    Path tempDir;

    private Connection getConnection(Path dbPath) throws Exception {
        String url = "jdbc:sqlite:" + dbPath.toString();
        return DriverManager.getConnection(url);
    }

    @Test
    @DisplayName("Should create schema from scratch")
    void shouldCreateSchemaFromScratch() throws Exception {
        Path dbPath = tempDir.resolve("test-schema.db");

        try (Connection conn = getConnection(dbPath)) {
            SchemaManager schemaManager = new SchemaManager(conn);
            assertThatNoException().isThrownBy(() -> schemaManager.initializeSchema());
        }

        // Verify database file was created
        assertThat(dbPath).exists();

        // Verify tables were created
        try (Connection conn = getConnection(dbPath);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='repositories'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("name")).isEqualTo("repositories");
        }
    }

    @Test
    @DisplayName("Should track schema version")
    void shouldTrackSchemaVersion() throws Exception {
        Path dbPath = tempDir.resolve("test-version.db");

        try (Connection conn = getConnection(dbPath)) {
            SchemaManager schemaManager = new SchemaManager(conn);
            schemaManager.initializeSchema();

            // Verify schema version was set
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT value FROM metadata WHERE key = 'schema_version'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("value")).isEqualTo("2");
            }
        }
    }

    @Test
    @DisplayName("Should be idempotent - running twice should not fail")
    void shouldBeIdempotent() throws Exception {
        Path dbPath = tempDir.resolve("test-idempotent.db");

        try (Connection conn = getConnection(dbPath)) {
            SchemaManager schemaManager = new SchemaManager(conn);

            // First run
            schemaManager.initializeSchema();

            // Second run - should not fail
            assertThatNoException().isThrownBy(() -> schemaManager.initializeSchema());

            // Verify version is still correct
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT value FROM metadata WHERE key = 'schema_version'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("value")).isEqualTo("2");
            }
        }
    }

    @Test
    @DisplayName("Should handle migration from Liquibase-managed database")
    void shouldHandleLiquibaseMigration() throws Exception {
        Path dbPath = tempDir.resolve("test-liquibase-migration.db");

        try (Connection conn = getConnection(dbPath)) {
            // Simulate old Liquibase database
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE DATABASECHANGELOG (id TEXT)");
                stmt.execute("CREATE TABLE repositories (id INTEGER)");
            }

            // SchemaManager should handle this gracefully
            SchemaManager schemaManager = new SchemaManager(conn);
            assertThatNoException().isThrownBy(() -> schemaManager.initializeSchema());

            // Verify schema version was set
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT value FROM metadata WHERE key = 'schema_version'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("value")).isEqualTo("2");
            }
        }
    }

    @Test
    @DisplayName("Should create all required tables")
    void shouldCreateAllRequiredTables() throws Exception {
        Path dbPath = tempDir.resolve("test-all-tables.db");

        String[] expectedTables = {
            "metadata",
            "repositories",
            "org_dependencies",
            "dependencies",
            "dependency_edges",
            "analysis_reports",
            "blockers",
            "recommendations",
            "plugin_state",
            "recipe_executions",
            "recipes",
            "upgrade_recommendations"
        };

        try (Connection conn = getConnection(dbPath)) {
            SchemaManager schemaManager = new SchemaManager(conn);
            schemaManager.initializeSchema();

            for (String tableName : expectedTables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                             "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'")) {
                    assertThat(rs.next())
                        .withFailMessage("Table '%s' should exist", tableName)
                        .isTrue();
                }
            }
        }
    }

    @Test
    @DisplayName("Should create required indexes")
    void shouldCreateRequiredIndexes() throws Exception {
        Path dbPath = tempDir.resolve("test-indexes.db");

        String[] expectedIndexes = {
            "idx_deps_repo",
            "idx_deps_group",
            "idx_org_deps",
            "idx_edges_from",
            "idx_edges_to",
            "idx_blockers_repo",
            "idx_recommendations_repo",
            "idx_recipe_exec_repo"
        };

        try (Connection conn = getConnection(dbPath)) {
            SchemaManager schemaManager = new SchemaManager(conn);
            schemaManager.initializeSchema();

            for (String indexName : expectedIndexes) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                             "SELECT name FROM sqlite_master WHERE type='index' AND name='" + indexName + "'")) {
                    assertThat(rs.next())
                        .withFailMessage("Index '%s' should exist", indexName)
                        .isTrue();
                }
            }
        }
    }

    @Test
    @DisplayName("Should delete scan data tables on upgrade while preserving recipe history")
    void shouldDeleteScanDataTablesOnUpgrade() throws Exception {
        Path dbPath = tempDir.resolve("test-upgrade-delete.db");

        // Create old schema (version 1) using SchemaManager
        try (Connection conn = getConnection(dbPath)) {
            SchemaManager schemaManager = new SchemaManager(conn);
            schemaManager.initializeSchema();
            
            // Manually set version to 1 to simulate old schema
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("UPDATE metadata SET value = '1' WHERE key = 'schema_version'");
            }
        }

        // Verify old tables exist
        try (Connection conn = getConnection(dbPath)) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
                java.util.Set<String> tables = new java.util.HashSet<>();
                while (rs.next()) {
                    tables.add(rs.getString("name"));
                }
                assertThat(tables).contains("repositories", "dependencies", "analysis_reports", "recipes", "recipe_executions");
            }
        }

        // Run upgrade to version 2
        try (Connection conn = getConnection(dbPath)) {
            SchemaManager schemaManager = new SchemaManager(conn);
            schemaManager.initializeSchema();
        }

        // Verify scan data tables were deleted
        try (Connection conn = getConnection(dbPath)) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
                java.util.Set<String> tables = new java.util.HashSet<>();
                while (rs.next()) {
                    tables.add(rs.getString("name"));
                }
                assertThat(tables).doesNotContain("repositories", "dependencies", "analysis_reports");
                // Recipe history should be preserved
                assertThat(tables).contains("recipes", "recipe_executions");
                // Metadata should be preserved
                assertThat(tables).contains("metadata");
            }

            // Verify schema version was updated
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT value FROM metadata WHERE key = 'schema_version'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("value")).isEqualTo("2");
            }
        }
    }
}
