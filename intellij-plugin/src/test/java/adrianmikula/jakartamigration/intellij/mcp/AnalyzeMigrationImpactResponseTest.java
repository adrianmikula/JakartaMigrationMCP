package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AnalyzeMigrationImpactResponse
 */
public class AnalyzeMigrationImpactResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Response should have overall impact")
    public void testResponseHasOverallImpact() {
        AnalyzeMigrationImpactResponse response = new AnalyzeMigrationImpactResponse();
        
        AnalyzeMigrationImpactResponse.ImpactAssessment impact = 
            new AnalyzeMigrationImpactResponse.ImpactAssessment();
        impact.setLevel("HIGH");
        impact.setDescription("Significant migration effort required");
        
        response.setOverallImpact(impact);
        
        assertThat(response.getOverallImpact()).isNotNull();
        assertThat(response.getOverallImpact().getLevel()).isEqualTo("HIGH");
        assertThat(response.getOverallImpact().getDescription()).isEqualTo("Significant migration effort required");
    }

    @Test
    @DisplayName("Response should have dependency impact details")
    public void testResponseHasDependencyImpact() {
        AnalyzeMigrationImpactResponse response = new AnalyzeMigrationImpactResponse();
        
        AnalyzeMigrationImpactResponse.DependencyImpactDetails details = 
            new AnalyzeMigrationImpactResponse.DependencyImpactDetails();
        
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo(
            "org.springframework", "spring-beans", "5.3.27", "6.0.9",
            DependencyMigrationStatus.NEEDS_UPGRADE, false
        ));
        details.setAffectedDependencies(deps);
        
        response.setDependencyImpact(details);
        
        assertThat(response.getDependencyImpact()).isNotNull();
        assertThat(response.getDependencyImpact().getAffectedDependencies()).hasSize(1);
    }

    @Test
    @DisplayName("Response should have effort estimate")
    public void testResponseHasEffortEstimate() {
        AnalyzeMigrationImpactResponse response = new AnalyzeMigrationImpactResponse();
        
        AnalyzeMigrationImpactResponse.EffortEstimate estimate = 
            new AnalyzeMigrationImpactResponse.EffortEstimate();
        estimate.setEstimatedHours(40);
        estimate.setConfidence("HIGH");
        
        response.setEstimatedEffort(estimate);
        
        assertThat(response.getEstimatedEffort()).isNotNull();
        assertThat(response.getEstimatedEffort().getEstimatedHours()).isEqualTo(40);
        assertThat(response.getEstimatedEffort().getConfidence()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Response should serialize to JSON correctly")
    public void testResponseJsonSerialization() throws Exception {
        AnalyzeMigrationImpactResponse response = new AnalyzeMigrationImpactResponse();
        
        AnalyzeMigrationImpactResponse.ImpactAssessment impact = 
            new AnalyzeMigrationImpactResponse.ImpactAssessment();
        impact.setLevel("MEDIUM");
        response.setOverallImpact(impact);
        
        String json = objectMapper.writeValueAsString(response);
        
        assertThat(json).contains("\"overallImpact\"");
        assertThat(json).contains("\"level\":\"MEDIUM\"");
    }

    @Test
    @DisplayName("Response should deserialize from JSON correctly")
    public void testResponseJsonDeserialization() throws Exception {
        String json = """
            {
                "overallImpact": {
                    "level": "HIGH",
                    "description": "Significant changes required"
                },
                "dependencyImpact": {
                    "affectedDependencies": []
                },
                "estimatedEffort": {
                    "estimatedHours": 100,
                    "confidence": "HIGH"
                }
            }
            """;
        
        AnalyzeMigrationImpactResponse response = 
            objectMapper.readValue(json, AnalyzeMigrationImpactResponse.class);
        
        assertThat(response.getOverallImpact()).isNotNull();
        assertThat(response.getOverallImpact().getLevel()).isEqualTo("HIGH");
        assertThat(response.getDependencyImpact()).isNotNull();
        assertThat(response.getEstimatedEffort()).isNotNull();
        assertThat(response.getEstimatedEffort().getEstimatedHours()).isEqualTo(100);
    }

    @Test
    @DisplayName("ImpactAssessment should store level and description")
    public void testImpactAssessmentFields() {
        AnalyzeMigrationImpactResponse.ImpactAssessment impact = 
            new AnalyzeMigrationImpactResponse.ImpactAssessment();
        
        impact.setLevel("CRITICAL");
        impact.setDescription("Critical migration issues found");
        impact.setRiskFactors(List.of("Breaking API changes", "Database schema changes"));
        
        assertThat(impact.getLevel()).isEqualTo("CRITICAL");
        assertThat(impact.getDescription()).isEqualTo("Critical migration issues found");
        assertThat(impact.getRiskFactors()).hasSize(2);
    }

    @Test
    @DisplayName("EffortEstimate should store estimatedHours and confidence")
    public void testEffortEstimateFields() {
        AnalyzeMigrationImpactResponse.EffortEstimate estimate = 
            new AnalyzeMigrationImpactResponse.EffortEstimate();
        
        estimate.setEstimatedHours(80);
        estimate.setConfidence("MEDIUM");
        
        assertThat(estimate.getEstimatedHours()).isEqualTo(80);
        assertThat(estimate.getConfidence()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("DependencyImpactDetails should handle null dependencies")
    public void testDependencyImpactDetailsNull() {
        AnalyzeMigrationImpactResponse.DependencyImpactDetails details = 
            new AnalyzeMigrationImpactResponse.DependencyImpactDetails();
        
        // Should not throw when dependencies is null
        assertThat(details.getAffectedDependencies()).isNull();
    }

    @Test
    @DisplayName("DependencyImpactDetails should store transitive changes")
    public void testDependencyImpactDetailsTransitiveChanges() {
        AnalyzeMigrationImpactResponse.DependencyImpactDetails details = 
            new AnalyzeMigrationImpactResponse.DependencyImpactDetails();
        
        details.setTransitiveDependencyChanges(15);
        
        assertThat(details.getTransitiveDependencyChanges()).isEqualTo(15);
    }

    @Test
    @DisplayName("DependencyImpactDetails should store breaking changes")
    public void testDependencyImpactDetailsBreakingChanges() {
        AnalyzeMigrationImpactResponse.DependencyImpactDetails details = 
            new AnalyzeMigrationImpactResponse.DependencyImpactDetails();
        
        details.setBreakingChanges(List.of("javax.xml.bind removed", "javax.activation renamed"));
        
        assertThat(details.getBreakingChanges()).hasSize(2);
    }
}
