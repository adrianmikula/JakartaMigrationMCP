package adrianmikula.jakartamigration.analysis.persistence;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Populates the migration_issues_registry table with default scanner types.
 * Extracted from SqliteMigrationAnalysisStore to reduce class size.
 */
@Slf4j
public class MigrationIssuesRegistryPopulator {
    
    private final ScannerTypeRegistry scannerTypeRegistry;
    
    public MigrationIssuesRegistryPopulator() {
        this.scannerTypeRegistry = new ScannerTypeRegistry();
    }
    
    /**
     * Populates migration_issues_registry table with default scanner types.
     */
    public void populateRegistry(Connection conn) throws SQLException {
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
            // Get all scanner types from the registry
            var scanners = scannerTypeRegistry.getAllScanners();
            
            String insertSql = """
                    INSERT OR REPLACE INTO migration_issues_registry
                    (scanner_type, ui_tab_name, legacy_namespace, target_namespace, refactor_recipe, description, anticipated_error_messages, solution_hint, is_premium)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (var entry : scanners.entrySet()) {
                    String scannerType = entry.getKey();
                    ScannerTypeMetadata metadata = entry.getValue();
                    
                    pstmt.setString(1, scannerType);
                    pstmt.setString(2, metadata.uiTab());
                    pstmt.setString(3, metadata.legacyNamespace());
                    pstmt.setString(4, metadata.targetNamespace());
                    pstmt.setString(5, metadata.refactorRecipe());
                    pstmt.setString(6, metadata.description());
                    pstmt.setString(7, metadata.anticipatedErrorMessages());
                    pstmt.setString(8, metadata.solutionHint());
                    pstmt.setBoolean(9, metadata.isPremium());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            conn.commit();
            log.info("Successfully populated {} scanner types in migration issues registry", scanners.size());
            
        } catch (Exception e) {
            log.error("Failed to populate migration issues registry", e);
            conn.rollback();
            throw e;
        }
    }
}
