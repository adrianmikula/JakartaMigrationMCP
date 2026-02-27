package adrianmikula.jakartamigration.intellij.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dependency summary model from TypeSpec: intellij-plugin-ui.tsp
 */
public class DependencySummary {
    @JsonProperty("totalDependencies")
    private Integer totalDependencies;

    @JsonProperty("affectedDependencies")
    private Integer affectedDependencies;

    @JsonProperty("blockerDependencies")
    private Integer blockerDependencies;

    @JsonProperty("migrableDependencies")
    private Integer migrableDependencies;

    @JsonProperty("noJakartaSupportCount")
    private Integer noJakartaSupportCount = 0;

    @JsonProperty("xmlFilesCount")
    private Integer xmlFilesCount = 0;

    @JsonProperty("transitiveDependencies")
    private Integer transitiveDependencies = 0;

    // Getters and setters
    public Integer getTotalDependencies() {
        return totalDependencies;
    }

    public void setTotalDependencies(Integer totalDependencies) {
        this.totalDependencies = totalDependencies;
    }

    public Integer getAffectedDependencies() {
        return affectedDependencies;
    }

    public void setAffectedDependencies(Integer affectedDependencies) {
        this.affectedDependencies = affectedDependencies;
    }

    public Integer getBlockerDependencies() {
        return blockerDependencies;
    }

    public void setBlockerDependencies(Integer blockerDependencies) {
        this.blockerDependencies = blockerDependencies;
    }

    public Integer getMigrableDependencies() {
        return migrableDependencies;
    }

    public void setMigrableDependencies(Integer migrableDependencies) {
        this.migrableDependencies = migrableDependencies;
    }

    public Integer getNoJakartaSupportCount() {
        return noJakartaSupportCount;
    }

    public void setNoJakartaSupportCount(Integer noJakartaSupportCount) {
        this.noJakartaSupportCount = noJakartaSupportCount;
    }

    public Integer getXmlFilesCount() {
        return xmlFilesCount;
    }

    public void setXmlFilesCount(Integer xmlFilesCount) {
        this.xmlFilesCount = xmlFilesCount;
    }

    public Integer getTransitiveDependencies() {
        return transitiveDependencies;
    }

    public void setTransitiveDependencies(Integer transitiveDependencies) {
        this.transitiveDependencies = transitiveDependencies;
    }
}