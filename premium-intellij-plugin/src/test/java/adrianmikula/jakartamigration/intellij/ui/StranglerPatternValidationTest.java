package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
import adrianmikula.jakartamigration.intellij.ui.UiTextLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to validate that Strangler Pattern phase descriptions meet 2026 industry standards.
 * This test ensures the newly added strangler pattern phases are comprehensive and include modern concepts.
 */
public class StranglerPatternValidationTest {

    @Test
    @DisplayName("Strangler Pattern phases should meet 2026 industry standards")
    public void testStranglerPatternStandardsCompliance() {
        // Test all 4 strangler pattern phases
        for (int phaseIndex = 1; phaseIndex <= 4; phaseIndex++) {
            String description = UiTextLoader.getPhaseDescription("strangler", phaseIndex);
            String title = UiTextLoader.getPhaseTitle("strangler", phaseIndex);
            
            // Verify phase content exists
            assertThat(description).isNotEmpty();
            assertThat(title).isNotEmpty();
            
            // Validate with PhaseContentValidator
            boolean meets2026Standards = PhaseContentValidator.contains2026Standards(description);
            boolean meetsLengthRequirements = PhaseContentValidator.meetsLengthRequirements(description);
            boolean validatesContent = PhaseContentValidator.validatesPhaseContent(MigrationStrategy.STRANGLER, phaseIndex - 1, description);
            
            // Print validation report for debugging
            String report = PhaseContentValidator.generateValidationReport(MigrationStrategy.STRANGLER, phaseIndex - 1, description);
            System.out.println("=== Strangler Pattern Phase " + phaseIndex + " Validation ===");
            System.out.println("Title: " + title);
            System.out.println("Description Length: " + description.length() + " characters");
            System.out.println("Word Count: " + description.split("\\s+").length);
            System.out.println(report);
            System.out.println();
            
            // Assertions for compliance
            assertThat(meets2026Standards).as("Phase " + phaseIndex + " should contain 2026 industry standards").isTrue();
            assertThat(meetsLengthRequirements).as("Phase " + phaseIndex + " should meet length requirements").isTrue();
            assertThat(validatesContent).as("Phase " + phaseIndex + " should validate content for strangler pattern").isTrue();
        }
    }

    @Test
    @DisplayName("Strangler Pattern phase steps should be comprehensive")
    public void testStranglerPatternPhaseSteps() {
        // Test that all phases have comprehensive step lists
        for (int phaseIndex = 1; phaseIndex <= 4; phaseIndex++) {
            String[] steps = UiTextLoader.getPhaseStepsArray("strangler", phaseIndex);
            
            assertThat(steps).isNotEmpty();
            assertThat(steps.length).as("Phase " + phaseIndex + " should have at least 3 steps").isGreaterThanOrEqualTo(3);
            
            // Validate each step is substantial
            for (int stepIndex = 0; stepIndex < steps.length; stepIndex++) {
                String step = steps[stepIndex];
                assertThat(step).as("Step " + (stepIndex + 1) + " of phase " + phaseIndex + " should not be empty").isNotEmpty();
                assertThat(step.length()).as("Step " + (stepIndex + 1) + " of phase " + phaseIndex + " should be substantial").isGreaterThan(10);
            }
        }
    }

    @Test
    @DisplayName("Strangler Pattern phases should include modern Jakarta EE concepts")
    public void testStranglerPatternJakartaEEConcepts() {
        String[] requiredConcepts = {
            "Jakarta EE 10", "Core Profile", "microservices", "service mesh", 
            "Istio", "OpenTelemetry", "Eclipse Transformer", "OpenRewrite",
            "Kubernetes", "monitoring", "security", "performance"
        };
        
        for (int phaseIndex = 1; phaseIndex <= 4; phaseIndex++) {
            String description = UiTextLoader.getPhaseDescription("strangler", phaseIndex);
            String title = UiTextLoader.getPhaseTitle("strangler", phaseIndex);
            
            // Check that each phase includes several modern concepts
            int conceptCount = 0;
            for (String concept : requiredConcepts) {
                if (description.toLowerCase().contains(concept.toLowerCase())) {
                    conceptCount++;
                }
            }
            
            // Each phase should include at least 5 modern concepts
            assertThat(conceptCount).as("Phase " + phaseIndex + " (" + title + ") should include modern Jakarta EE concepts").isGreaterThanOrEqualTo(5);
        }
    }
}
