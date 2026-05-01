package adrianmikula.jakartamigration.storage;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.analysis.persistence.ObjectMapperService;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/**
 * Centralized migration store for Jakarta migration data.
 * Uses a single database in the user's home directory with workspace isolation.
 */
@Slf4j
public class CentralizedMigrationStore implements AutoCloseable {

    private static final int DB_VERSION = 1;
    private static final String DEFAULT_DB_PATH = System.getProperty("user.home") + "/.jakartamigration/jakarta-migration.db";

    private final String dbPath;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private final ObjectMapperService objectMapper;

    public CentralizedMigrationStore() {
        this(DEFAULT_DB_PATH);
    }

    /**
     * Constructor for testing with custom database path.
     * @param customDbPath Custom path for the database file
     */
    public CentralizedMigrationStore(String customDbPath) {
        this.dbPath = customDbPath;
        this.objectMapper = new ObjectMapperService();
        ensureParentDirectoryExists();
        initializeDatabase();
    }

    private void ensureParentDirectoryExists() {
        Path path = Path.of(dbPath);
        Path parent = path.getParent();
        if (parent != null) {
            try {
                java.nio.file.Files.createDirectories(parent);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to create database directory: " + parent, e);
            }
        }
    }

    private Connection getConnection() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            conn.setAutoCommit(false);
            connectionHolder.set(conn);
        }
        return conn;
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            createTables(conn);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize centralized database", e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        // Projects table
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS projects (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        workspace_id TEXT NOT NULL,
                        project_path TEXT NOT NULL,
                        name TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        last_analyzed_at TIMESTAMP,
                        UNIQUE(workspace_id, project_path)
                    )
                    """);
        }

        // Analysis reports table
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS analysis_reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        workspace_id TEXT NOT NULL,
                        project_path TEXT NOT NULL,
                        total_dependencies INTEGER,
                        direct_dependencies INTEGER,
                        transitive_dependencies INTEGER,
                        jakarta_ready_count INTEGER,
                        needs_migration_count INTEGER,
                        blocked_count INTEGER,
                        readiness_score REAL,
                        risk_level TEXT,
                        raw_report TEXT,
                        analysis_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (workspace_id, project_path) REFERENCES projects(workspace_id, project_path)
                    )
                    """);
        }

        // Dependencies table
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS dependencies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        workspace_id TEXT NOT NULL,
                        project_path TEXT NOT NULL,
                        group_id TEXT,
                        artifact_id TEXT,
                        version TEXT,
                        scope TEXT,
                        is_direct BOOLEAN,
                        is_jakarta_compatible BOOLEAN,
                        risk_level TEXT,
                        migration_status TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }

        // Dependency edges table
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS dependency_edges (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        workspace_id TEXT NOT NULL,
                        project_path TEXT NOT NULL,
                        from_group_id TEXT,
                        from_artifact_id TEXT,
                        to_group_id TEXT,
                        to_artifact_id TEXT,
                        edge_type TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }

        // Blockers table
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS blockers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        workspace_id TEXT NOT NULL,
                        project_path TEXT NOT NULL,
                        blocker_type TEXT,
                        description TEXT,
                        affected_artifact TEXT,
                        confidence REAL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }

        // Recommendations table
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS recommendations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        workspace_id TEXT NOT NULL,
                        project_path TEXT NOT NULL,
                        category TEXT,
                        priority TEXT,
                        description TEXT,
                        action TEXT,
                        estimated_effort TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }
    }

    /**
     * Registers a project in the centralized database.
     */
    public void registerProject(String workspaceId, Path projectPath) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT OR REPLACE INTO projects (workspace_id, project_path, name, created_at)
                    VALUES (?, ?, ?, datetime('now'))
                    """)) {
                stmt.setString(1, workspaceId);
                stmt.setString(2, projectPath.toString());
                stmt.setString(3, projectPath.getFileName().toString());
                stmt.executeUpdate();
            }
            conn.commit();
            log.info("Registered project: {} in workspace: {}", projectPath, workspaceId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register project", e);
        }
    }

    /**
     * Saves a complete dependency analysis report.
     */
    public void saveAnalysisReport(String workspaceId, Path projectPath, DependencyAnalysisReport report) {
        try (Connection conn = getConnection()) {
            // Register project if not exists
            registerProject(workspaceId, projectPath);

            // Save raw report as JSON
            String rawReportJson = objectMapper.toJson(report);
            NamespaceCompatibilityMap namespaceMap = report.namespaceMap();

            // Insert analysis report
            try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT INTO analysis_reports (
                        workspace_id, project_path, total_dependencies, direct_dependencies,
                        transitive_dependencies, jakarta_ready_count, needs_migration_count,
                        blocked_count, readiness_score, risk_level, raw_report
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                Set<Artifact> allArtifacts = report.dependencyGraph().getNodes();
                stmt.setString(1, workspaceId);
                stmt.setString(2, projectPath.toString());
                stmt.setInt(3, allArtifacts.size());
                stmt.setInt(4, (int) allArtifacts.stream().filter(a -> !a.transitive()).count());
                stmt.setInt(5, (int) allArtifacts.stream().filter(Artifact::transitive).count());
                stmt.setInt(6, countJakartaReady(allArtifacts, namespaceMap));
                stmt.setInt(7, countNeedsMigration(allArtifacts, namespaceMap));
                stmt.setInt(8, report.blockers().size());
                stmt.setDouble(9, report.readinessScore().score());
                stmt.setString(10, riskLevelFromScore(report.riskAssessment().riskScore()));
                stmt.setString(11, rawReportJson);
                stmt.executeUpdate();
            }

            // Save dependencies
            saveDependencies(workspaceId, projectPath, report);

            // Save dependency edges
            saveDependencyEdges(workspaceId, projectPath, report);

            // Save blockers
            saveBlockers(workspaceId, projectPath, report);

            // Save recommendations
            saveRecommendations(workspaceId, projectPath, report);

            // Update project's last analyzed timestamp
            try (PreparedStatement updateStmt = conn.prepareStatement("""
                    UPDATE projects 
                    SET last_analyzed_at = datetime('now')
                    WHERE workspace_id = ? AND project_path = ?
                    """)) {
                updateStmt.setString(1, workspaceId);
                updateStmt.setString(2, projectPath.toString());
                updateStmt.executeUpdate();
            }

            conn.commit();
            log.info("Saved analysis report for project: {} in workspace: {}", projectPath, workspaceId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save analysis report", e);
        }
    }

    /**
     * Gets the latest analysis report for a project.
     */
    public Optional<DependencyAnalysisReport> getLatestAnalysisReport(String workspaceId, Path projectPath) {
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement("""
                        SELECT raw_report FROM analysis_reports
                        WHERE workspace_id = ? AND project_path = ?
                        ORDER BY analysis_time DESC LIMIT 1
                        """)) {
                stmt.setString(1, workspaceId);
                stmt.setString(2, projectPath.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String rawJson = rs.getString("raw_report");
                        return Optional.of(objectMapper.fromJson(rawJson, DependencyAnalysisReport.class));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get analysis report", e);
            }
    }

    // Helper methods for saving related data

    private void saveDependencies(String workspaceId, Path projectPath, DependencyAnalysisReport report) {
        try (Connection conn = getConnection()) {
            Set<Artifact> allArtifacts = report.dependencyGraph().getNodes();
            NamespaceCompatibilityMap namespaceMap = report.namespaceMap();

            String insertSql = """
                    INSERT OR REPLACE INTO dependencies (
                        workspace_id, project_path, group_id, artifact_id, version, scope,
                        is_direct, is_jakarta_compatible, risk_level, migration_status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (Artifact artifact : allArtifacts) {
                    pstmt.setString(1, workspaceId);
                    pstmt.setString(2, projectPath.toString());
                    pstmt.setString(3, artifact.groupId());
                    pstmt.setString(4, artifact.artifactId());
                    pstmt.setString(5, artifact.version());
                    pstmt.setString(6, artifact.scope());
                    pstmt.setBoolean(7, !artifact.transitive());
                    pstmt.setBoolean(8, isJakartaReady(artifact, namespaceMap));
                    pstmt.setString(9, "INFO");
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                log.debug("Saved {} dependencies for project: {}", allArtifacts.size(), projectPath);
            }
        } catch (SQLException e) {
            log.error("Failed to save dependencies", e);
        }
    }

    private void saveDependencyEdges(String workspaceId, Path projectPath, DependencyAnalysisReport report) {
        try (Connection conn = getConnection()) {
            String insertSql = """
                    INSERT OR REPLACE INTO dependency_edges (
                        workspace_id, project_path, from_group_id, from_artifact_id,
                        to_group_id, to_artifact_id, edge_type
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (Dependency edge : report.dependencyGraph().getEdges()) {
                    pstmt.setString(1, workspaceId);
                    pstmt.setString(2, projectPath.toString());
                    pstmt.setString(3, edge.from().groupId());
                    pstmt.setString(4, edge.from().artifactId());
                    pstmt.setString(5, edge.to().groupId());
                    pstmt.setString(6, edge.to().artifactId());
                    pstmt.setString(7, edge.scope());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                log.debug("Saved {} dependency edges for project: {}", report.dependencyGraph().getEdges().size(), projectPath);
            }
        } catch (SQLException e) {
            log.error("Failed to save dependency edges", e);
        }
    }

    private void saveBlockers(String workspaceId, Path projectPath, DependencyAnalysisReport report) {
        try (Connection conn = getConnection()) {
            String insertSql = """
                    INSERT OR REPLACE INTO blockers (
                        workspace_id, project_path, blocker_type, description,
                        affected_artifact, confidence
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (Blocker blocker : report.blockers()) {
                    pstmt.setString(1, workspaceId);
                    pstmt.setString(2, projectPath.toString());
                    pstmt.setString(3, blocker.type().name());
                    pstmt.setString(4, blocker.reason());
                    pstmt.setString(5, blocker.artifact().toIdentifier());
                    pstmt.setDouble(6, blocker.confidence());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                log.debug("Saved {} blockers for project: {}", report.blockers().size(), projectPath);
            }
        } catch (SQLException e) {
            log.error("Failed to save blockers", e);
        }
    }

    private void saveRecommendations(String workspaceId, Path projectPath, DependencyAnalysisReport report) {
        try (Connection conn = getConnection()) {
            String insertSql = """
                    INSERT OR REPLACE INTO recommendations (
                        workspace_id, project_path, category, priority,
                        description, action, estimated_effort
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (VersionRecommendation recommendation : report.recommendations()) {
                    pstmt.setString(1, workspaceId);
                    pstmt.setString(2, projectPath.toString());
                    pstmt.setString(3, "VERSION");
                    pstmt.setString(4, "HIGH");
                    pstmt.setString(5, recommendation.recommendedArtifact().version());
                    pstmt.setString(6, "Upgrade");
                    pstmt.setString(7, "Low effort");
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                log.debug("Saved {} recommendations for project: {}", report.recommendations().size(), projectPath);
            }
        } catch (SQLException e) {
            log.error("Failed to save recommendations", e);
        }
    }

    // Helper methods for artifact analysis

    private String riskLevelFromScore(double riskScore) {
        if (riskScore >= 0.8) return "HIGH";
        if (riskScore >= 0.6) return "MEDIUM";
        if (riskScore >= 0.3) return "LOW";
        return "INFO";
    }

    private int countJakartaReady(Set<Artifact> artifacts, NamespaceCompatibilityMap namespaceMap) {
        if (namespaceMap == null) return 0;
        return (int) artifacts.stream()
                .filter(artifact -> isJakartaReady(artifact, namespaceMap))
                .count();
    }

    private int countNeedsMigration(Set<Artifact> artifacts, NamespaceCompatibilityMap namespaceMap) {
        if (namespaceMap == null) return artifacts.size();
        return (int) artifacts.stream()
                .filter(artifact -> !isJakartaReady(artifact, namespaceMap))
                .count();
    }

    private boolean isJakartaReady(Artifact artifact, NamespaceCompatibilityMap namespaceMap) {
        if (namespaceMap == null) return false;
        Namespace namespace = namespaceMap.get(artifact);
        return namespace == Namespace.JAKARTA || namespace == Namespace.MIXED;
    }

    public Path getDatabasePath() {
        return Path.of(dbPath);
    }

    @Override
    public void close() {
        try {
            Connection conn = connectionHolder.get();
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            log.warn("Error closing centralized database connection", e);
        } finally {
            connectionHolder.remove();
        }
        log.debug("CentralizedMigrationStore closed");
    }
}
