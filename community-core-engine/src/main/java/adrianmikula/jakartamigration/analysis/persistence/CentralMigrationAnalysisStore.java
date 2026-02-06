package adrianmikula.jakartamigration.analysis.persistence;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.coderefactoring.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.sqlite.SQLiteConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralized SQLite-based persistence service for Jakarta migration analysis data.
 * Stores data in user profile folder (~/.jakarta-migration/) to enable:
 * - Cross-repository dependency tracking
 * - Organization/internal dependency classification
 * - Analysis status tracking for org dependencies
 * - Combined view of all analyzed repositories
 */
@Slf4j
public class CentralMigrationAnalysisStore implements AutoCloseable {

    private static final String DB_FILE = "central-migration-analysis.db";
    private static final int DB_VERSION = 2; // Increment for schema changes

    private final Path dbPath;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private final ObjectMapperService objectMapper;

    // User-configurable org namespace patterns
    private final Set<String> orgNamespacePatterns = new HashSet<>();

    public CentralMigrationAnalysisStore() {
        this.dbPath = getCentralDbPath();
        this.objectMapper = new ObjectMapperService();
        initializeDatabase();
    }

    /**
     * Gets the central database path in user profile folder.
     */
    public static Path getCentralDbPath() {
        String userHome = System.getProperty("user.home");
        Path basePath;

        // Try to use a standard location based on OS
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            String appData = System.getenv("APPDATA");
            basePath = appData != null ? Paths.get(appData) : Paths.get(userHome, "AppData", "Roaming");
        } else if (os.contains("mac")) {
            basePath = Paths.get(userHome, "Library", "Application Support");
        } else {
            // Linux and others
            basePath = Paths.get(userHome, ".config");
        }

        Path dbDir = basePath.resolve("JakartaMigration");
        try {
            Files.createDirectories(dbDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create analysis directory: " + dbDir, e);
        }
        return dbDir.resolve(DB_FILE);
    }

    /**
     * Sets organization namespace patterns for classifying internal dependencies.
     * Patterns are matched against groupId (supports wildcards like "com.myorg.*")
     */
    public void setOrgNamespacePatterns(Set<String> patterns) {
        this.orgNamespacePatterns.clear();
        this.orgNamespacePatterns.addAll(patterns);
    }

    /**
     * Checks if a dependency belongs to the organization.
     */
    public boolean isOrgDependency(String groupId) {
        for (String pattern : orgNamespacePatterns) {
            if (matchesPattern(groupId, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPattern(String groupId, String pattern) {
        if (pattern.equals("*") || pattern.equals(groupId)) {
            return true;
        }
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return groupId.startsWith(prefix);
        }
        return groupId.equals(pattern);
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

            // Repositories table - tracks analyzed repositories
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

            // Organization dependencies table - tracks cross-repo internal dependencies
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

            // Dependencies table - stores all discovered dependencies (from all repos)
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
                    UNIQUE(repository_path, group_id, artifact_id, version)
                )
            """);

            // Dependency graph edges
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dependency_edges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_path TEXT NOT NULL,
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
                    UNIQUE(repository_path, analysis_time)
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
                    created_at TEXT DEFAULT (datetime('now'))
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
                    created_at TEXT DEFAULT (datetime('now'))
                )
            """);

            // Indexes for efficient querying
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_deps_repo ON dependencies(repository_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_deps_group ON dependencies(group_id, artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_org_deps ON org_dependencies(group_id, artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_from ON dependency_edges(repository_path, from_group_id, from_artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_to ON dependency_edges(repository_path, to_group_id, to_artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_blockers_repo ON blockers(repository_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_recommendations_repo ON recommendations(repository_path)");

            conn.commit();
            log.info("Central database created at {}", dbPath);
        }
    }

    private void updateSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            int version = rs.getInt(1);
            if (version < DB_VERSION) {
                stmt.execute("PRAGMA user_version = " + DB_VERSION);
                log.info("Database schema updated to version {}", DB_VERSION);
            }
        }
    }

    // ==================== Repository Operations ====================

    /**
     * Registers a repository for tracking.
     */
    public void registerRepository(Path repositoryPath, boolean isOrgRepo) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO repositories (repository_path, repository_name, is_org_repo, last_analyzed_at)
                VALUES (?, ?, ?, datetime('now'))
                ON CONFLICT(repository_path) DO UPDATE SET
                    repository_name = excluded.repository_name,
                    is_org_repo = excluded.is_org_repo,
                    last_analyzed_at = datetime('now')
                """)) {
                stmt.setString(1, repositoryPath.toString());
                stmt.setString(2, repositoryPath.getFileName().toString());
                stmt.setBoolean(3, isOrgRepo);
                stmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register repository", e);
        }
    }

    /**
     * Gets all registered repositories.
     */
    public List<RepositoryInfo> getRepositories() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                 SELECT * FROM repositories ORDER BY last_analyzed_at DESC
                 """)) {
            List<RepositoryInfo> repos = new ArrayList<>();
            while (rs.next()) {
                repos.add(new RepositoryInfo(
                    rs.getString("repository_path"),
                    rs.getString("repository_name"),
                    rs.getBoolean("is_org_repo"),
                    rs.getTimestamp("last_analyzed_at") != null ? rs.getTimestamp("last_analyzed_at").toInstant() : null
                ));
            }
            return repos;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get repositories", e);
        }
    }

    // ==================== Org Dependency Operations ====================

    /**
     * Saves an organization dependency that needs to be tracked across repositories.
     */
    public void saveOrgDependency(String groupId, String artifactId, String version,
                                  boolean isAnalyzed, String sourceRepositoryPath) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO org_dependencies (
                    group_id, artifact_id, version, source_repository_id,
                    is_analyzed, analyzed_at, migration_status
                )
                VALUES (
                    (SELECT id FROM repositories WHERE repository_path = ?),
                    ?, ?, ?,
                    ?, datetime('now'), 'PENDING'
                )
                ON CONFLICT(group_id, artifact_id, version) DO UPDATE SET
                    is_analyzed = excluded.is_analyzed,
                    updated_at = datetime('now')
                """)) {
                // Note: Simplified - actual implementation would need proper subquery handling
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save org dependency", e);
        }
    }

    /**
     * Gets all organization dependencies.
     */
    public List<OrgDependencyInfo> getOrgDependencies() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                 SELECT od.*, r.repository_path as source_repo
                 FROM org_dependencies od
                 LEFT JOIN repositories r ON od.source_repository_id = r.id
                 ORDER BY od.group_id, od.artifact_id
                 """)) {
            List<OrgDependencyInfo> deps = new ArrayList<>();
            while (rs.next()) {
                deps.add(new OrgDependencyInfo(
                    rs.getString("group_id"),
                    rs.getString("artifact_id"),
                    rs.getString("version"),
                    rs.getString("source_repo"),
                    rs.getBoolean("is_analyzed"),
                    rs.getTimestamp("analyzed_at") != null ? rs.getTimestamp("analyzed_at").toInstant() : null,
                    rs.getBoolean("jakarta_ready"),
                    rs.getString("migration_status")
                ));
            }
            return deps;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get org dependencies", e);
        }
    }

    /**
     * Gets organization dependencies that haven't been analyzed yet.
     */
    public List<OrgDependencyInfo> getUnanalyzedOrgDependencies() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                 SELECT od.*, r.repository_path as source_repo
                 FROM org_dependencies od
                 LEFT JOIN repositories r ON od.source_repository_id = r.id
                 WHERE od.is_analyzed = FALSE
                 ORDER BY od.group_id, od.artifact_id
                 """)) {
            try (ResultSet rs = stmt.executeQuery()) {
                List<OrgDependencyInfo> deps = new ArrayList<>();
                while (rs.next()) {
                    deps.add(new OrgDependencyInfo(
                        rs.getString("group_id"),
                        rs.getString("artifact_id"),
                        rs.getString("version"),
                        rs.getString("source_repo"),
                        rs.getBoolean("is_analyzed"),
                        null,
                        rs.getBoolean("jakarta_ready"),
                        rs.getString("migration_status")
                    ));
                }
                return deps;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get unanalyzed org dependencies", e);
        }
    }

    /**
     * Marks an organization dependency as analyzed.
     */
    public void markOrgDependencyAnalyzed(String groupId, String artifactId, String version,
                                          boolean jakartaReady, String migrationStatus) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                 UPDATE org_dependencies SET
                     is_analyzed = TRUE,
                     analyzed_at = datetime('now'),
                     jakarta_ready = ?,
                     migration_status = ?,
                     updated_at = datetime('now')
                 WHERE group_id = ? AND artifact_id = ? AND (version = ? OR version IS NULL)
                 """)) {
            stmt.setBoolean(1, jakartaReady);
            stmt.setString(2, migrationStatus);
            stmt.setString(3, groupId);
            stmt.setString(4, artifactId);
            stmt.setString(5, version);
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark org dependency as analyzed", e);
        }
    }

    // ==================== Analysis Report Operations ====================

    /**
     * Saves a complete dependency analysis report.
     */
    public void saveAnalysisReport(Path repositoryPath, DependencyAnalysisReport report, boolean isOrgRepo) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // Register/update repository
            registerRepository(repositoryPath, isOrgRepo);

            // Save raw report as JSON
            String rawReportJson = objectMapper.toJson(report);
            NamespaceCompatibilityMap namespaceMap = report.namespaceMap();

            // Count org dependencies
            long orgDepsCount = report.dependencyGraph().getNodes().stream()
                .filter(a -> isOrgDependency(a.groupId()))
                .count();

            // Insert analysis report
            long reportId;
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO analysis_reports (
                    repository_path, total_dependencies, direct_dependencies,
                    transitive_dependencies, org_dependencies,
                    jakarta_ready_count, needs_migration_count,
                    blocked_count, readiness_score, risk_level, raw_report
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {

                Set<Artifact> allArtifacts = report.dependencyGraph().getNodes();
                stmt.setString(1, repositoryPath.toString());
                stmt.setInt(2, allArtifacts.size());
                stmt.setInt(3, (int) allArtifacts.stream().filter(a -> !a.transitive()).count());
                stmt.setInt(4, (int) allArtifacts.stream().filter(a -> a.transitive()).count());
                stmt.setInt(5, (int) orgDepsCount);
                stmt.setInt(6, countJakartaReady(allArtifacts, namespaceMap));
                stmt.setInt(7, countNeedsMigration(allArtifacts, namespaceMap));
                stmt.setInt(8, report.blockers().size());
                stmt.setDouble(9, report.readinessScore().score());
                stmt.setString(10, riskLevelFromScore(report.riskAssessment().riskScore()));
                stmt.setString(11, rawReportJson);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    reportId = rs.next() ? rs.getLong(1) : -1;
                }
            }

            // Save dependencies with org classification
            saveDependencies(conn, repositoryPath, report, namespaceMap);

            // Save dependency edges
            saveDependencyEdges(conn, repositoryPath, report);

            // Save blockers
            saveBlockers(conn, repositoryPath, reportId, report.blockers());

            // Save recommendations
            saveRecommendations(conn, repositoryPath, reportId, report.recommendations());

            // Update last analyzed timestamp
            try (PreparedStatement stmt = conn.prepareStatement("""
                UPDATE repositories SET last_analyzed_at = datetime('now')
                WHERE repository_path = ?
                """)) {
                stmt.setString(1, repositoryPath.toString());
                stmt.executeUpdate();
            }

            conn.commit();
            log.info("Saved analysis report for repository: {}", repositoryPath);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save analysis report", e);
        }
    }

    // ==================== Dependency Operations ====================

    /**
     * Gets all dependencies for a repository.
     */
    public List<DependencyInfo> getDependencies(Path repositoryPath) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                 SELECT * FROM dependencies WHERE repository_path = ? ORDER BY group_id, artifact_id
                 """)) {
            stmt.setString(1, repositoryPath.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                List<DependencyInfo> deps = new ArrayList<>();
                while (rs.next()) {
                    String namespace = rs.getBoolean("is_org_dependency") ? "INTERNAL" : "EXTERNAL";
                    deps.add(new DependencyInfo(
                        rs.getString("group_id"),
                        rs.getString("artifact_id"),
                        rs.getString("version"),
                        rs.getString("scope"),
                        namespace,
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
     * Gets all dependencies across all repositories.
     */
    public List<DependencyInfo> getAllDependencies() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                 SELECT * FROM dependencies ORDER BY repository_path, group_id, artifact_id
                 """)) {
            List<DependencyInfo> deps = new ArrayList<>();
            while (rs.next()) {
                String namespace = rs.getBoolean("is_org_dependency") ? "INTERNAL" : "EXTERNAL";
                deps.add(new DependencyInfo(
                    rs.getString("group_id"),
                    rs.getString("artifact_id"),
                    rs.getString("version"),
                    rs.getString("scope"),
                    namespace,
                    rs.getString("namespace"),
                    rs.getBoolean("is_jakarta_compatible"),
                    rs.getString("risk_level"),
                    rs.getString("migration_status")
                ));
            }
            return deps;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get all dependencies", e);
        }
    }

    // ==================== Utility Methods ====================

    private void saveDependencies(Connection conn, Path repositoryPath, DependencyAnalysisReport report,
                                  NamespaceCompatibilityMap namespaceMap) throws SQLException {
        String path = repositoryPath.toString();

        // Clear existing dependencies
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM dependencies WHERE repository_path = '" + path + "'");
        }

        // Insert all artifacts with org classification
        for (Artifact artifact : report.dependencyGraph().getNodes()) {
            Namespace namespace = namespaceMap.get(artifact);
            boolean isOrg = isOrgDependency(artifact.groupId());

            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO dependencies (
                    repository_path, group_id, artifact_id, version, scope,
                    is_direct, is_org_dependency, namespace, is_jakarta_compatible, risk_level, migration_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
                stmt.setString(1, path);
                stmt.setString(2, artifact.groupId());
                stmt.setString(3, artifact.artifactId());
                stmt.setString(4, artifact.version());
                stmt.setString(5, artifact.scope());
                stmt.setBoolean(6, !artifact.transitive());
                stmt.setBoolean(7, isOrg);
                stmt.setString(8, namespace.name());
                stmt.setBoolean(9, namespace == Namespace.JAKARTA);
                stmt.setString(10, determineRiskLevel(namespace));
                stmt.setString(11, determineMigrationStatus(namespace));
                stmt.executeUpdate();
            }
        }
    }

    private void saveDependencyEdges(Connection conn, Path repositoryPath, DependencyAnalysisReport report) throws SQLException {
        String path = repositoryPath.toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM dependency_edges WHERE repository_path = '" + path + "'");
        }

        for (Dependency dep : report.dependencyGraph().getEdges()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO dependency_edges (repository_path, from_group_id, from_artifact_id, to_group_id, to_artifact_id)
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

    private void saveBlockers(Connection conn, Path repositoryPath, long reportId, List<Blocker> blockers) throws SQLException {
        String path = repositoryPath.toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM blockers WHERE repository_path = '" + path + "'");
        }

        for (Blocker blocker : blockers) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO blockers (repository_path, analysis_report_id, blocker_type, description, affected_artifact, confidence)
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

    private void saveRecommendations(Connection conn, Path repositoryPath, long reportId,
                                     List<VersionRecommendation> recommendations) throws SQLException {
        String path = repositoryPath.toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM recommendations WHERE repository_path = '" + path + "'");
        }

        for (VersionRecommendation rec : recommendations) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO recommendations (repository_path, analysis_report_id, category, priority, description, action, estimated_effort)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
                stmt.setString(1, path);
                stmt.setLong(2, reportId);
                stmt.setString(3, rec.currentArtifact().groupId() + ":" + rec.currentArtifact().artifactId());
                stmt.setString(4, rec.compatibilityScore() > 0.8 ? "high" : "medium");
                stmt.setString(5, "Upgrade " + rec.currentArtifact().groupId() + ":" + rec.currentArtifact().artifactId() +
                    " from " + rec.currentArtifact().version() + " to " +
                    (rec.recommendedArtifact() != null ? rec.recommendedArtifact().version() : "latest"));
                stmt.setString(6, "Update dependency version in pom.xml/build.gradle");
                stmt.setString(7, rec.compatibilityScore() > 0.8 ? "low" : "medium");
                stmt.executeUpdate();
            }
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
            case JAVAX, MIXED -> "HIGH";
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

    public Path getDbPath() {
        return dbPath;
    }

    // ==================== Inner Classes ====================

    /**
     * Dependency info with classification (INTERNAL/EXTERNAL).
     */
    public record DependencyInfo(
        String groupId,
        String artifactId,
        String version,
        String scope,
        String classification, // "INTERNAL" or "EXTERNAL"
        String namespace,
        boolean isJakartaCompatible,
        String riskLevel,
        String migrationStatus
    ) {}

    /**
     * Organization dependency info with analysis status.
     */
    public record OrgDependencyInfo(
        String groupId,
        String artifactId,
        String version,
        String sourceRepository,
        boolean isAnalyzed,
        Instant analyzedAt,
        boolean jakartaReady,
        String migrationStatus
    ) {}

    /**
     * Repository info.
     */
    public record RepositoryInfo(
        String repositoryPath,
        String repositoryName,
        boolean isOrgRepo,
        Instant lastAnalyzedAt
    ) {}
}
