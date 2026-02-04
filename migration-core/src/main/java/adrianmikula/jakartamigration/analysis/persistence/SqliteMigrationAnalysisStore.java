package adrianmikula.jakartamigration.analysis.persistence;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.coderefactoring.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.sqlite.SQLiteConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQLite-based persistence service for Jakarta migration analysis data.
 * Provides efficient storage and retrieval for:
 * - Dependency analysis reports
 * - Dependency graphs (nodes and edges)
 * - Migration plans
 * - Blockers and recommendations
 *
 * Database is stored in the project's .jakarta-migration directory.
 */
@Slf4j
public class SqliteMigrationAnalysisStore implements AutoCloseable {

    private static final String DB_FILE = "jakarta-migration.db";
    private static final int DB_VERSION = 1;

    private final Path dbPath;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private final ObjectMapperService objectMapper;

    public SqliteMigrationAnalysisStore(Path projectPath) {
        Path dbDir = projectPath.resolve(".jakarta-migration");
        try {
            Files.createDirectories(dbDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create analysis directory: " + dbDir, e);
        }
        this.dbPath = dbDir.resolve(DB_FILE);
        this.objectMapper = new ObjectMapperService();
        initializeDatabase();
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
            updateSchema(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Metadata table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT,
                    updated_at TEXT DEFAULT (datetime('now'))
                )
            """);

            // Projects table - supports multiple projects
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_path TEXT UNIQUE NOT NULL,
                    name TEXT,
                    created_at TEXT DEFAULT (datetime('now')),
                    last_analyzed_at TEXT
                )
            """);

            // Dependencies table - stores all discovered dependencies
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dependencies (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_path TEXT NOT NULL,
                    group_id TEXT NOT NULL,
                    artifact_id TEXT NOT NULL,
                    version TEXT,
                    scope TEXT,
                    is_direct BOOLEAN DEFAULT TRUE,
                    namespace TEXT,
                    is_jakarta_compatible BOOLEAN,
                    risk_level TEXT,
                    migration_status TEXT,
                    created_at TEXT DEFAULT (datetime('now')),
                    UNIQUE(project_path, group_id, artifact_id, version)
                )
            """);

            // Dependency graph edges
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dependency_edges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_path TEXT NOT NULL,
                    from_group_id TEXT NOT NULL,
                    from_artifact_id TEXT NOT NULL,
                    to_group_id TEXT NOT NULL,
                    to_artifact_id TEXT NOT NULL,
                    edge_type TEXT DEFAULT 'dependency',
                    created_at TEXT DEFAULT (datetime('now'))
                )
            """);

            // Analysis reports table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS analysis_reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_path TEXT NOT NULL,
                    analysis_time TEXT DEFAULT (datetime('now')),
                    total_dependencies INTEGER,
                    direct_dependencies INTEGER,
                    transitive_dependencies INTEGER,
                    jakarta_ready_count INTEGER,
                    needs_migration_count INTEGER,
                    blocked_count INTEGER,
                    readiness_score REAL,
                    risk_level TEXT,
                    raw_report TEXT,
                    UNIQUE(project_path, analysis_time)
                )
            """);

            // Blockers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS blockers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_path TEXT NOT NULL,
                    analysis_report_id INTEGER,
                    blocker_type TEXT NOT NULL,
                    description TEXT,
                    affected_artifact TEXT,
                    confidence REAL,
                    created_at TEXT DEFAULT (datetime('now'))
                )
            """);

            // Recommendations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recommendations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_path TEXT NOT NULL,
                    analysis_report_id INTEGER,
                    category TEXT,
                    priority TEXT,
                    description TEXT,
                    action TEXT,
                    estimated_effort TEXT,
                    created_at TEXT DEFAULT (datetime('now'))
                )
            """);

            // Migration plans table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS migration_plans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_path TEXT NOT NULL,
                    created_at TEXT DEFAULT (datetime('now')),
                    total_phases INTEGER,
                    total_files INTEGER,
                    estimated_duration TEXT,
                    overall_risk_score REAL,
                    raw_plan TEXT,
                    UNIQUE(project_path, created_at)
                )
            """);

            // Migration phases table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS migration_phases (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plan_id INTEGER NOT NULL,
                    phase_number INTEGER NOT NULL,
                    description TEXT,
                    file_count INTEGER,
                    estimated_duration TEXT,
                    status TEXT DEFAULT 'pending',
                    raw_phase TEXT
                )
            """);

            // Indexes for efficient querying
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_deps_project ON dependencies(project_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_from ON dependency_edges(project_path, from_group_id, from_artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_to ON dependency_edges(project_path, to_group_id, to_artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_blockers_project ON blockers(project_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_recommendations_project ON recommendations(project_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_phases_plan ON migration_phases(plan_id)");

            conn.commit();
            log.info("Database tables created/verified at {}", dbPath);
        }
    }

    private void updateSchema(Connection conn) throws SQLException {
        // Check and apply schema updates for future versions
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            int version = rs.getInt(1);
            if (version < DB_VERSION) {
                stmt.execute("PRAGMA user_version = " + DB_VERSION);
                log.info("Database schema updated to version {}", DB_VERSION);
            }
        }
    }

    // ==================== Analysis Report Operations ====================

    /**
     * Saves a complete dependency analysis report.
     */
    public void saveAnalysisReport(Path projectPath, DependencyAnalysisReport report) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // Upsert project
            upsertProject(conn, projectPath);

            // Save raw report as JSON for full state recovery
            String rawReportJson = objectMapper.toJson(report);

            // Get namespace info from map
            NamespaceCompatibilityMap namespaceMap = report.namespaceMap();

            // Insert analysis report
            long reportId;
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO analysis_reports (
                    project_path, total_dependencies, direct_dependencies,
                    transitive_dependencies, jakarta_ready_count, needs_migration_count,
                    blocked_count, readiness_score, risk_level, raw_report
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {

                Set<Artifact> allArtifacts = report.dependencyGraph().getNodes();
                stmt.setString(1, projectPath.toString());
                stmt.setInt(2, allArtifacts.size());
                stmt.setInt(3, allArtifacts.size()); // Simplified - count all as direct
                stmt.setInt(4, allArtifacts.size());
                stmt.setInt(5, countJakartaReady(allArtifacts, namespaceMap));
                stmt.setInt(6, countNeedsMigration(allArtifacts, namespaceMap));
                stmt.setInt(7, report.blockers().size());
                stmt.setDouble(8, report.readinessScore().score());
                stmt.setString(9, riskLevelFromScore(report.riskAssessment().riskScore()));
                stmt.setString(10, rawReportJson);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    reportId = rs.next() ? rs.getLong(1) : -1;
                }
            }

            // Save dependencies
            saveDependencies(conn, projectPath, report, namespaceMap);

            // Save dependency edges
            saveDependencyEdges(conn, projectPath, report);

            // Save blockers
            saveBlockers(conn, projectPath, reportId, report.blockers());

            // Save recommendations
            saveRecommendations(conn, projectPath, reportId, report.recommendations());

            // Update last analyzed timestamp
            updateMetadata(conn, "last_analyzed_" + projectPath.toString(), Instant.now().toString());

            conn.commit();
            log.info("Saved analysis report for project: {}", projectPath);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save analysis report", e);
        }
    }

    /**
     * Loads the latest analysis report for a project.
     */
    public Optional<DependencyAnalysisReport> loadLatestAnalysisReport(Path projectPath) {
        try (Connection conn = getConnection()) {
            // Try to load from raw JSON first
            try (PreparedStatement stmt = conn.prepareStatement("""
                SELECT raw_report FROM analysis_reports
                WHERE project_path = ?
                ORDER BY analysis_time DESC LIMIT 1
                """)) {
                stmt.setString(1, projectPath.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String rawJson = rs.getString("raw_report");
                        return Optional.of(objectMapper.fromJson(rawJson, DependencyAnalysisReport.class));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load analysis report", e);
        }
    }

    /**
     * Gets analysis summary without full report.
     */
    public Optional<AnalysisSummary> getAnalysisSummary(Path projectPath) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT * FROM analysis_reports
                WHERE project_path = ?
                ORDER BY analysis_time DESC LIMIT 1
                """)) {
            stmt.setString(1, projectPath.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new AnalysisSummary(
                        rs.getInt("total_dependencies"),
                        rs.getInt("direct_dependencies"),
                        rs.getInt("jakarta_ready_count"),
                        rs.getInt("needs_migration_count"),
                        rs.getInt("blocked_count"),
                        rs.getDouble("readiness_score"),
                        rs.getString("risk_level"),
                        rs.getTimestamp("analysis_time").toInstant()
                    ));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get analysis summary", e);
        }
    }

    // ==================== Dependency Operations ====================

    /**
     * Gets all dependencies for a project.
     */
    public List<DependencyInfo> getDependencies(Path projectPath) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT * FROM dependencies WHERE project_path = ? ORDER BY group_id, artifact_id
                """)) {
            stmt.setString(1, projectPath.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                List<DependencyInfo> deps = new ArrayList<>();
                while (rs.next()) {
                    deps.add(new DependencyInfo(
                        rs.getString("group_id"),
                        rs.getString("artifact_id"),
                        rs.getString("version"),
                        rs.getString("scope"),
                        rs.getString("namespace"),
                        rs.getBoolean("is_jakarta_compatible"),
                        rs.getString("risk_level"),
                        rs.getString("migration_status")
                    ));
                }
                return deps;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get dependencies", e);
        }
    }

    /**
     * Gets dependencies that need migration (non-Jakarta compatible).
     */
    public List<DependencyInfo> getDependenciesNeedingMigration(Path projectPath) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT * FROM dependencies
                WHERE project_path = ? AND (is_jakarta_compatible = FALSE OR is_jakarta_compatible IS NULL)
                ORDER BY risk_level, group_id, artifact_id
                """)) {
            stmt.setString(1, projectPath.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                List<DependencyInfo> deps = new ArrayList<>();
                while (rs.next()) {
                    deps.add(new DependencyInfo(
                        rs.getString("group_id"),
                        rs.getString("artifact_id"),
                        rs.getString("version"),
                        rs.getString("scope"),
                        rs.getString("namespace"),
                        rs.getBoolean("is_jakarta_compatible"),
                        rs.getString("risk_level"),
                        rs.getString("migration_status")
                    ));
                }
                return deps;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get dependencies needing migration", e);
        }
    }

    // ==================== Migration Plan Operations ====================

    /**
     * Saves a migration plan.
     */
    public void saveMigrationPlan(Path projectPath, MigrationPlan plan) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // Insert plan
            long planId;
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO migration_plans (
                    project_path, total_phases, total_files,
                    estimated_duration, overall_risk_score, raw_plan
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, projectPath.toString());
                stmt.setInt(2, plan.phases().size());
                stmt.setInt(3, plan.totalFileCount());
                stmt.setString(4, plan.estimatedDuration() != null ? plan.estimatedDuration().toString() : null);
                stmt.setDouble(5, plan.overallRisk().riskScore());
                stmt.setString(6, objectMapper.toJson(plan));
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    planId = rs.next() ? rs.getLong(1) : -1;
                }
            }

            // Insert phases
            for (RefactoringPhase phase : plan.phases()) {
                try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT INTO migration_phases (
                        plan_id, phase_number, description, file_count,
                        estimated_duration, raw_phase
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                    stmt.setLong(1, planId);
                    stmt.setInt(2, phase.phaseNumber());
                    stmt.setString(3, phase.description());
                    stmt.setInt(4, phase.files().size());
                    stmt.setString(5, phase.estimatedDuration() != null ? phase.estimatedDuration().toString() : null);
                    stmt.setString(6, objectMapper.toJson(phase));
                    stmt.executeUpdate();
                }
            }

            conn.commit();
            log.info("Saved migration plan for project: {} with {} phases", projectPath, plan.phases().size());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save migration plan", e);
        }
    }

    /**
     * Loads the latest migration plan for a project.
     */
    public Optional<MigrationPlan> loadLatestMigrationPlan(Path projectPath) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT raw_plan FROM migration_plans
                WHERE project_path = ?
                ORDER BY created_at DESC LIMIT 1
                """)) {
            stmt.setString(1, projectPath.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String rawJson = rs.getString("raw_plan");
                    return Optional.of(objectMapper.fromJson(rawJson, MigrationPlan.class));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load migration plan", e);
        }
    }

    // ==================== Blocker Operations ====================

    /**
     * Gets all blockers for a project.
     */
    public List<StoredBlocker> getBlockers(Path projectPath) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT * FROM blockers WHERE project_path = ? ORDER BY confidence DESC
                """)) {
            stmt.setString(1, projectPath.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                List<StoredBlocker> blockers = new ArrayList<>();
                while (rs.next()) {
                    blockers.add(new StoredBlocker(
                        rs.getString("blocker_type"),
                        rs.getString("description"),
                        rs.getString("affected_artifact"),
                        rs.getDouble("confidence")
                    ));
                }
                return blockers;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get blockers", e);
        }
    }

    // ==================== Recommendation Operations ====================

    /**
     * Gets all recommendations for a project.
     */
    public List<StoredRecommendation> getRecommendations(Path projectPath) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT * FROM recommendations WHERE project_path = ? ORDER BY priority, created_at DESC
                """)) {
            stmt.setString(1, projectPath.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                List<StoredRecommendation> recs = new ArrayList<>();
                while (rs.next()) {
                    recs.add(new StoredRecommendation(
                        rs.getString("category"),
                        rs.getString("priority"),
                        rs.getString("description"),
                        rs.getString("action"),
                        rs.getString("estimated_effort")
                    ));
                }
                return recs;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get recommendations", e);
        }
    }

    // ==================== Utility Operations ====================

    /**
     * Checks if analysis data exists for a project.
     */
    public boolean hasAnalysisData(Path projectPath) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT 1 FROM analysis_reports WHERE project_path = ? LIMIT 1
                """)) {
            stmt.setString(1, projectPath.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check analysis data", e);
        }
    }

    /**
     * Gets the last analysis timestamp.
     */
    public Optional<Instant> getLastAnalysisTime(Path projectPath) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT MAX(analysis_time) as last_time FROM analysis_reports WHERE project_path = ?
                """)) {
            stmt.setString(1, projectPath.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getTimestamp("last_time") != null) {
                    return Optional.of(rs.getTimestamp("last_time").toInstant());
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get last analysis time", e);
        }
    }

    /**
     * Clears all analysis data for a project.
     */
    public boolean clearAnalysisData(Path projectPath) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            String path = projectPath.toString();

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM migration_phases WHERE plan_id IN " +
                    "(SELECT id FROM migration_plans WHERE project_path = '" + path + "')");
                stmt.execute("DELETE FROM migration_plans WHERE project_path = '" + path + "'");
                stmt.execute("DELETE FROM blockers WHERE project_path = '" + path + "'");
                stmt.execute("DELETE FROM recommendations WHERE project_path = '" + path + "'");
                stmt.execute("DELETE FROM dependency_edges WHERE project_path = '" + path + "'");
                stmt.execute("DELETE FROM dependencies WHERE project_path = '" + path + "'");
                stmt.execute("DELETE FROM analysis_reports WHERE project_path = '" + path + "'");
                stmt.execute("DELETE FROM projects WHERE project_path = '" + path + "'");
            }

            conn.commit();
            log.info("Cleared all analysis data for project: {}", projectPath);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear analysis data", e);
        }
    }

    /**
     * Gets database file path for debugging.
     */
    public Path getDbPath() {
        return dbPath;
    }

    // ==================== Private Helper Methods ====================

    private void upsertProject(Connection conn, Path projectPath) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("""
            INSERT INTO projects (project_path, name, last_analyzed_at)
            VALUES (?, ?, datetime('now'))
            ON CONFLICT(project_path) DO UPDATE SET last_analyzed_at = datetime('now')
            """)) {
            stmt.setString(1, projectPath.toString());
            stmt.setString(2, projectPath.getFileName().toString());
            stmt.executeUpdate();
        }
    }

    private void saveDependencies(Connection conn, Path projectPath, DependencyAnalysisReport report, NamespaceCompatibilityMap namespaceMap) throws SQLException {
        String path = projectPath.toString();

        // Clear existing dependencies
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM dependencies WHERE project_path = '" + path + "'");
        }

        // Insert all artifacts from the graph
        for (Artifact artifact : report.dependencyGraph().getNodes()) {
            Namespace namespace = namespaceMap.get(artifact);
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO dependencies (
                    project_path, group_id, artifact_id, version, scope,
                    is_direct, namespace, is_jakarta_compatible, risk_level, migration_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
                stmt.setString(1, path);
                stmt.setString(2, artifact.groupId());
                stmt.setString(3, artifact.artifactId());
                stmt.setString(4, artifact.version());
                stmt.setString(5, artifact.scope());
                stmt.setBoolean(6, !artifact.transitive());
                stmt.setString(7, namespace.name());
                stmt.setBoolean(8, namespace == Namespace.JAKARTA);
                stmt.setString(9, determineRiskLevel(namespace));
                stmt.setString(10, determineMigrationStatus(namespace));
                stmt.executeUpdate();
            }
        }
    }

    private void saveDependencyEdges(Connection conn, Path projectPath, DependencyAnalysisReport report) throws SQLException {
        String path = projectPath.toString();

        // Clear existing edges
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM dependency_edges WHERE project_path = '" + path + "'");
        }

        // Insert edges from the graph
        for (Dependency dep : report.dependencyGraph().getEdges()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO dependency_edges (project_path, from_group_id, from_artifact_id, to_group_id, to_artifact_id)
                VALUES (?, ?, ?, ?, ?)
                """)) {
                stmt.setString(1, path);
                stmt.setString(2, dep.from().groupId());
                stmt.setString(3, dep.from().artifactId());
                stmt.setString(4, dep.to().groupId());
                stmt.setString(5, dep.to().artifactId());
                stmt.executeUpdate();
            }
        }
    }

    private void saveBlockers(Connection conn, Path projectPath, long reportId, List<Blocker> blockers) throws SQLException {
        String path = projectPath.toString();

        // Clear existing blockers
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM blockers WHERE project_path = '" + path + "'");
        }

        for (Blocker blocker : blockers) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO blockers (project_path, analysis_report_id, blocker_type, description, affected_artifact, confidence)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
                stmt.setString(1, path);
                stmt.setLong(2, reportId);
                stmt.setString(3, blocker.type().name());
                stmt.setString(4, blocker.reason());
                stmt.setString(5, blocker.artifact().toCoordinate());
                stmt.setDouble(6, blocker.confidence());
                stmt.executeUpdate();
            }
        }
    }

    private void saveRecommendations(Connection conn, Path projectPath, long reportId, List<VersionRecommendation> recommendations) throws SQLException {
        String path = projectPath.toString();

        // Clear existing recommendations
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM recommendations WHERE project_path = '" + path + "'");
        }

        for (VersionRecommendation rec : recommendations) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO recommendations (project_path, analysis_report_id, category, priority, description, action, estimated_effort)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
                stmt.setString(1, path);
                stmt.setLong(2, reportId);
                stmt.setString(3, rec.currentArtifact().groupId() + ":" + rec.currentArtifact().artifactId());
                stmt.setString(4, rec.compatibilityScore() > 0.8 ? "high" : "medium");
                stmt.setString(5, "Upgrade " + rec.currentArtifact().groupId() + ":" + rec.currentArtifact().artifactId() + " from " + rec.currentArtifact().version() + " to " + (rec.recommendedArtifact() != null ? rec.recommendedArtifact().version() : "latest"));
                stmt.setString(6, "Update dependency version in pom.xml/build.gradle");
                stmt.setString(7, rec.compatibilityScore() > 0.8 ? "low" : "medium");
                stmt.executeUpdate();
            }
        }
    }

    private void updateMetadata(Connection conn, String key, String value) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("""
            INSERT OR REPLACE INTO metadata (key, value, updated_at) VALUES (?, ?, datetime('now'))
            """)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.executeUpdate();
        }
    }

    private int countJakartaReady(Set<Artifact> artifacts, NamespaceCompatibilityMap namespaceMap) {
        return (int) artifacts.stream()
            .filter(a -> namespaceMap.get(a) == Namespace.JAKARTA)
            .count();
    }

    private int countNeedsMigration(Set<Artifact> artifacts, NamespaceCompatibilityMap namespaceMap) {
        return (int) artifacts.stream()
            .filter(a -> {
                Namespace ns = namespaceMap.get(a);
                return ns == Namespace.JAVAX || ns == Namespace.MIXED;
            })
            .count();
    }

    private String determineRiskLevel(Namespace namespace) {
        return switch (namespace) {
            case JAKARTA -> "LOW";
            case JAVAX -> "HIGH";
            case MIXED -> "HIGH";
            case UNKNOWN -> "MEDIUM";
        };
    }

    private String determineMigrationStatus(Namespace namespace) {
        return switch (namespace) {
            case JAKARTA -> "READY";
            case JAVAX, MIXED -> "NEEDS_MIGRATION";
            case UNKNOWN -> "UNKNOWN";
        };
    }

    private String riskLevelFromScore(double score) {
        if (score < 0.3) return "LOW";
        if (score < 0.7) return "MEDIUM";
        return "HIGH";
    }

    @Override
    public void close() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("Error closing database connection", e);
            }
            connectionHolder.remove();
        }
    }

    // ==================== Inner Classes ====================

    /**
     * Simple dependency info record for queries.
     */
    public record DependencyInfo(
        String groupId,
        String artifactId,
        String version,
        String scope,
        String namespace,
        boolean isJakartaCompatible,
        String riskLevel,
        String migrationStatus
    ) {}

    /**
     * Analysis summary record.
     */
    public record AnalysisSummary(
        int totalDependencies,
        int directDependencies,
        int jakartaReady,
        int needsMigration,
        int blocked,
        double readinessScore,
        String riskLevel,
        Instant analysisTime
    ) {}

    /**
     * Stored blocker record.
     */
    public record StoredBlocker(
        String type,
        String description,
        String affectedArtifact,
        double confidence
    ) {}

    /**
     * Stored recommendation record.
     */
    public record StoredRecommendation(
        String category,
        String priority,
        String description,
        String action,
        String estimatedEffort
    ) {}
}
