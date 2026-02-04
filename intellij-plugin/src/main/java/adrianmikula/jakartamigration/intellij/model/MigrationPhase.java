package adrianmikula.jakartamigration.intellij.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Migration phase model from TypeSpec: intellij-plugin-ui.tsp
 */
public class MigrationPhase {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private PhaseStatus status;

    @JsonProperty("order")
    private Integer order;

    @JsonProperty("estimatedDuration")
    private Integer estimatedDuration;

    @JsonProperty("prerequisites")
    private List<String> prerequisites;

    @JsonProperty("tasks")
    private List<PhaseTask> tasks;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public PhaseStatus getStatus() { return status; }
    public void setStatus(PhaseStatus status) { this.status = status; }

    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }

    public Integer getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public List<String> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(List<String> prerequisites) { this.prerequisites = prerequisites; }

    public List<PhaseTask> getTasks() { return tasks; }
    public void setTasks(List<PhaseTask> tasks) { this.tasks = tasks; }
}