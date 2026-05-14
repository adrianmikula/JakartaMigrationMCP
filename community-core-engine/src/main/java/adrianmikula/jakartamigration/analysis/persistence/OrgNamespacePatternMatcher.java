package adrianmikula.jakartamigration.analysis.persistence;

import java.util.HashSet;
import java.util.Set;

/**
 * Matches groupIds against organization namespace patterns.
 * Extracted from CentralMigrationAnalysisStore to reduce class size.
 */
public class OrgNamespacePatternMatcher {
    
    private final Set<String> orgNamespacePatterns = new HashSet<>();
    
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
}
