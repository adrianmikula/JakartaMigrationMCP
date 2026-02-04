package adrianmikula.jakartamigration.intellij.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request for migration impact analysis from TypeSpec: mcp-integration.tsp
 */
public class AnalyzeMigrationImpactRequest {
    @JsonProperty("projectPath")
    private String projectPath;

    @JsonProperty("includeSourceAnalysis")
    private boolean includeSourceAnalysis = true;

    @JsonProperty("analysisDepth")
    private String analysisDepth = "STANDARD";

    public AnalyzeMigrationImpactRequest() {
    }

    public AnalyzeMigrationImpactRequest(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public boolean isIncludeSourceAnalysis() {
        return includeSourceAnalysis;
    }

    public void setIncludeSourceAnalysis(boolean includeSourceAnalysis) {
        this.includeSourceAnalysis = includeSourceAnalysis;
    }

    public String getAnalysisDepth() {
        return analysisDepth;
    }

    public void setAnalysisDepth(String analysisDepth) {
        this.analysisDepth = analysisDepth;
    }
}
