import "@typespec/http";
import "@typespec/rest";
import "@typespec/openapi3";

using TypeSpec.Http;
using TypeSpec.Rest;

@service({
  title: "Jakarta Migration IntelliJ Plugin UI",
  version: "1.0.0",
})
@doc("TypeSpec specification for Jakarta Migration IntelliJ Plugin UI components and data models")
namespace JakartaMigrationIntellijPlugin;

@doc("Main dashboard component that displays migration overview")
model MigrationDashboard {
  @doc("Project readiness score from 0-100")
  readinessScore: int32;
  
  @doc("Overall migration status")
  status: MigrationStatus;
  
  @doc("Summary of affected dependencies")
  dependencySummary: DependencySummary;
  
  @doc("Current migration phase")
  currentPhase: MigrationPhase;
  
  @doc("Last analysis timestamp")
  lastAnalyzed: utcDateTime;
}

@doc("Migration status enumeration")
enum MigrationStatus {
  @doc("Not analyzed yet")
  NotAnalyzed: "NOT_ANALYZED",
  
  @doc("Ready for migration")
  Ready: "READY",
  
  @doc("Has blockers that need resolution")
  HasBlockers: "HAS_BLOCKERS",
  
  @doc("Migration in progress")
  InProgress: "IN_PROGRESS",
  
  @doc("Migration completed")
  Completed: "COMPLETED",
  
  @doc("Migration failed")
  Failed: "FAILED"
}

@doc("Summary of dependency analysis")
model DependencySummary {
  @doc("Total number of dependencies")
  totalDependencies: int32;
  
  @doc("Number of dependencies that need migration")
  affectedDependencies: int32;
  
  @doc("Number of blocking dependencies")
  blockerDependencies: int32;
  
  @doc("Number of dependencies with available Jakarta versions")
  migrableDependencies: int32;
}

@doc("Individual dependency information for the dependencies table")
model DependencyInfo {
  @doc("Dependency group ID")
  groupId: string;
  
  @doc("Dependency artifact ID")
  artifactId: string;
  
  @doc("Current version")
  currentVersion: string;
  
  @doc("Recommended Jakarta version (if available)")
  recommendedVersion?: string;
  
  @doc("Migration status of this dependency")
  migrationStatus: DependencyMigrationStatus;
  
  @doc("Whether this dependency blocks migration")
  isBlocker: boolean;
  
  @doc("Risk level for migrating this dependency")
  riskLevel: RiskLevel;
  
  @doc("Description of migration impact")
  migrationImpact?: string;
}

@doc("Migration status for individual dependencies")
enum DependencyMigrationStatus {
  @doc("Compatible with Jakarta")
  Compatible: "COMPATIBLE",
  
  @doc("Needs version upgrade")
  NeedsUpgrade: "NEEDS_UPGRADE",
  
  @doc("No Jakarta version available")
  NoJakartaVersion: "NO_JAKARTA_VERSION",
  
  @doc("Requires manual migration")
  RequiresManualMigration: "REQUIRES_MANUAL_MIGRATION",
  
  @doc("Already migrated")
  Migrated: "MIGRATED"
}

@doc("Risk level enumeration")
enum RiskLevel {
  @doc("Low risk migration")
  Low: "LOW",
  
  @doc("Medium risk migration")
  Medium: "MEDIUM",
  
  @doc("High risk migration")
  High: "HIGH",
  
  @doc("Critical risk migration")
  Critical: "CRITICAL"
}

@doc("Migration phase information")
model MigrationPhase {
  @doc("Phase identifier")
  id: string;
  
  @doc("Phase name")
  name: string;
  
  @doc("Phase description")
  description: string;
  
  @doc("Phase status")
  status: PhaseStatus;
  
  @doc("Phase order/sequence")
  order: int32;
  
  @doc("Estimated duration in hours")
  estimatedDuration?: int32;
  
  @doc("Dependencies that must be completed before this phase")
  prerequisites: string[];
  
  @doc("Tasks within this phase")
  tasks: PhaseTask[];
}

@doc("Status of a migration phase")
enum PhaseStatus {
  @doc("Phase not started")
  NotStarted: "NOT_STARTED",
  
  @doc("Phase in progress")
  InProgress: "IN_PROGRESS",
  
  @doc("Phase completed successfully")
  Completed: "COMPLETED",
  
  @doc("Phase failed")
  Failed: "FAILED",
  
  @doc("Phase skipped")
  Skipped: "SKIPPED"
}

@doc("Individual task within a migration phase")
model PhaseTask {
  @doc("Task identifier")
  id: string;
  
  @doc("Task name")
  name: string;
  
  @doc("Task description")
  description?: string;
  
  @doc("Task status")
  status: TaskStatus;
  
  @doc("Task type")
  type: TaskType;
  
  @doc("Whether this task can be automated")
  isAutomatable: boolean;
}

@doc("Status of an individual task")
enum TaskStatus {
  @doc("Task pending")
  Pending: "PENDING",
  
  @doc("Task in progress")
  InProgress: "IN_PROGRESS",
  
  @doc("Task completed")
  Completed: "COMPLETED",
  
  @doc("Task failed")
  Failed: "FAILED"
}

@doc("Type of migration task")
enum TaskType {
  @doc("Dependency version update")
  DependencyUpdate: "DEPENDENCY_UPDATE",
  
  @doc("Source code transformation")
  SourceTransformation: "SOURCE_TRANSFORMATION",
  
  @doc("Configuration file update")
  ConfigurationUpdate: "CONFIGURATION_UPDATE",
  
  @doc("Manual verification")
  ManualVerification: "MANUAL_VERIFICATION",
  
  @doc("Testing")
  Testing: "TESTING"
}

@doc("Module dependency graph node")
model ModuleDependencyNode {
  @doc("Module identifier")
  moduleId: string;
  
  @doc("Module name")
  moduleName: string;
  
  @doc("Migration status of this module")
  migrationStatus: ModuleMigrationStatus;
  
  @doc("Dependencies on other modules")
  dependencies: string[];
  
  @doc("Modules that depend on this module")
  dependents: string[];
  
  @doc("Risk level for migrating this module")
  riskLevel: RiskLevel;
}

@doc("Migration status for modules")
enum ModuleMigrationStatus {
  @doc("Module not analyzed")
  NotAnalyzed: "NOT_ANALYZED",
  
  @doc("Module ready for migration")
  Ready: "READY",
  
  @doc("Module has blockers")
  HasBlockers: "HAS_BLOCKERS",
  
  @doc("Module migration in progress")
  InProgress: "IN_PROGRESS",
  
  @doc("Module migrated successfully")
  Migrated: "MIGRATED"
}

@doc("Complete dependency graph for visualization")
model DependencyGraph {
  @doc("All modules in the project")
  modules: ModuleDependencyNode[];
  
  @doc("Suggested migration order")
  migrationOrder: string[];
  
  @doc("Critical path modules")
  criticalPath: string[];
}