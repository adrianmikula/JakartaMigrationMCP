package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.intellij.service.RiskScoringService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple test to verify risk scoring service handles all scan types
 */
public class RiskScoringTest {
    
    public static void main(String[] args) {
        System.out.println("Testing RiskScoringService...");
        
        try {
            RiskScoringService service = RiskScoringService.getInstance();
            
            // Test all scan types that DashboardComponent uses
            Map<String, List<RiskScoringService.RiskFinding>> scanFindings = new HashMap<>();
            
            // Add empty findings lists for all scan types
            scanFindings.put("jpa", List.of());
            scanFindings.put("beanValidation", List.of());
            scanFindings.put("servletJsp", List.of());
            scanFindings.put("cdiInjection", List.of());
            scanFindings.put("buildConfig", List.of());
            scanFindings.put("restSoap", List.of());
            scanFindings.put("deprecatedApi", List.of());
            scanFindings.put("securityApi", List.of());
            scanFindings.put("jmsMessaging", List.of());
            scanFindings.put("configFiles", List.of());
            
            // Test dependency issues
            Map<String, Integer> dependencyIssues = new HashMap<>();
            dependencyIssues.put("test", 1);
            
            // This should not throw IllegalArgumentException
            RiskScoringService.RiskScore result = service.calculateRiskScore(
                scanFindings, 
                dependencyIssues
            );
            
            System.out.println("✅ SUCCESS: All scan types have valid risk configurations!");
            System.out.println("Risk score: " + result.totalScore());
            System.out.println("Risk level: " + result.category());
            
        } catch (Exception e) {
            System.err.println("❌ FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
