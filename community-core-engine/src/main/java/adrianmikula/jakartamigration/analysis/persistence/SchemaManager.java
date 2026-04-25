package adrianmikula.jakartamigration.analysis.persistence;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Simple JDBC-based schema management for SQLite database.
 * Replaces Liquibase to avoid ClassLoader issues in IntelliJ plugin environments.
 */
@Slf4j
public class SchemaManager {

    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final String VERSION_KEY = "schema_version";

    private final Connection connection;

    public SchemaManager(Connection connection) {
        this.connection = connection;
    }

    /**
     * Initializes or migrates the database schema to the current version.
     */
    public void initializeSchema() throws SQLException {
        int currentVersion = getSchemaVersion();
        log.info("Current schema version: {}, target version: {}", currentVersion, CURRENT_SCHEMA_VERSION);

        if (currentVersion < 1) {
            log.info("Executing baseline schema migration (version 1)");
            executeBaselineMigration();
            setSchemaVersion(1);
            if (!connection.getAutoCommit()) {
                connection.commit();  // Commit the schema creation
            }
            log.info("Schema migration to version 1 completed successfully");
        }

        // Future migrations go here:
        // if (currentVersion < 2) { executeMigration2(); setSchemaVersion(2); }
    }

    /**
     * Gets the current schema version from the metadata table.
     * Returns 0 if the metadata table doesn't exist (fresh database).
     */
    private int getSchemaVersion() {
        try {
            // First check if we're migrating from Liquibase
            if (isLiquibaseMigration()) {
                log.info("Detected Liquibase-managed database, migrating to new schema tracking");
                return 0; // Will trigger baseline migration which is idempotent
            }

            // Check metadata table
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT value FROM metadata WHERE key = '" + VERSION_KEY + "'")) {
                if (rs.next()) {
                    return Integer.parseInt(rs.getString("value"));
                }
            }
        } catch (SQLException e) {
            // Table doesn't exist yet - this is expected for fresh databases
            log.debug("Could not read schema version (expected for new databases): {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Checks if this database was previously managed by Liquibase.
     */
    private boolean isLiquibaseMigration() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='DATABASECHANGELOG'")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Stores the schema version in the metadata table.
     */
    private void setSchemaVersion(int version) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO metadata (key, value, updated_at) VALUES (?, ?, datetime('now'))")) {
            stmt.setString(1, VERSION_KEY);
            stmt.setString(2, String.valueOf(version));
            stmt.executeUpdate();
        }
    }

    /**
     * Baseline migration - creates all tables and indexes.
     * This is idempotent (uses IF NOT EXISTS).
     */
    private void executeBaselineMigration() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Metadata table for schema versioning
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT,
                    updated_at TEXT DEFAULT (datetime('now'))
                )
                """);

            // Repositories table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS repositories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_path TEXT UNIQUE NOT NULL,
                    repository_name TEXT,
                    is_org_repo BOOLEAN DEFAULT FALSE,
                    created_at TEXT DEFAULT (datetime('now')),
                    last_analyzed_at TEXT
                )
                """);

            // Organization dependencies table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS org_dependencies (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id TEXT NOT NULL,
                    artifact_id TEXT NOT NULL,
                    version TEXT,
                    source_repository_id INTEGER,
                    is_analyzed BOOLEAN DEFAULT FALSE,
                    analyzed_repository_id INTEGER,
                    analyzed_at TEXT,
                    jakarta_ready BOOLEAN,
                    migration_status TEXT,
                    notes TEXT,
                    created_at TEXT DEFAULT (datetime('now')),
                    updated_at TEXT DEFAULT (datetime('now')),
                    UNIQUE(group_id, artifact_id, version)
                )
                """);

            // Dependencies table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dependencies (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_path TEXT NOT NULL,
                    group_id TEXT NOT NULL,
                    artifact_id TEXT NOT NULL,
                    version TEXT,
                    scope TEXT,
                    is_direct BOOLEAN DEFAULT TRUE,
                    is_org_dependency BOOLEAN DEFAULT FALSE,
                    namespace TEXT,
                    is_jakarta_compatible BOOLEAN,
                    risk_level TEXT,
                    migration_status TEXT,
                    created_at TEXT DEFAULT (datetime('now')),
                    UNIQUE(repository_path, group_id, artifact_id, version),
                    FOREIGN KEY(repository_path) REFERENCES repositories(repository_path) ON DELETE CASCADE
                )
                """);

            // Dependency edges table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dependency_edges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_path TEXT NOT NULL,
                    from_group_id TEXT NOT NULL,
                    from_artifact_id TEXT NOT NULL,
                    to_group_id TEXT NOT NULL,
                    to_artifact_id TEXT NOT NULL,
                    edge_type TEXT DEFAULT 'dependency',
                    created_at TEXT DEFAULT (datetime('now')),
                    FOREIGN KEY(repository_path) REFERENCES repositories(repository_path) ON DELETE CASCADE
                )
                """);

            // Analysis reports table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS analysis_reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_path TEXT NOT NULL,
                    analysis_time TEXT DEFAULT (datetime('now')),
                    total_dependencies INTEGER,
                    direct_dependencies INTEGER,
                    transitive_dependencies INTEGER,
                    org_dependencies INTEGER,
                    jakarta_ready_count INTEGER,
                    needs_migration_count INTEGER,
                    blocked_count INTEGER,
                    readiness_score REAL,
                    risk_level TEXT,
                    raw_report TEXT,
                    UNIQUE(repository_path, analysis_time),
                    FOREIGN KEY(repository_path) REFERENCES repositories(repository_path) ON DELETE CASCADE
                )
                """);

            // Blockers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS blockers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_path TEXT NOT NULL,
                    analysis_report_id INTEGER,
                    blocker_type TEXT NOT NULL,
                    description TEXT,
                    affected_artifact TEXT,
                    confidence REAL,
                    created_at TEXT DEFAULT (datetime('now')),
                    FOREIGN KEY(repository_path) REFERENCES repositories(repository_path) ON DELETE CASCADE
                )
                """);

            // Recommendations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recommendations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_path TEXT NOT NULL,
                    analysis_report_id INTEGER,
                    category TEXT,
                    priority TEXT,
                    description TEXT,
                    action TEXT,
                    estimated_effort TEXT,
                    created_at TEXT DEFAULT (datetime('now')),
                    FOREIGN KEY(repository_path) REFERENCES repositories(repository_path) ON DELETE CASCADE
                )
                """);

            // Plugin state table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS plugin_state (
                    repository_path TEXT NOT NULL,
                    state_key TEXT NOT NULL,
                    state_json TEXT,
                    updated_at TEXT DEFAULT (datetime('now')),
                    PRIMARY KEY (repository_path, state_key)
                )
                """);

            // Recipe executions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recipe_executions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_path TEXT NOT NULL,
                    recipe_name TEXT NOT NULL,
                    executed_at TEXT DEFAULT (datetime('now')),
                    success BOOLEAN,
                    message TEXT,
                    affected_files TEXT,
                    FOREIGN KEY(repository_path) REFERENCES repositories(repository_path) ON DELETE CASCADE
                )
                """);

            // Recipes table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recipes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    description TEXT,
                    category TEXT NOT NULL,
                    recipe_type TEXT NOT NULL,
                    openrewrite_recipe_name TEXT,
                    pattern TEXT,
                    safety TEXT,
                    replacement TEXT,
                    file_pattern TEXT,
                    reversible BOOLEAN DEFAULT TRUE,
                    created_at TEXT DEFAULT (datetime('now'))
                )
                """);

            // Upgrade recommendations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS upgrade_recommendations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    current_group_id TEXT NOT NULL,
                    current_artifact_id TEXT NOT NULL,
                    recommended_group_id TEXT NOT NULL,
                    recommended_artifact_id TEXT NOT NULL,
                    recommended_version TEXT,
                    associated_recipe_name TEXT,
                    created_at TEXT DEFAULT (datetime('now')),
                    UNIQUE(current_group_id, current_artifact_id)
                )
                """);

            // Create indexes for efficient querying
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_deps_repo ON dependencies(repository_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_deps_group ON dependencies(group_id, artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_org_deps ON org_dependencies(group_id, artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_from ON dependency_edges(repository_path, from_group_id, from_artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_to ON dependency_edges(repository_path, to_group_id, to_artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_blockers_repo ON blockers(repository_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_recommendations_repo ON recommendations(repository_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_recipe_exec_repo ON recipe_executions(repository_path)");

            log.info("Baseline schema created successfully");
        }
    }
}
