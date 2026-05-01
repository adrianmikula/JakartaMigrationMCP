package adrianmikula.jakartamigration.intellij.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test to isolate the formatting issue.
 */
public class DebugFormatTest {
    
    @Test
    void testSimpleFormatting() {
        // Test basic formatting to understand the issue
        String template = "Risk: %s, Score: %.0f, Level: %s, Readiness: %d%%";
        
        String result = template.formatted(
            "low",
            25.5,
            "LOW", 
            75
        );
        
        assertNotNull(result);
        System.out.println("Formatted result: " + result);
    }
    
    @Test
    void testIntegerFormatting() {
        // Test integer formatting specifically
        String template = "Dependencies: %d, Compatible: %d, Issues: %d";
        
        String result = template.formatted(
            0,
            0,
            0
        );
        
        assertNotNull(result);
        System.out.println("Integer formatted result: " + result);
    }
}
