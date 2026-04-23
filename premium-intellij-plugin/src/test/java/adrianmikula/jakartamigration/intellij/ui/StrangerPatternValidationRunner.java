package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
import adrianmikula.jakartamigration.intellij.ui.UiTextLoader;

/**
 * Simple validation runner to test strangler pattern phase descriptions
 * without requiring full test compilation.
 */
public class StrangerPatternValidationRunner {
    
    public static void main(String[] args) {
        System.out.println("=== Strangler Pattern Phase Validation ===\n");
        
        // Test all 4 strangler pattern phases
        for (int phaseIndex = 1; phaseIndex <= 4; phaseIndex++) {
            String description = UiTextLoader.getPhaseDescription("strangler", phaseIndex);
            String title = UiTextLoader.getPhaseTitle("strangler", phaseIndex);
            String[] steps = UiTextLoader.getPhaseStepsArray("strangler", phaseIndex);
            
            System.out.println("Phase " + phaseIndex + ": " + title);
            System.out.println("Description Length: " + description.length() + " characters");
            System.out.println("Word Count: " + description.split("\\s+").length);
            System.out.println("Step Count: " + steps.length);
            
            // Validate with PhaseContentValidator
            boolean meets2026Standards = PhaseContentValidator.contains2026Standards(description);
            boolean meetsLengthRequirements = PhaseContentValidator.meetsLengthRequirements(description);
            boolean validatesContent = PhaseContentValidator.validatesPhaseContent(MigrationStrategy.STRANGLER, phaseIndex - 1, description);
            boolean validatesSteps = PhaseContentValidator.validatesPhaseSteps(steps);
            
            System.out.println("2026 Standards Compliance: " + (meets2026Standards ? "PASS" : "FAIL"));
            System.out.println("Length Requirements: " + (meetsLengthRequirements ? "PASS" : "FAIL"));
            System.out.println("Content Validation: " + (validatesContent ? "PASS" : "FAIL"));
            System.out.println("Step Validation: " + (validatesSteps ? "PASS" : "FAIL"));
            
            // Generate detailed report
            String report = PhaseContentValidator.generateValidationReport(MigrationStrategy.STRANGLER, phaseIndex - 1, description);
            System.out.println(report);
            
            System.out.println("\n" + "=".repeat(80) + "\n");
        }
    }
}
