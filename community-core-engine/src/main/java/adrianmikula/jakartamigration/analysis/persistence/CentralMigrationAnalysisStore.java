package adrianmikula.jakartamigration.analysis.persistence;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Centralized SQLite-based persistence service for Jakarta migration analysis
 * data.
 * Stores data in user profile folder (~/.jakarta-migration/) to enable:
 * - Cross-repository dependency tracking
 * - Organization/internal dependency classification
 * - Analysis status tracking for org dependencies
 * - Combined view of all analyzed repositories
 */
@Slf4j
public class CentralMigrationAnalysisStore implements AutoCloseable {

    private static final String DB_FILE = "central-migration-analysis.db";

    private final Path dbPath;
    private final ObjectMapperService objectMapper;

    // User-configurable org namespace patterns
    private final Set<String> orgNamespacePatterns = new HashSet<>();

    public CentralMigrationAnalysisStore() {
        this(getCentralDbPath());
    }

    public CentralMigrationAnalysisStore(Path dbPath) {
        this.dbPath = dbPath;
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
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return groupId.equals(prefix) || groupId.startsWith(prefix + ".");
        }
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return groupId.startsWith(prefix);
        }
        return groupId.equals(pattern);
    }

    private Connection getConnection() throws SQLException {
        try {
            // Ensure the driver is loaded by the plugin classloader
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            log.error("SQLite JDBC driver not found on classpath", e);
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        conn.setAutoCommit(false);
        return conn;
    }

    private void initializeDatabase() {
        // Check if database file exists and if it was created with manual migrations
        boolean needsRebuild = false;
        if (Files.exists(dbPath)) {
            try (Connection conn = getConnection()) {
                // Check if DATABASECHANGELOG table exists (Liquibase marker)
                DatabaseMetaData metaData = conn.getMetaData();
                ResultSet tables = metaData.getTables(null, null, "DATABASECHANGELOG", null);
                if (!tables.next()) {
                    log.warn("Database exists but was not created with Liquibase. Rebuilding...");
                    needsRebuild = true;
                }
            } catch (SQLException e) {
                log.warn("Failed to check database version, rebuilding: {}", e.getMessage());
                needsRebuild = true;
            }
        }

        // If rebuild needed, delete old database file
        if (needsRebuild) {
            try {
                Files.deleteIfExists(dbPath);
                log.info("Deleted old database file for rebuild");
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete old database file", e);
            }
        }

        // Initialize database schema using custom JDBC-based migration
        // This replaces Liquibase to avoid ClassLoader issues in IntelliJ plugin environments
        try (Connection conn = getConnection()) {
            SchemaManager schemaManager = new SchemaManager(conn);
            schemaManager.initializeSchema();
            log.info("Database schema initialized successfully");

            // Populate recipes from YAML
            populateRecipesFromYaml(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    /**
     * Populates recipes table from YAML file if empty.
     */
    private void populateRecipesFromYaml(Connection conn) throws SQLException {
        // Check if recipes table is empty
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recipes")) {
            if (rs.next() && rs.getInt(1) > 0) {
                log.info("Recipes table already populated, skipping YAML import");
                return;
            }
        }

        log.info("Populating recipes table from YAML file");
        
        try {
            // Load recipes from YAML file
            InputStream yamlStream = getClass().getClassLoader()
                    .getResourceAsStream("recipes.yaml");
            if (yamlStream == null) {
                log.warn("recipes.yaml file not found in classpath");
                return;
            }

            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> yamlData = yaml.load(yamlStream);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recipes = (List<Map<String, Object>>) yamlData.get("recipes");
            
            if (recipes == null || recipes.isEmpty()) {
                log.warn("No recipes found in YAML file");
                return;
            }

            // Insert recipes into database
            String insertSql = """
                    INSERT OR REPLACE INTO recipes (
                        name, description, pattern, safety, reversible, category
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (Map<String, Object> recipe : recipes) {
                    String name = (String) recipe.get("name");
                    String description = (String) recipe.get("description");
                    String pattern = (String) recipe.get("pattern");
                    String safety = (String) recipe.get("safety");
                    Boolean reversible = (Boolean) recipe.get("reversible");
                    String category = (String) recipe.get("category");
                    
                    pstmt.setString(1, name);
                    pstmt.setString(2, description);
                    pstmt.setString(3, pattern);
                    pstmt.setString(4, safety);
                    pstmt.setBoolean(5, reversible != null ? reversible : false);
                    pstmt.setString(6, category);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            conn.commit();
            log.info("Successfully populated {} recipes from YAML file", recipes.size());
            
        } catch (Exception e) {
            log.error("Failed to populate recipes from YAML file", e);
            conn.rollback();
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
                String lastAnalyzed = rs.getString("last_analyzed_at");
                repos.add(new RepositoryInfo(
                        rs.getString("repository_path"),
                        rs.getString("repository_name"),
                        rs.getBoolean("is_org_repo"),
                        lastAnalyzed != null
                                ? Instant
                                        .parse(lastAnalyzed.replace(" ", "T") + (lastAnalyzed.contains("T") ? "" : "Z"))
                                : null));
            }
            return repos;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get repositories", e);
        }
    }

    // ==================== Org Dependency Operations ====================

    /**
     * Saves an organization dependency that needs to be tracked across
     * repositories.
     */
    public void saveOrgDependency(String groupId, String artifactId, String version,
            boolean isAnalyzed, String sourceRepositoryPath) {
        try (Connection conn = getConnection()) {
            upsertOrgDependencyInternal(conn, groupId, artifactId, version, isAnalyzed, sourceRepositoryPath, false,
                    "PENDING");
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save org dependency", e);
        }
    }

    private void upsertOrgDependencyInternal(Connection conn, String groupId, String artifactId, String version,
            boolean isAnalyzed, String sourceRepositoryPath, boolean jakartaReady, String migrationStatus)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                """
                        INSERT INTO org_dependencies (
                            group_id, artifact_id, version, source_repository_id,
                            is_analyzed, analyzed_at, jakarta_ready, migration_status
                        )
                        VALUES (?, ?, ?, (SELECT id FROM repositories WHERE repository_path = ?), ?, datetime('now'), ?, ?)
                        ON CONFLICT(group_id, artifact_id, version) DO UPDATE SET
                            is_analyzed = excluded.is_analyzed,
                            jakarta_ready = excluded.jakarta_ready,
                            migration_status = excluded.migration_status,
                            updated_at = datetime('now')
                        """)) {
            stmt.setString(1, groupId);
            stmt.setString(2, artifactId);
            stmt.setString(3, version);
            stmt.setString(4, sourceRepositoryPath);
            stmt.setBoolean(5, isAnalyzed);
            stmt.setBoolean(6, jakartaReady);
            stmt.setString(7, migrationStatus);
            stmt.executeUpdate();
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
                String analyzedAtStr = rs.getString("analyzed_at");
                deps.add(new OrgDependencyInfo(
                        rs.getString("group_id"),
                        rs.getString("artifact_id"),
                        rs.getString("version"),
                        rs.getString("source_repo"),
                        rs.getBoolean("is_analyzed"),
                        analyzedAtStr != null
                                ? Instant.parse(
                                        analyzedAtStr.replace(" ", "T") + (analyzedAtStr.contains("T") ? "" : "Z"))
                                : null,
                        rs.getBoolean("jakarta_ready"),
                        rs.getString("migration_status")));
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
                            rs.getString("migration_status")));
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
            boolean isAnalyzed, String migrationStatus) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    UPDATE org_dependencies
                    SET is_analyzed = ?,
                        analyzed_at = datetime('now'),
                        migration_status = ?,
                        updated_at = datetime('now')
                    WHERE group_id = ? AND artifact_id = ? AND version = ?
                    """)) {
                stmt.setBoolean(1, isAnalyzed);
                stmt.setString(2, migrationStatus);
                stmt.setString(3, groupId);
                stmt.setString(4, artifactId);
                stmt.setString(5, version);
                stmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark org dependency analyzed", e);
        }
    }

    // ==================== Analysis Report Operations ====================

    /**
     * Saves a complete dependency analysis report.
     */
    public void saveAnalysisReport(Path repositoryPath, DependencyAnalysisReport report, boolean isOrgRepo) {
        // Register/update repository to satisfy foreign keys
        registerRepository(repositoryPath, isOrgRepo);

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

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
                    """)) {

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
            }

            // Get the last inserted row id for SQLite
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                reportId = rs.next() ? rs.getLong(1) : -1;
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

    /**
     * Gets the latest dependency analysis report for a repository.
     */
    public DependencyAnalysisReport getLatestAnalysisReport(Path repositoryPath) {
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement("""
                        SELECT raw_report FROM analysis_reports
                        WHERE repository_path = ?
                        ORDER BY analysis_time DESC LIMIT 1
                        """)) {
            stmt.setString(1, repositoryPath.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String rawReportJson = rs.getString("raw_report");
                    if (rawReportJson != null && !rawReportJson.isEmpty()) {
                        return objectMapper.fromJson(rawReportJson, DependencyAnalysisReport.class);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get latest analysis report", e);
        }
        return null;
    }

    // ==================== Plugin State Operations ====================

    /**
     * Saves arbitrary JSON state for the plugin.
     */
    public void savePluginState(Path repositoryPath, String stateKey, String stateJson) {
        // Ensure repository exists before referencing it
        registerRepository(repositoryPath, false);

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement("""
                        INSERT INTO plugin_state (repository_path, state_key, state_json, updated_at)
                        VALUES (?, ?, ?, datetime('now'))
                        ON CONFLICT(repository_path, state_key) DO UPDATE SET
                            state_json = excluded.state_json,
                            updated_at = datetime('now')
                        """)) {
            stmt.setString(1, repositoryPath.toString());
            stmt.setString(2, stateKey);
            stmt.setString(3, stateJson);
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            log.error("Failed to save plugin state", e);
        }
    }

    /**
     * Retrieves arbitrary JSON state for the plugin.
     */
    public String getPluginState(Path repositoryPath, String stateKey) {
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement("""
                        SELECT state_json FROM plugin_state
                        WHERE repository_path = ? AND state_key = ?
                        """)) {
            stmt.setString(1, repositoryPath.toString());
            stmt.setString(2, stateKey);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("state_json");
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get plugin state", e);
        }
        return null;
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
                            rs.getString("migration_status")));
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
                        rs.getString("migration_status")));
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
                    INSERT OR REPLACE INTO dependencies (
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

            // If it's an organization dependency and this is an org repo, or if it's a
            // direct org dependency,
            // track it globally to enable cross-repository visibility.
            if (isOrg) {
                upsertOrgDependencyInternal(conn,
                        artifact.groupId(),
                        artifact.artifactId(),
                        artifact.version(),
                        false, // isAnalyzed set to false until proven otherwise
                        path,
                        namespace == Namespace.JAKARTA,
                        determineMigrationStatus(namespace));
            }
        }
    }

    private void saveDependencyEdges(Connection conn, Path repositoryPath, DependencyAnalysisReport report)
            throws SQLException {
        String path = repositoryPath.toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM dependency_edges WHERE repository_path = '" + path + "'");
        }

        for (Dependency dep : report.dependencyGraph().getEdges()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    """
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

    private void saveBlockers(Connection conn, Path repositoryPath, long reportId, List<Blocker> blockers)
            throws SQLException {
        String path = repositoryPath.toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM blockers WHERE repository_path = '" + path + "'");
        }

        for (Blocker blocker : blockers) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    """
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
            try (PreparedStatement stmt = conn.prepareStatement(
                    """
                            INSERT INTO recommendations (repository_path, analysis_report_id, category, priority, description, action, estimated_effort)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """)) {
                stmt.setString(1, path);
                stmt.setLong(2, reportId);
                stmt.setString(3, rec.currentArtifact().groupId() + ":" + rec.currentArtifact().artifactId());
                stmt.setString(4, rec.compatibilityScore() > 0.8 ? "high" : "medium");
                stmt.setString(5,
                        "Upgrade " + rec.currentArtifact().groupId() + ":" + rec.currentArtifact().artifactId() +
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
        if (score < 0.3)
            return "LOW";
        if (score < 0.7)
            return "MEDIUM";
        return "HIGH";
    }

    @Override
    public void close() {
        // No persistent connection to close
    }

    public Path getDbPath() {
        return dbPath;
    }

    // ==================== Recipe Execution Operations ====================

    /**
     * Saves a recipe execution result.
     */
    public void saveRecipeExecution(String repositoryPath, String recipeName, boolean success, String message,
            List<String> affectedFiles) {
        String sql = """
                    INSERT INTO recipe_executions (repository_path, recipe_name, success, message, affected_files)
                    VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            registerRepository(Paths.get(repositoryPath), false); // Ensure repo exists

            pstmt.setString(1, repositoryPath);
            pstmt.setString(2, recipeName);
            pstmt.setBoolean(3, success);
            pstmt.setString(4, message);
            pstmt.setString(5, affectedFiles != null ? String.join(",", affectedFiles) : "");

            pstmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            log.error("Failed to save recipe execution", e);
        }
    }

    /**
     * Gets execution history for a specific recipe in a repository.
     */
    public List<Map<String, Object>> getRecipeExecutions(String repositoryPath, String recipeName) {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT * FROM recipe_executions WHERE repository_path = ? AND recipe_name = ? ORDER BY executed_at DESC";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, repositoryPath);
            pstmt.setString(2, recipeName);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("recipe_name", rs.getString("recipe_name"));
                    map.put("executed_at", rs.getString("executed_at"));
                    map.put("success", rs.getBoolean("success"));
                    map.put("message", rs.getString("message"));
                    String affectedFiles = rs.getString("affected_files");
                    map.put("affected_files", (affectedFiles == null || affectedFiles.isEmpty())
                            ? Collections.emptyList()
                            : Arrays.asList(affectedFiles.split(",")));
                    results.add(map);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get recipe executions", e);
        }
        return results;
    }

    /**
     * Gets all available migration recipes from the catalog.
     */
    public List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition> getRecipes() {
        return getRecipesByQuery("SELECT * FROM recipes ORDER BY category, name");
    }

    /**
     * Gets recipes by category.
     */
    public List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition> getRecipesByCategory(
            String category) {
        return getRecipesByQuery("SELECT * FROM recipes WHERE category = ? ORDER BY name", category);
    }

    private List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition> getRecipesByQuery(String sql,
            String... params) {
        List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition> recipes = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setString(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recipes.add(mapRecipe(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query recipes: " + sql, e);
        }
        return recipes;
    }

    private adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition mapRecipe(ResultSet rs)
            throws SQLException {
        return adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .category(adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory
                        .valueOf(rs.getString("category")))
                .recipeType(adrianmikula.jakartamigration.coderefactoring.domain.RecipeType
                        .valueOf(rs.getString("recipe_type")))
                .openRewriteRecipeName(rs.getString("openrewrite_recipe_name"))
                .pattern(rs.getString("pattern"))
                .replacement(rs.getString("replacement"))
                .filePattern(rs.getString("file_pattern"))
                .reversible(rs.getBoolean("reversible"))
                .createdAt(Instant.parse(rs.getString("created_at").replace(" ", "T") + "Z"))
                .status(adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.NEVER_RUN) // Default
                .build();
    }

    /**
     * Saves or updates a recipe in the catalog.
     */
    public void saveRecipe(adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition recipe) {
        log.info("saveRecipe called: name={}, type={}, openRewriteRecipeName={}", 
                recipe.getName(), recipe.getRecipeType(), recipe.getOpenRewriteRecipeName());
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT INTO recipes (
                        name, description, category, recipe_type, openrewrite_recipe_name,
                        pattern, replacement, file_pattern, reversible, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
                    ON CONFLICT(name) DO UPDATE SET
                        description = excluded.description,
                        category = excluded.category,
                        recipe_type = excluded.recipe_type,
                        openrewrite_recipe_name = excluded.openrewrite_recipe_name,
                        pattern = excluded.pattern,
                        replacement = excluded.replacement,
                        file_pattern = excluded.file_pattern,
                        reversible = excluded.reversible
                    """)) {
                stmt.setString(1, recipe.getName());
                stmt.setString(2, recipe.getDescription());
                stmt.setString(3, recipe.getCategory().name());
                stmt.setString(4, recipe.getRecipeType().name());
                stmt.setString(5, recipe.getOpenRewriteRecipeName());
                stmt.setString(6, recipe.getPattern());
                stmt.setString(7, recipe.getReplacement());
                stmt.setString(8, recipe.getFilePattern());
                stmt.setBoolean(9, recipe.isReversible());
                stmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("Failed to save recipe: " + recipe.getName(), e);
        }
    }

    /**
     * Deletes all recipes from the catalog.
     * Used to ensure DB matches YAML configuration exactly on startup.
     */
    public void deleteAllRecipes() {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM recipes");
            }
            conn.commit();
            log.info("Deleted all recipes from catalog");
        } catch (SQLException e) {
            log.error("Failed to delete all recipes", e);
        }
    }

    /**
     * Gets all recipe executions for a repository, ordered by most recent first.
     */
    public List<Map<String, Object>> getAllRecipeExecutions(String repositoryPath) {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT * FROM recipe_executions WHERE repository_path = ? ORDER BY executed_at DESC LIMIT 100";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, repositoryPath);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("recipe_name", rs.getString("recipe_name"));
                    map.put("executed_at", rs.getString("executed_at"));
                    map.put("success", rs.getBoolean("success"));
                    map.put("message", rs.getString("message"));
                    String affectedFiles = rs.getString("affected_files");
                    map.put("affected_files", (affectedFiles == null || affectedFiles.isEmpty())
                            ? Collections.emptyList()
                            : Arrays.asList(affectedFiles.split(",")));
                    results.add(map);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get all recipe executions", e);
        }
        return results;
    }

    // ==================== Upgrade Recommendation Operations ====================

    /**
     * Saves an upgrade recommendation mapping javax artifact to jakarta equivalent.
     */
    public void saveUpgradeRecommendation(String currentGroupId, String currentArtifactId,
            String recommendedGroupId, String recommendedArtifactId, String recommendedVersion,
            String associatedRecipeName) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT INTO upgrade_recommendations (
                        current_group_id, current_artifact_id, recommended_group_id,
                        recommended_artifact_id, recommended_version, associated_recipe_name
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(current_group_id, current_artifact_id) DO UPDATE SET
                        recommended_group_id = excluded.recommended_group_id,
                        recommended_artifact_id = excluded.recommended_artifact_id,
                        recommended_version = excluded.recommended_version,
                        associated_recipe_name = excluded.associated_recipe_name
                    """)) {
                stmt.setString(1, currentGroupId);
                stmt.setString(2, currentArtifactId);
                stmt.setString(3, recommendedGroupId);
                stmt.setString(4, recommendedArtifactId);
                stmt.setString(5, recommendedVersion);
                stmt.setString(6, associatedRecipeName);
                stmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("Failed to save upgrade recommendation for {}:{}", currentGroupId, currentArtifactId, e);
        }
    }

    /**
     * Gets an upgrade recommendation for a specific javax artifact.
     */
    public UpgradeRecommendation getUpgradeRecommendation(String groupId, String artifactId) {
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement("""
                        SELECT * FROM upgrade_recommendations
                        WHERE current_group_id = ? AND current_artifact_id = ?
                        """)) {
            stmt.setString(1, groupId);
            stmt.setString(2, artifactId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UpgradeRecommendation(
                            rs.getString("current_group_id"),
                            rs.getString("current_artifact_id"),
                            rs.getString("recommended_group_id"),
                            rs.getString("recommended_artifact_id"),
                            rs.getString("recommended_version"),
                            rs.getString("associated_recipe_name"));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get upgrade recommendation for {}:{}", groupId, artifactId, e);
        }
        return null;
    }

    /**
     * Gets all upgrade recommendations.
     */
    public List<UpgradeRecommendation> getAllUpgradeRecommendations() {
        List<UpgradeRecommendation> recommendations = new ArrayList<>();
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("""
                        SELECT * FROM upgrade_recommendations ORDER BY current_group_id, current_artifact_id
                        """)) {
            while (rs.next()) {
                recommendations.add(new UpgradeRecommendation(
                        rs.getString("current_group_id"),
                        rs.getString("current_artifact_id"),
                        rs.getString("recommended_group_id"),
                        rs.getString("recommended_artifact_id"),
                        rs.getString("recommended_version"),
                        rs.getString("associated_recipe_name")));
            }
        } catch (SQLException e) {
            log.error("Failed to get all upgrade recommendations", e);
        }
        return recommendations;
    }

    // ==================== Inner Classes ====================

    /**
     * Upgrade recommendation mapping javax artifact to jakarta equivalent.
     */
    public record UpgradeRecommendation(
            String currentGroupId,
            String currentArtifactId,
            String recommendedGroupId,
            String recommendedArtifactId,
            String recommendedVersion,
            String associatedRecipeName) {
    }

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
            String migrationStatus) {
    }

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
            String migrationStatus) {
    }

    /**
     * Repository info.
     */
    public record RepositoryInfo(
            String repositoryPath,
            String repositoryName,
            boolean isOrgRepo,
            Instant lastAnalyzedAt) {
    }
}
