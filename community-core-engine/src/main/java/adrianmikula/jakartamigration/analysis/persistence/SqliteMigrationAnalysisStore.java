package adrianmikula.jakartamigration.analysis.persistence;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private static final int DB_VERSION = 2;

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
        log.info("Jakarta Migration Plugin - Local SQLite database initialized at: {}", dbPath);
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
            populateMigrationIssuesRegistry(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Populates migration_issues_registry table with default scanner types.
     */
    private void populateMigrationIssuesRegistry(Connection conn) throws SQLException {
        // Check if registry table is already populated
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM migration_issues_registry")) {
            if (rs.next() && rs.getInt(1) > 0) {
                log.info("Migration issues registry already populated, skipping default entries");
                return;
            }
        }

        log.info("Populating migration issues registry with default scanner types");
        
        try {
            // Default migration issues registry entries
            String[] scannerTypes = {
                "JPA_ANNOTATION_SCANNER",
                "BEAN_VALIDATION_SCANNER", 
                "SERVLET_JSP_SCANNER",
                "CDI_INJECTION_SCANNER",
                "BUILD_CONFIG_SCANNER",
                "REST_SOAP_SCANNER",
                "DEPRECATED_API_SCANNER",
                "SECURITY_API_SCANNER",
                "JMS_MESSAGING_SCANNER",
                "TRANSITIVE_DEPENDENCY_SCANNER",
                "CONFIG_FILE_SCANNER",
                "CLASSLOADER_MODULE_SCANNER",
                "LOGGING_METRICS_SCANNER",
                "SERIALIZATION_CACHE_SCANNER",
                "REFLECTION_USAGE_SCANNER",
                "THIRD_PARTY_LIB_SCANNER"
            };

            String insertSql = """
                    INSERT OR REPLACE INTO migration_issues_registry
                    (scanner_type, ui_tab_name, legacy_namespace, target_namespace, refactor_recipe, description, anticipated_error_messages, solution_hint, is_premium)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (String scannerType : scannerTypes) {
                    String uiTabName = mapScannerTypeToUITab(scannerType);
                    String legacyNamespace = mapScannerTypeToLegacyNamespace(scannerType);
                    String targetNamespace = mapScannerTypeToTargetNamespace(scannerType);
                    String refactorRecipe = mapScannerTypeToRecipe(scannerType);
                    String description = mapScannerTypeToDescription(scannerType);
                    String anticipatedErrors = mapScannerTypeToAnticipatedErrors(scannerType);
                    String solutionHint = mapScannerTypeToSolutionHint(scannerType);
                    boolean isPremium = mapScannerTypeToPremium(scannerType);
                    
                    pstmt.setString(1, scannerType);
                    pstmt.setString(2, uiTabName);
                    pstmt.setString(3, legacyNamespace);
                    pstmt.setString(4, targetNamespace);
                    pstmt.setString(5, refactorRecipe);
                    pstmt.setString(6, description);
                    pstmt.setString(7, anticipatedErrors);
                    pstmt.setString(8, solutionHint);
                    pstmt.setBoolean(9, isPremium);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            conn.commit();
            log.info("Successfully populated {} scanner types in migration issues registry", scannerTypes.length);
            
        } catch (Exception e) {
            log.error("Failed to populate migration issues registry", e);
            conn.rollback();
        }
    }

    private String mapScannerTypeToUITab(String scannerType) {
        return switch (scannerType) {
            case "JPA_ANNOTATION_SCANNER" -> "Advanced Scans";
            case "BEAN_VALIDATION_SCANNER" -> "Advanced Scans";
            case "SERVLET_JSP_SCANNER" -> "Advanced Scans";
            case "CDI_INJECTION_SCANNER" -> "Advanced Scans";
            case "BUILD_CONFIG_SCANNER" -> "Advanced Scans";
            case "REST_SOAP_SCANNER" -> "Advanced Scans";
            case "DEPRECATED_API_SCANNER" -> "Advanced Scans";
            case "SECURITY_API_SCANNER" -> "Advanced Scans";
            case "JMS_MESSAGING_SCANNER" -> "Advanced Scans";
            case "TRANSITIVE_DEPENDENCY_SCANNER" -> "Advanced Scans";
            case "CONFIG_FILE_SCANNER" -> "Advanced Scans";
            case "CLASSLOADER_MODULE_SCANNER" -> "Advanced Scans";
            case "LOGGING_METRICS_SCANNER" -> "Advanced Scans";
            case "SERIALIZATION_CACHE_SCANNER" -> "Advanced Scans";
            case "REFLECTION_USAGE_SCANNER" -> "Advanced Scans";
            case "THIRD_PARTY_LIB_SCANNER" -> "Advanced Scans";
            default -> "Unknown";
        };
    }

    private String mapScannerTypeToLegacyNamespace(String scannerType) {
        return switch (scannerType) {
            case "JPA_ANNOTATION_SCANNER" -> "javax.persistence";
            case "BEAN_VALIDATION_SCANNER" -> "javax.validation";
            case "SERVLET_JSP_SCANNER" -> "javax.servlet";
            case "CDI_INJECTION_SCANNER" -> "javax.enterprise";
            case "BUILD_CONFIG_SCANNER" -> "javax.tools";
            case "REST_SOAP_SCANNER" -> "javax.ws.rs";
            case "DEPRECATED_API_SCANNER" -> "javax";
            case "SECURITY_API_SCANNER" -> "javax.security";
            case "JMS_MESSAGING_SCANNER" -> "javax.jms";
            case "TRANSITIVE_DEPENDENCY_SCANNER" -> "javax";
            case "CONFIG_FILE_SCANNER" -> "javax";
            case "CLASSLOADER_MODULE_SCANNER" -> "javax";
            case "LOGGING_METRICS_SCANNER" -> "javax";
            case "SERIALIZATION_CACHE_SCANNER" -> "javax";
            case "REFLECTION_USAGE_SCANNER" -> "javax";
            case "THIRD_PARTY_LIB_SCANNER" -> "javax";
            default -> "javax";
        };
    }

    private String mapScannerTypeToTargetNamespace(String scannerType) {
        return switch (scannerType) {
            case "JPA_ANNOTATION_SCANNER" -> "jakarta.persistence";
            case "BEAN_VALIDATION_SCANNER" -> "jakarta.validation";
            case "SERVLET_JSP_SCANNER" -> "jakarta.servlet";
            case "CDI_INJECTION_SCANNER" -> "jakarta.enterprise";
            case "BUILD_CONFIG_SCANNER" -> "jakarta.tools";
            case "REST_SOAP_SCANNER" -> "jakarta.ws.rs";
            case "DEPRECATED_API_SCANNER" -> "jakarta";
            case "SECURITY_API_SCANNER" -> "jakarta.security";
            case "JMS_MESSAGING_SCANNER" -> "jakarta.jms";
            case "TRANSITIVE_DEPENDENCY_SCANNER" -> "jakarta";
            case "CONFIG_FILE_SCANNER" -> "jakarta";
            case "CLASSLOADER_MODULE_SCANNER" -> "jakarta";
            case "LOGGING_METRICS_SCANNER" -> "jakarta";
            case "SERIALIZATION_CACHE_SCANNER" -> "jakarta";
            case "REFLECTION_USAGE_SCANNER" -> "jakarta";
            case "THIRD_PARTY_LIB_SCANNER" -> "jakarta";
            default -> "jakarta";
        };
    }

    private String mapScannerTypeToRecipe(String scannerType) {
        return switch (scannerType) {
            case "JPA_ANNOTATION_SCANNER" -> "JavaxPersistenceToJakartaPersistence";
            case "BEAN_VALIDATION_SCANNER" -> "JavaxValidationToJakartaValidation";
            case "SERVLET_JSP_SCANNER" -> "JavaxServletToJakartaServlet";
            case "CDI_INJECTION_SCANNER" -> "JavaxEnterpriseToJakartaEnterprise";
            case "BUILD_CONFIG_SCANNER" -> "JavaxToolsToJakartaTools";
            case "REST_SOAP_SCANNER" -> "JavaxWsRsToJakartaWsRs";
            case "DEPRECATED_API_SCANNER" -> "JavaxToJakarta";
            case "SECURITY_API_SCANNER" -> "JavaxSecurityToJakartaSecurity";
            case "JMS_MESSAGING_SCANNER" -> "JavaxJmsToJakartaJms";
            case "TRANSITIVE_DEPENDENCY_SCANNER" -> "JavaxToJakarta";
            case "CONFIG_FILE_SCANNER" -> "JavaxToJakarta";
            case "CLASSLOADER_MODULE_SCANNER" -> "JavaxToJakarta";
            case "LOGGING_METRICS_SCANNER" -> "JavaxToJakarta";
            case "SERIALIZATION_CACHE_SCANNER" -> "JavaxToJakarta";
            case "REFLECTION_USAGE_SCANNER" -> "JavaxToJakarta";
            case "THIRD_PARTY_LIB_SCANNER" -> "JavaxToJakarta";
            default -> "JavaxToJakarta";
        };
    }

    private String mapScannerTypeToDescription(String scannerType) {
        return switch (scannerType) {
            case "JPA_ANNOTATION_SCANNER" -> "Detects JPA annotations that need migration";
            case "BEAN_VALIDATION_SCANNER" -> "Detects Bean Validation annotations that need migration";
            case "SERVLET_JSP_SCANNER" -> "Detects Servlet/JSP APIs that need migration";
            case "CDI_INJECTION_SCANNER" -> "Detects CDI annotations that need migration";
            case "BUILD_CONFIG_SCANNER" -> "Detects build configuration issues";
            case "REST_SOAP_SCANNER" -> "Detects REST/SOAP APIs that need migration";
            case "DEPRECATED_API_SCANNER" -> "Detects deprecated javax APIs";
            case "SECURITY_API_SCANNER" -> "Detects security APIs that need migration";
            case "JMS_MESSAGING_SCANNER" -> "Detects JMS APIs that need migration";
            case "TRANSITIVE_DEPENDENCY_SCANNER" -> "Detects transitive dependencies that need migration";
            case "CONFIG_FILE_SCANNER" -> "Detects configuration files that need migration";
            case "CLASSLOADER_MODULE_SCANNER" -> "Detects classloader/module issues";
            case "LOGGING_METRICS_SCANNER" -> "Detects logging/metrics APIs that need migration";
            case "SERIALIZATION_CACHE_SCANNER" -> "Detects serialization/cache APIs that need migration";
            case "REFLECTION_USAGE_SCANNER" -> "Detects reflection usage that may be affected";
            case "THIRD_PARTY_LIB_SCANNER" -> "Detects third-party libraries that need Jakarta compatibility";
            default -> "Unknown scanner type";
        };
    }

    private String mapScannerTypeToAnticipatedErrors(String scannerType) {
        return switch (scannerType) {
            case "JPA_ANNOTATION_SCANNER" -> "ClassNotFoundException: javax.persistence.Entity";
            case "BEAN_VALIDATION_SCANNER" -> "ClassNotFoundException: javax.validation.Constraint";
            case "SERVLET_JSP_SCANNER" -> "ClassNotFoundException: javax.servlet.HttpServlet";
            case "CDI_INJECTION_SCANNER" -> "ClassNotFoundException: javax.enterprise.inject.Inject";
            case "BUILD_CONFIG_SCANNER" -> "Build configuration issues";
            case "REST_SOAP_SCANNER" -> "ClassNotFoundException: javax.ws.rs.Path";
            case "DEPRECATED_API_SCANNER" -> "Deprecated API usage warnings";
            case "SECURITY_API_SCANNER" -> "ClassNotFoundException: javax.security.auth";
            case "JMS_MESSAGING_SCANNER" -> "ClassNotFoundException: javax.jms.Message";
            case "TRANSITIVE_DEPENDENCY_SCANNER" -> "Dependency resolution issues";
            case "CONFIG_FILE_SCANNER" -> "Configuration file parsing errors";
            case "CLASSLOADER_MODULE_SCANNER" -> "Class loading issues";
            case "LOGGING_METRICS_SCANNER" -> "Logging framework issues";
            case "SERIALIZATION_CACHE_SCANNER" -> "Serialization compatibility issues";
            case "REFLECTION_USAGE_SCANNER" -> "Reflection access issues";
            case "THIRD_PARTY_LIB_SCANNER" -> "Third-party library compatibility issues";
            default -> "Unknown error patterns";
        };
    }

    private String mapScannerTypeToSolutionHint(String scannerType) {
        return switch (scannerType) {
            case "JPA_ANNOTATION_SCANNER" -> "Use JPA migration recipe to update annotations";
            case "BEAN_VALIDATION_SCANNER" -> "Use Bean Validation migration recipe";
            case "SERVLET_JSP_SCANNER" -> "Use Servlet migration recipe";
            case "CDI_INJECTION_SCANNER" -> "Use CDI migration recipe";
            case "BUILD_CONFIG_SCANNER" -> "Update build configuration for Jakarta EE";
            case "REST_SOAP_SCANNER" -> "Use JAX-RS migration recipe";
            case "DEPRECATED_API_SCANNER" -> "Replace deprecated APIs with Jakarta equivalents";
            case "SECURITY_API_SCANNER" -> "Use security API migration recipe";
            case "JMS_MESSAGING_SCANNER" -> "Use JMS migration recipe";
            case "TRANSITIVE_DEPENDENCY_SCANNER" -> "Update dependencies to Jakarta versions";
            case "CONFIG_FILE_SCANNER" -> "Update configuration files for Jakarta EE";
            case "CLASSLOADER_MODULE_SCANNER" -> "Update classloader/module configuration";
            case "LOGGING_METRICS_SCANNER" -> "Update logging/metrics configuration";
            case "SERIALIZATION_CACHE_SCANNER" -> "Update serialization/cache configuration";
            case "REFLECTION_USAGE_SCANNER" -> "Update reflection calls for Jakarta compatibility";
            case "THIRD_PARTY_LIB_SCANNER" -> "Update third-party libraries to Jakarta-compatible versions";
            default -> "Contact support for migration assistance";
        };
    }

    private boolean mapScannerTypeToPremium(String scannerType) {
        return true; // All advanced scanners are premium features
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

            // Migration Issues Registry - maps scanner types to namespaces and refactor
            // recipes
            // This enables looking up what scanner handles which issue type
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS migration_issues_registry (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            scanner_type TEXT NOT NULL UNIQUE,
                            ui_tab_name TEXT NOT NULL,
                            legacy_namespace TEXT NOT NULL,
                            target_namespace TEXT NOT NULL,
                            refactor_recipe TEXT,
                            description TEXT,
                            anticipated_error_messages TEXT,
                            solution_hint TEXT,
                            is_premium BOOLEAN DEFAULT FALSE,
                            created_at TEXT DEFAULT (datetime('now'))
                        )
                    """);

            // Migration Issues - stores found issues from scans
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS migration_issues (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            project_path TEXT NOT NULL,
                            registry_id INTEGER NOT NULL,
                            file_path TEXT,
                            line_number INTEGER,
                            column_number INTEGER,
                            code_snippet TEXT,
                            issue_severity TEXT DEFAULT 'medium',
                            is_resolved BOOLEAN DEFAULT FALSE,
                            resolved_at TEXT,
                            created_at TEXT DEFAULT (datetime('now')),
                            FOREIGN KEY (registry_id) REFERENCES migration_issues_registry(id)
                        )
                    """);

            // Indexes for efficient querying
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_deps_project ON dependencies(project_path)");
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_edges_from ON dependency_edges(project_path, from_group_id, from_artifact_id)");
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_edges_to ON dependency_edges(project_path, to_group_id, to_artifact_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_blockers_project ON blockers(project_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_recommendations_project ON recommendations(project_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_phases_plan ON migration_phases(plan_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_issues_project ON migration_issues(project_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_issues_registry ON migration_issues(registry_id)");

            // Recipe executions table (Req: "New record is added to the history DB every
            // time a refactor recipe is started")
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS recipe_executions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            project_path TEXT NOT NULL,
                            recipe_name TEXT NOT NULL,
                            executed_at TEXT DEFAULT (datetime('now')),
                            success BOOLEAN,
                            message TEXT,
                            undo_execution_id INTEGER,
                            is_undo BOOLEAN DEFAULT FALSE,
                            FOREIGN KEY (undo_execution_id) REFERENCES recipe_executions(id)
                        )
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_recipe_exec_project ON recipe_executions(project_path)");

            // Changed files join table (Req: "stored in a separate DB table with a join key
            // to the main history table")
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS recipe_changed_files (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            execution_id INTEGER NOT NULL,
                            file_path TEXT NOT NULL,
                            original_content TEXT,
                            FOREIGN KEY(execution_id) REFERENCES recipe_executions(id) ON DELETE CASCADE
                        )
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_changed_files_exec ON recipe_changed_files(execution_id)");

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
                if (version < 2) {
                    // Upgrade to version 2: add refactoring history tables
                    stmt.execute("""
                                CREATE TABLE IF NOT EXISTS recipe_executions (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    project_path TEXT NOT NULL,
                                    recipe_name TEXT NOT NULL,
                                    executed_at TEXT DEFAULT (datetime('now')),
                                    success BOOLEAN,
                                    message TEXT,
                                    undo_execution_id INTEGER,
                                    is_undo BOOLEAN DEFAULT FALSE,
                                    FOREIGN KEY (undo_execution_id) REFERENCES recipe_executions(id)
                                )
                            """);
                    stmt.execute(
                            "CREATE INDEX IF NOT EXISTS idx_recipe_exec_project ON recipe_executions(project_path)");

                    stmt.execute("""
                                CREATE TABLE IF NOT EXISTS recipe_changed_files (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    execution_id INTEGER NOT NULL,
                                    file_path TEXT NOT NULL,
                                    original_content TEXT,
                                    FOREIGN KEY(execution_id) REFERENCES recipe_executions(id) ON DELETE CASCADE
                                )
                            """);
                    stmt.execute(
                            "CREATE INDEX IF NOT EXISTS idx_changed_files_exec ON recipe_changed_files(execution_id)");
                }
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
                    """)) {

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
            }

            // Get the last inserted row id for SQLite
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                reportId = rs.next() ? rs.getLong(1) : -1;
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
                    String lastTime = rs.getString("analysis_time");
                    return Optional.of(new AnalysisSummary(
                            rs.getInt("total_dependencies"),
                            rs.getInt("direct_dependencies"),
                            rs.getInt("jakarta_ready_count"),
                            rs.getInt("needs_migration_count"),
                            rs.getInt("blocked_count"),
                            rs.getDouble("readiness_score"),
                            rs.getString("risk_level"),
                            lastTime != null
                                    ? Instant.parse(lastTime.replace(" ", "T") + (lastTime.contains("T") ? "" : "Z"))
                                    : Instant.now()));
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
                            rs.getString("migration_status")));
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
                            rs.getString("migration_status")));
                }
                return deps;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get dependencies needing migration", e);
        }
    }

    // ==================== Migration Plan Operations ====================

    // TODO: Re-enable when MigrationPlan domain class is reimplemented (see
    // REFACTOR.md)
    // saveMigrationPlan and loadLatestMigrationPlan methods removed because they
    // referenced deleted coderefactoring.domain.MigrationPlan and RefactoringPhase
    // types.

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
                            rs.getDouble("confidence")));
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
                            rs.getString("estimated_effort")));
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
                if (rs.next()) {
                    String lastTime = rs.getString("last_time");
                    if (lastTime != null) {
                        return Optional
                                .of(Instant.parse(lastTime.replace(" ", "T") + (lastTime.contains("T") ? "" : "Z")));
                    }
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

    private void saveDependencies(Connection conn, Path projectPath, DependencyAnalysisReport report,
            NamespaceCompatibilityMap namespaceMap) throws SQLException {
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

    private void saveDependencyEdges(Connection conn, Path projectPath, DependencyAnalysisReport report)
            throws SQLException {
        String path = projectPath.toString();

        // Clear existing edges
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM dependency_edges WHERE project_path = '" + path + "'");
        }

        // Insert edges from the graph
        for (Dependency dep : report.dependencyGraph().getEdges()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    """
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

    private void saveBlockers(Connection conn, Path projectPath, long reportId, List<Blocker> blockers)
            throws SQLException {
        String path = projectPath.toString();

        // Clear existing blockers
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM blockers WHERE project_path = '" + path + "'");
        }

        for (Blocker blocker : blockers) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    """
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

    private void saveRecommendations(Connection conn, Path projectPath, long reportId,
            List<VersionRecommendation> recommendations) throws SQLException {
        String path = projectPath.toString();

        // Clear existing recommendations
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM recommendations WHERE project_path = '" + path + "'");
        }

        for (VersionRecommendation rec : recommendations) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    """
                            INSERT INTO recommendations (project_path, analysis_report_id, category, priority, description, action, estimated_effort)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """)) {
                stmt.setString(1, path);
                stmt.setLong(2, reportId);
                stmt.setString(3, rec.currentArtifact().groupId() + ":" + rec.currentArtifact().artifactId());
                stmt.setString(4, rec.compatibilityScore() > 0.8 ? "high" : "medium");
                stmt.setString(5,
                        "Upgrade " + rec.currentArtifact().groupId() + ":" + rec.currentArtifact().artifactId()
                                + " from " + rec.currentArtifact().version() + " to "
                                + (rec.recommendedArtifact() != null ? rec.recommendedArtifact().version() : "latest"));
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
        if (score < 0.3)
            return "LOW";
        if (score < 0.7)
            return "MEDIUM";
        return "HIGH";
    }

    // ==================== Migration Issues Registry Operations
    // ====================

    /**
     * Registers a new scanner type in the migration issues registry.
     * This maps scanner types to their UI tab, namespaces, and refactor recipe.
     */
    public void registerMigrationIssueType(
            String scannerType,
            String uiTabName,
            String legacyNamespace,
            String targetNamespace,
            String refactorRecipe,
            String description,
            String anticipatedErrorMessages,
            String solutionHint,
            boolean isPremium) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    """
                            INSERT OR REPLACE INTO migration_issues_registry
                            (scanner_type, ui_tab_name, legacy_namespace, target_namespace, refactor_recipe, description, anticipated_error_messages, solution_hint, is_premium)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """)) {
                stmt.setString(1, scannerType);
                stmt.setString(2, uiTabName);
                stmt.setString(3, legacyNamespace);
                stmt.setString(4, targetNamespace);
                stmt.setString(5, refactorRecipe);
                stmt.setString(6, description);
                stmt.setString(7, anticipatedErrorMessages);
                stmt.setString(8, solutionHint);
                stmt.setBoolean(9, isPremium);
                stmt.executeUpdate();
                conn.commit();
                log.info("Registered migration issue type: {}", scannerType);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register migration issue type: " + scannerType, e);
        }
    }

    /**
     * Gets all registered migration issue types.
     */
    public List<MigrationIssueRegistry> getRegisteredIssueTypes() {
        List<MigrationIssueRegistry> results = new ArrayList<>();
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM migration_issues_registry")) {
                while (rs.next()) {
                    results.add(new MigrationIssueRegistry(
                            rs.getLong("id"),
                            rs.getString("scanner_type"),
                            rs.getString("ui_tab_name"),
                            rs.getString("legacy_namespace"),
                            rs.getString("target_namespace"),
                            rs.getString("refactor_recipe"),
                            rs.getString("description"),
                            rs.getString("anticipated_error_messages"),
                            rs.getString("solution_hint"),
                            rs.getBoolean("is_premium")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get registered issue types", e);
        }
        return results;
    }

    /**
     * Gets a registered issue type by scanner type.
     */
    public Optional<MigrationIssueRegistry> getRegistryByScannerType(String scannerType) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn
                    .prepareStatement("SELECT * FROM migration_issues_registry WHERE scanner_type = ?")) {
                stmt.setString(1, scannerType);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new MigrationIssueRegistry(
                                rs.getLong("id"),
                                rs.getString("scanner_type"),
                                rs.getString("ui_tab_name"),
                                rs.getString("legacy_namespace"),
                                rs.getString("target_namespace"),
                                rs.getString("refactor_recipe"),
                                rs.getString("description"),
                                rs.getString("anticipated_error_messages"),
                                rs.getString("solution_hint"),
                                rs.getBoolean("is_premium")));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get registry by scanner type: " + scannerType, e);
        }
        return Optional.empty();
    }

    /**
     * Saves a recipe execution history record.
     */
    public long saveRecipeExecution(Path projectPath,
            RecipeExecutionHistory history) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT INTO recipe_executions (
                        project_path, recipe_name, executed_at, success, message,
                        undo_execution_id, is_undo
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                stmt.setString(1, projectPath.toString());
                stmt.setString(2, history.getRecipeName());
                stmt.setString(3, history.getExecutedAt() != null ? history.getExecutedAt().toString()
                        : Instant.now().toString());
                stmt.setBoolean(4, history.isSuccess());
                stmt.setString(5, history.getMessage());
                if (history.getUndoExecutionId() != null) {
                    stmt.setLong(6, history.getUndoExecutionId());
                } else {
                    stmt.setNull(6, Types.INTEGER);
                }
                stmt.setBoolean(7, history.isUndo());
                stmt.executeUpdate();

                try (Statement idStmt = conn.createStatement();
                        ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        history.setId(id);
                        conn.commit();
                        return id;
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("Failed to save recipe execution: " + history.getRecipeName(), e);
        }
        return -1;
    }

    /**
     * Updates an existing recipe execution history record.
     */
    public void updateRecipeExecution(
            RecipeExecutionHistory history) {
        if (history.getId() == null) {
            return;
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    UPDATE recipe_executions SET
                        success = ?, message = ?, undo_execution_id = ?
                    WHERE id = ?
                    """)) {
                stmt.setBoolean(1, history.isSuccess());
                stmt.setString(2, history.getMessage());
                if (history.getUndoExecutionId() != null) {
                    stmt.setLong(3, history.getUndoExecutionId());
                } else {
                    stmt.setNull(3, Types.INTEGER);
                }
                stmt.setLong(4, history.getId());
                stmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("Failed to update recipe execution: " + history.getId(), e);
        }
    }

    /**
     * Saves a record of a file changed by a recipe.
     */
    public void saveRecipeChangedFile(long executionId, String filePath, String originalContent) {
        log.debug("Saving changed file for execution {}: {}", executionId, filePath);
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT INTO recipe_changed_files (execution_id, file_path, original_content)
                    VALUES (?, ?, ?)
                    """)) {
                stmt.setLong(1, executionId);
                stmt.setString(2, filePath);
                stmt.setString(3, originalContent);
                int rowsAffected = stmt.executeUpdate();
                conn.commit();
                log.debug("Saved changed file: {} rows affected for execution {}", rowsAffected, executionId);
            }
        } catch (SQLException e) {
            log.error("Failed to save recipe changed file for execution: " + executionId, e);
        }
    }

    /**
     * Links a historical record to its undo action record.
     */
    public void linkUndoExecution(long originalId, long undoId) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    UPDATE recipe_executions SET undo_execution_id = ? WHERE id = ?
                    """)) {
                stmt.setLong(1, undoId);
                stmt.setLong(2, originalId);
                stmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("Failed to link undo execution: " + originalId + " -> " + undoId, e);
        }
    }

    /**
     * Gets changed files for a specific execution.
     */
    public List<Map<String, String>> getChangedFiles(long executionId) {
        List<Map<String, String>> files = new ArrayList<>();
        log.debug("Getting changed files for execution: {}", executionId);
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    SELECT file_path, original_content FROM recipe_changed_files WHERE execution_id = ?
                    """)) {
                stmt.setLong(1, executionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> fileInfo = new HashMap<>();
                        fileInfo.put("filePath", rs.getString("file_path"));
                        fileInfo.put("originalContent", rs.getString("original_content"));
                        files.add(fileInfo);
                    }
                }
            }
            log.debug("Found {} changed files for execution {}", files.size(), executionId);
        } catch (SQLException e) {
            log.error("Failed to get changed files for execution: " + executionId, e);
        }
        return files;
    }

    /**
     * Gets recipe execution history for a project.
     */
    public List<RecipeExecutionHistory> getRecipeHistory(
            Path projectPath) {
        log.info("Querying recipe history for project: {}", projectPath);
        List<RecipeExecutionHistory> historyList = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // First get executions
            Map<Long, RecipeExecutionHistory> historyMap = new LinkedHashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement("""
                    SELECT * FROM recipe_executions WHERE project_path = ? ORDER BY executed_at DESC
                    """)) {
                stmt.setString(1, projectPath.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        RecipeExecutionHistory h = RecipeExecutionHistory
                                .builder()
                                .id(id)
                                .recipeName(rs.getString("recipe_name"))
                                .executedAt(Instant.parse(rs.getString("executed_at")))
                                .success(rs.getBoolean("success"))
                                .message(rs.getString("message"))
                                .undoExecutionId(rs.getObject("undo_execution_id") != null
                                        ? rs.getLong("undo_execution_id")
                                        : null)
                                .isUndo(rs.getBoolean("is_undo"))
                                .affectedFiles(new ArrayList<>())
                                .build();
                        historyMap.put(id, h);
                        historyList.add(h);
                    }
                }
            }

            // Then get all changed files for these executions in one go
            if (!historyList.isEmpty()) {
                String ids = historyMap.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(
                                "SELECT execution_id, file_path FROM recipe_changed_files WHERE execution_id IN (" + ids
                                        + ")")) {
                    while (rs.next()) {
                        long execId = rs.getLong("execution_id");
                        String path = rs.getString("file_path");
                        if (historyMap.containsKey(execId)) {
                            historyMap.get(execId).getAffectedFiles().add(path);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get recipe history for project: " + projectPath, e);
        } catch (Exception e) {
            log.error("Unexpected error getting history: " + e.getMessage(), e);
        }
        log.info("Found {} history records", historyList.size());
        return historyList;
    }

    private List<String> getAffectedFilePaths(long executionId) {
        List<String> paths = new ArrayList<>();
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn
                    .prepareStatement("SELECT file_path FROM recipe_changed_files WHERE execution_id = ?")) {
                stmt.setLong(1, executionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        paths.add(rs.getString("file_path"));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get affected file paths for execution: " + executionId, e);
        }
        return paths;
    }

    /**
     * Gets the latest execution for a specific recipe in a project.
     */
    public Optional<adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory> getLatestExecutionForRecipe(
            Path projectPath, String recipeName) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    SELECT * FROM recipe_executions WHERE project_path = ? AND recipe_name = ?
                    ORDER BY executed_at DESC LIMIT 1;
                    """)) {
                stmt.setString(1, projectPath.toString());
                stmt.setString(2, recipeName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long id = rs.getLong("id");
                        return Optional.of(
                                adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory.builder()
                                        .id(id)
                                        .recipeName(rs.getString("recipe_name"))
                                        .executedAt(Instant.parse(rs.getString("executed_at")))
                                        .success(rs.getBoolean("success"))
                                        .message(rs.getString("message"))
                                        .undoExecutionId(rs.getObject("undo_execution_id") != null
                                                ? rs.getLong("undo_execution_id")
                                                : null)
                                        .isUndo(rs.getBoolean("is_undo"))
                                        .affectedFiles(getAffectedFilePaths(id))
                                        .build());
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get latest execution for recipe: " + recipeName, e);
        }
        return Optional.empty();
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
            String migrationStatus) {
    }

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
            Instant analysisTime) {
    }

    /**
     * Stored blocker record.
     */
    public record StoredBlocker(
            String type,
            String description,
            String affectedArtifact,
            double confidence) {
    }

    /**
     * Stored recommendation record.
     */
    public record StoredRecommendation(
            String category,
            String priority,
            String description,
            String action,
            String estimatedEffort) {
    }

    /**
     * Migration issue registry record - maps scanner types to UI tabs, namespaces,
     * and refactor recipes.
     */
    public record MigrationIssueRegistry(
            long id,
            String scannerType,
            String uiTabName,
            String legacyNamespace,
            String targetNamespace,
            String refactorRecipe,
            String description,
            String anticipatedErrorMessages,
            String solutionHint,
            boolean isPremium) {
    }
}
