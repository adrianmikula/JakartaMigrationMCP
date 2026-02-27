package adrianmikula.jakartamigration.intellij.mcp;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

public class McpToolRegistryTest extends BasePlatformTestCase {

    public void testGetAllToolsIncludesNewTools() {
        List<McpToolDefinition> tools = McpToolRegistry.getAllTools();

        boolean foundRunAdvancedScan = false;
        boolean foundApplyJakartaRecipe = false;

        for (McpToolDefinition tool : tools) {
            if ("runAdvancedScan".equals(tool.getName())) {
                foundRunAdvancedScan = true;
            } else if ("applyJakartaRecipe".equals(tool.getName())) {
                foundApplyJakartaRecipe = true;
            }
        }

        assertThat(foundRunAdvancedScan).isTrue();
        assertThat(foundApplyJakartaRecipe).isTrue();
    }
}
