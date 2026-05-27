package adrianmikula.jakartamigration.pdfreporting.service.impl;

import java.util.Arrays;
import java.util.List;

/**
 * Provider for migration strategy data and HTML table generation.
 * Contains the 6 migration strategies with their details.
 */
public class MigrationStrategyProvider {
    
    private final HtmlUtils htmlUtils;
    
    public MigrationStrategyProvider() {
        this.htmlUtils = new HtmlUtils();
    }
    
    /**
     * Migration strategy data for PDF reports.
     */
    public record MigrationStrategyData(
        String name,
        String description,
        String benefits,
        String risks,
        String phases,
        String color,
        String useCase
    ) {}
    
    /**
     * Get all available migration strategies.
     */
    public List<MigrationStrategyData> getMigrationStrategies() {
        return Arrays.asList(
            new MigrationStrategyData(
                "Big Bang",
                "Migrate everything at once",
                "- Migrate all dependencies at once\n- Single comprehensive change\n- Best for small projects",
                "- Higher risk - issues affect entire codebase\n- Longer rollback time\n- Requires comprehensive testing",
                "1. Dependency Upgrade: Update all build files\n2. Code Refactor: Replace all javax.* imports\n3. XML/Config Update: Update configuration files\n4. Global Testing: Comprehensive testing",
                "#dc3545",
                "Small projects with low complexity and comprehensive testing capabilities"
            ),
            new MigrationStrategyData(
                "Incremental",
                "One dependency at a time",
                "- Migrate dependencies incrementally\n- Update one dependency, test, proceed\n- Lower risk per change\n- Best for large projects",
                "- Longer migration timeline\n- Must maintain compatibility\n- May require dual dependencies",
                "1. Dependency Scan: Identify javax dependencies\n2. Priority Ranking: Order by risk level\n3. Step-by-Step Upgrade: One artifact at a time\n4. Continuous Testing: Test after each change",
                "#ffc107",
                "Large projects with complex dependencies and need for risk mitigation"
            ),
            new MigrationStrategyData(
                "Transform",
                "Combined build and runtime transformation",
                "- Combine build and runtime approaches\n- Use OpenRewrite for automated changes\n- Deploy runtime adapters for edge cases",
                "- Most complex implementation\n- Requires build and runtime config\n- Higher resource overhead",
                "1. Recipe Selection: Choose Rewrite recipes\n2. Batch Execution: Run transformation across codebase\n3. Diff Review: Manual inspection of changes\n4. Final Validation: Automated test verification",
                "#17a2b8",
                "Projects requiring automation and mixed build/runtime approaches"
            ),
            new MigrationStrategyData(
                "Microservices",
                "Migrate each service independently",
                "- Migrate services one at a time\n- Each service can use different strategy\n- Independent deployment and testing",
                "- Requires coordination across services\n- Handle inter-service dependencies\n- May need service mesh updates",
                "1. Service Inventory: Map microservices and dependencies\n2. Dependency Analysis: Identify shared libraries\n3. Migration Planning: Order by dependency complexity\n4. Incremental Rollout: Deploy migrated services with legacy",
                "#6c757d",
                "Microservices architectures with independent service deployment"
            ),
            new MigrationStrategyData(
                "Adapter Pattern",
                "Use adapter classes for javax/jakarta compatibility",
                "- Maintain backward compatibility\n- Gradual replacement of javax with jakarta\n- Lower risk changes\n- Easy to rollback adapters",
                "- Additional code maintenance\n- Runtime overhead for adapters\n- More complex classpath management",
                "1. Adapter Config: Setup runtime bytecode instrumentation\n2. Runtime Proxy: Intercept javax calls, redirect to jakarta\n3. Legacy Support: Link old libraries to new EE runtime\n4. Monitor: Monitor performance and errors",
                "#6f42c1",
                "Projects requiring backward compatibility and gradual migration"
            ),
            new MigrationStrategyData(
                "Strangler",
                "Migrate module by module",
                "- Migrate one module at a time\n- New features built in Jakarta EE\n- Existing features gradually migrated\n- Good for monolithic applications",
                "- Requires inter-module compatibility layers\n- Can create duplicate logic during transition\n- Managing two EE environments simultaneously",
                "1. Interface Definition: Define module boundaries\n2. Bridge Setup: Create compatibility layer for cross-module calls\n3. Vertical Slices: Migrate one functional slice at a time\n4. Decommission: Remove legacy modules once replaced",
                "#28a745",
                "Monolithic applications with clear module boundaries"
            )
        );
    }
    
    /**
     * Generate HTML table for migration strategy comparison.
     */
    public String generateMigrationStrategyTableHtml() {
        List<MigrationStrategyData> strategies = getMigrationStrategies();
        
        StringBuilder tableHtml = new StringBuilder();
        tableHtml.append("""
            <div class="strategy-table-container">
                <table class="strategy-comparison-table">
                    <thead>
                        <tr>
                            <th>Strategy</th>
                            <th>Description</th>
                            <th>Benefits</th>
                            <th>Risks</th>
                            <th>Implementation Phases</th>
                            <th>Best For</th>
                        </tr>
                    </thead>
                    <tbody>
            """);
        
        for (MigrationStrategyData strategy : strategies) {
            tableHtml.append(String.format("""
                <tr class="strategy-row">
                    <td class="strategy-name">
                        <div class="strategy-indicator" style="background-color: %s;"></div>
                        <strong>%s</strong>
                    </td>
                    <td class="strategy-description">%s</td>
                    <td class="strategy-benefits">%s</td>
                    <td class="strategy-risks">%s</td>
                    <td class="strategy-phases">%s</td>
                    <td class="strategy-use-case">%s</td>
                </tr>
            """, 
                strategy.color(),
                this.htmlUtils.escapeHtml(strategy.name()),
                this.htmlUtils.escapeHtml(strategy.description()),
                this.htmlUtils.escapeHtml(strategy.benefits()).replace("\n", "<br>"),
                this.htmlUtils.escapeHtml(strategy.risks()).replace("\n", "<br>"),
                this.htmlUtils.escapeHtml(strategy.phases()).replace("\n", "<br>"),
                this.htmlUtils.escapeHtml(strategy.useCase())
            ));
        }
        
        tableHtml.append("""
                    </tbody>
                </table>
            </div>
            """);
        
        return tableHtml.toString();
    }
}
