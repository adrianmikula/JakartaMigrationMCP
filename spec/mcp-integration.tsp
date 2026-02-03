import "@typespec/http";
import "@typespec/rest";
import "./intellij-plugin-ui.tsp";

using TypeSpec.Http;
using TypeSpec.Rest;

@service({
  title: "Jakarta Migration MCP Integration",
  version: "1.0.0",
})
@doc("TypeSpec specification for MCP server integration with IntelliJ plugin")
namespace JakartaMigrationMcpIntegration;

@doc("MCP client service interface for communicating with the Jakarta Migration MCP server")
interface McpClientService {
  @doc("Analyze Jakarta readiness of a project")
  @post
  analyzeJakartaReadiness(@body request: AnalyzeJakartaReadinessRequest): AnalyzeJakartaReadinessResponse;
  
  @doc("Detect migration blockers in a project")
  @post
  detectBlockers(@body request: DetectBlockersRequest): DetectBlockersResponse;
  
  @doc("Get version recommendations for dependencies")
  @post
  recommendVersions(@body request: RecommendVersionsRequest): RecommendVersionsResponse;
  
  @doc("Analyze comprehensive migration impact")
  @post
  analyzeMigrationImpact(@body request: AnalyzeMigrationImpactRequest): AnalyzeMigrationImpactResponse;
  
  @doc("Create a migration plan")
  @post
  createMigrationPlan(@body request: CreateMigrationPlanRequest): CreateMigrationPlanResponse;
  
  @doc("Execute a migration plan")
  @post
  executeMigrationPlan(@body request: ExecuteMigrationPlanRequest): ExecuteMigrationPlanResponse;
  
  @doc("Apply automatic fixes")
  @post
  applyAutoFixes(@body request: ApplyAutoFixesRequest): ApplyAutoFixesResponse;
}

@doc("Request to analyze Jakarta readiness")
model AnalyzeJakartaReadinessRequest {
  @doc("Path to the project directory")
  projectPath: string;
  
  @doc("Additional analysis options")
  options?: AnalysisOptions;
}

@doc("Response from Jakarta readiness analysis")
model AnalyzeJakartaReadinessResponse {
  @doc("Readiness score from 0-100")
  readinessScore: int32;
  
  @doc("Overall assessment")
  assessment: string;
  
  @doc("Detailed findings")
  findings: ReadinessFinding[];
  
  @doc("Recommendations")
  recommendations: string[];
}

@doc("Individual readiness finding")
model ReadinessFinding {
  @doc("Finding category")
  category: string;
  
  @doc("Finding description")
  description: string;
  
  @doc("Severity level")
  severity: JakartaMigrationIntellijPlugin.RiskLevel;
  
  @doc("Affected files or components")
  affectedItems: string[];
}

@doc("Request to detect migration blockers")
model DetectBlockersRequest {
  @doc("Path to the project directory")
  projectPath: string;
  
  @doc("Analysis options")
  options?: AnalysisOptions;
}

@doc("Response from blocker detection")
model DetectBlockersResponse {
  @doc("List of blocking dependencies")
  blockers: BlockerInfo[];
  
  @doc("Total number of blockers found")
  totalBlockers: int32;
  
  @doc("Blocker summary by category")
  summary: BlockerSummary;
}

@doc("Information about a migration blocker")
model BlockerInfo {
  @doc("Blocker type")
  type: BlockerType;
  
  @doc("Blocker description")
  description: string;
  
  @doc("Affected dependency or component")
  affectedComponent: string;
  
  @doc("Suggested resolution")
  resolution?: string;
  
  @doc("Severity of the blocker")
  severity: JakartaMigrationIntellijPlugin.RiskLevel;
}

@doc("Types of migration blockers")
enum BlockerType {
  @doc("Dependency has no Jakarta equivalent")
  NoJakartaEquivalent: "NO_JAKARTA_EQUIVALENT",
  
  @doc("Transitive dependency conflict")
  TransitiveDependencyConflict: "TRANSITIVE_DEPENDENCY_CONFLICT",
  
  @doc("Binary incompatibility")
  BinaryIncompatibility: "BINARY_INCOMPATIBILITY",
  
  @doc("Custom code using javax APIs")
  CustomCodeUsage: "CUSTOM_CODE_USAGE",
  
  @doc("Configuration file references")
  ConfigurationReferences: "CONFIGURATION_REFERENCES"
}

@doc("Summary of blockers by category")
model BlockerSummary {
  @doc("Number of dependency blockers")
  dependencyBlockers: int32;
  
  @doc("Number of code blockers")
  codeBlockers: int32;
  
  @doc("Number of configuration blockers")
  configurationBlockers: int32;
}

@doc("Request for version recommendations")
model RecommendVersionsRequest {
  @doc("Path to the project directory")
  projectPath: string;
  
  @doc("Specific dependencies to analyze (optional)")
  dependencies?: string[];
}

@doc("Response with version recommendations")
model RecommendVersionsResponse {
  @doc("List of dependency recommendations")
  recommendations: DependencyRecommendation[];
  
  @doc("Summary of recommendations")
  summary: RecommendationSummary;
}

@doc("Recommendation for a specific dependency")
model DependencyRecommendation {
  @doc("Dependency group ID")
  groupId: string;
  
  @doc("Dependency artifact ID")
  artifactId: string;
  
  @doc("Current version")
  currentVersion: string;
  
  @doc("Recommended Jakarta version")
  recommendedVersion: string;
  
  @doc("Migration notes")
  migrationNotes?: string;
  
  @doc("Confidence level in the recommendation")
  confidence: ConfidenceLevel;
}

@doc("Confidence level in recommendations")
enum ConfidenceLevel {
  @doc("Low confidence")
  Low: "LOW",
  
  @doc("Medium confidence")
  Medium: "MEDIUM",
  
  @doc("High confidence")
  High: "HIGH"
}

@doc("Summary of version recommendations")
model RecommendationSummary {
  @doc("Total dependencies analyzed")
  totalDependencies: int32;
  
  @doc("Dependencies with recommendations")
  dependenciesWithRecommendations: int32;
  
  @doc("Dependencies without Jakarta versions")
  dependenciesWithoutJakartaVersions: int32;
}

@doc("Request for migration impact analysis")
model AnalyzeMigrationImpactRequest {
  @doc("Path to the project directory")
  projectPath: string;
  
  @doc("Include source code analysis")
  includeSourceAnalysis?: boolean = true;
  
  @doc("Analysis depth")
  analysisDepth?: AnalysisDepth = AnalysisDepth.Standard;
}

@doc("Analysis depth options")
enum AnalysisDepth {
  @doc("Quick analysis")
  Quick: "QUICK",
  
  @doc("Standard analysis")
  Standard: "STANDARD",
  
  @doc("Deep analysis")
  Deep: "DEEP"
}

@doc("Response from migration impact analysis")
model AnalyzeMigrationImpactResponse {
  @doc("Overall impact assessment")
  overallImpact: ImpactAssessment;
  
  @doc("Dependency impact details")
  dependencyImpact: DependencyImpactDetails;
  
  @doc("Source code impact details")
  sourceCodeImpact?: SourceCodeImpactDetails;
  
  @doc("Estimated migration effort")
  estimatedEffort: EffortEstimate;
}

@doc("Overall impact assessment")
model ImpactAssessment {
  @doc("Impact level")
  level: ImpactLevel;
  
  @doc("Impact description")
  description: string;
  
  @doc("Key risk factors")
  riskFactors: string[];
}

@doc("Impact level enumeration")
enum ImpactLevel {
  @doc("Minimal impact")
  Minimal: "MINIMAL",
  
  @doc("Low impact")
  Low: "LOW",
  
  @doc("Medium impact")
  Medium: "MEDIUM",
  
  @doc("High impact")
  High: "HIGH",
  
  @doc("Critical impact")
  Critical: "CRITICAL"
}

@doc("Dependency impact details")
model DependencyImpactDetails {
  @doc("Affected dependencies")
  affectedDependencies: JakartaMigrationIntellijPlugin.DependencyInfo[];
  
  @doc("Transitive dependency changes")
  transitiveDependencyChanges: int32;
  
  @doc("Breaking changes expected")
  breakingChanges: string[];
}

@doc("Source code impact details")
model SourceCodeImpactDetails {
  @doc("Files that need modification")
  filesToModify: string[];
  
  @doc("Estimated lines of code to change")
  estimatedLinesOfCodeToChange: int32;
  
  @doc("Import statement changes needed")
  importChanges: int32;
  
  @doc("Annotation changes needed")
  annotationChanges: int32;
}

@doc("Effort estimate for migration")
model EffortEstimate {
  @doc("Estimated hours for migration")
  estimatedHours: int32;
  
  @doc("Confidence in the estimate")
  confidence: ConfidenceLevel;
  
  @doc("Breakdown by activity")
  breakdown: EffortBreakdown;
}

@doc("Breakdown of effort by activity")
model EffortBreakdown {
  @doc("Hours for dependency updates")
  dependencyUpdates: int32;
  
  @doc("Hours for source code changes")
  sourceCodeChanges: int32;
  
  @doc("Hours for testing")
  testing: int32;
  
  @doc("Hours for manual verification")
  manualVerification: int32;
}

@doc("Request to create migration plan")
model CreateMigrationPlanRequest {
  @doc("Path to the project directory")
  projectPath: string;
  
  @doc("Migration strategy preferences")
  strategy?: MigrationStrategy;
}

@doc("Migration strategy options")
model MigrationStrategy {
  @doc("Preferred approach")
  approach: MigrationApproach;
  
  @doc("Risk tolerance")
  riskTolerance: JakartaMigrationIntellijPlugin.RiskLevel;
  
  @doc("Timeline constraints")
  timelineConstraints?: TimelineConstraints;
}

@doc("Migration approach options")
enum MigrationApproach {
  @doc("Migrate all at once")
  BigBang: "BIG_BANG",
  
  @doc("Migrate incrementally by module")
  Incremental: "INCREMENTAL",
  
  @doc("Migrate dependencies first, then code")
  DependenciesFirst: "DEPENDENCIES_FIRST"
}

@doc("Timeline constraints for migration")
model TimelineConstraints {
  @doc("Maximum duration in days")
  maxDurationDays?: int32;
  
  @doc("Preferred completion date")
  preferredCompletionDate?: plainDate;
  
  @doc("Available resources (team size)")
  availableResources?: int32;
}

@doc("Response with migration plan")
model CreateMigrationPlanResponse {
  @doc("Generated migration plan")
  migrationPlan: MigrationPlan;
  
  @doc("Plan metadata")
  planMetadata: PlanMetadata;
}

@doc("Complete migration plan")
model MigrationPlan {
  @doc("Plan identifier")
  planId: string;
  
  @doc("Migration phases")
  phases: JakartaMigrationIntellijPlugin.MigrationPhase[];
  
  @doc("Overall timeline")
  timeline: PlanTimeline;
  
  @doc("Risk assessment")
  riskAssessment: RiskAssessment;
}

@doc("Plan timeline information")
model PlanTimeline {
  @doc("Estimated start date")
  estimatedStartDate: plainDate;
  
  @doc("Estimated completion date")
  estimatedCompletionDate: plainDate;
  
  @doc("Total estimated duration in days")
  totalDurationDays: int32;
}

@doc("Risk assessment for the migration plan")
model RiskAssessment {
  @doc("Overall risk level")
  overallRisk: JakartaMigrationIntellijPlugin.RiskLevel;
  
  @doc("Identified risks")
  risks: IdentifiedRisk[];
  
  @doc("Mitigation strategies")
  mitigationStrategies: string[];
}

@doc("Individual identified risk")
model IdentifiedRisk {
  @doc("Risk description")
  description: string;
  
  @doc("Risk probability")
  probability: RiskProbability;
  
  @doc("Risk impact")
  impact: JakartaMigrationIntellijPlugin.RiskLevel;
  
  @doc("Mitigation approach")
  mitigation?: string;
}

@doc("Risk probability levels")
enum RiskProbability {
  @doc("Very low probability")
  VeryLow: "VERY_LOW",
  
  @doc("Low probability")
  Low: "LOW",
  
  @doc("Medium probability")
  Medium: "MEDIUM",
  
  @doc("High probability")
  High: "HIGH",
  
  @doc("Very high probability")
  VeryHigh: "VERY_HIGH"
}

@doc("Plan metadata")
model PlanMetadata {
  @doc("Plan creation timestamp")
  createdAt: utcDateTime;
  
  @doc("Plan version")
  version: string;
  
  @doc("Analysis parameters used")
  analysisParameters: AnalysisOptions;
}

@doc("Analysis options for various operations")
model AnalysisOptions {
  @doc("Include transitive dependencies")
  includeTransitiveDependencies?: boolean = true;
  
  @doc("Analyze test dependencies")
  analyzeTestDependencies?: boolean = true;
  
  @doc("Scan source code")
  scanSourceCode?: boolean = true;
  
  @doc("Analysis timeout in seconds")
  timeoutSeconds?: int32 = 300;
}

@doc("Request to execute migration plan")
model ExecuteMigrationPlanRequest {
  @doc("Path to the project directory")
  projectPath: string;
  
  @doc("Migration plan to execute")
  planId: string;
  
  @doc("Execution options")
  options?: ExecutionOptions;
}

@doc("Execution options for migration plan")
model ExecutionOptions {
  @doc("Dry run mode (don't make actual changes)")
  dryRun?: boolean = false;
  
  @doc("Auto-confirm automated changes")
  autoConfirm?: boolean = false;
  
  @doc("Backup project before changes")
  createBackup?: boolean = true;
}

@doc("Response from migration plan execution")
model ExecuteMigrationPlanResponse {
  @doc("Execution status")
  status: ExecutionStatus;
  
  @doc("Execution results")
  results: ExecutionResults;
  
  @doc("Any errors encountered")
  errors?: ExecutionError[];
}

@doc("Execution status")
enum ExecutionStatus {
  @doc("Execution started")
  Started: "STARTED",
  
  @doc("Execution in progress")
  InProgress: "IN_PROGRESS",
  
  @doc("Execution completed successfully")
  Completed: "COMPLETED",
  
  @doc("Execution failed")
  Failed: "FAILED",
  
  @doc("Execution cancelled")
  Cancelled: "CANCELLED"
}

@doc("Results from migration plan execution")
model ExecutionResults {
  @doc("Phases completed")
  phasesCompleted: int32;
  
  @doc("Tasks completed")
  tasksCompleted: int32;
  
  @doc("Files modified")
  filesModified: string[];
  
  @doc("Dependencies updated")
  dependenciesUpdated: string[];
}

@doc("Execution error information")
model ExecutionError {
  @doc("Error code")
  code: string;
  
  @doc("Error message")
  message: string;
  
  @doc("Phase where error occurred")
  phase?: string;
  
  @doc("Task where error occurred")
  task?: string;
}

@doc("Request to apply automatic fixes")
model ApplyAutoFixesRequest {
  @doc("Path to the project directory")
  projectPath: string;
  
  @doc("Types of fixes to apply")
  fixTypes?: AutoFixType[];
  
  @doc("Fix options")
  options?: AutoFixOptions;
}

@doc("Types of automatic fixes")
enum AutoFixType {
  @doc("Update import statements")
  ImportStatements: "IMPORT_STATEMENTS",
  
  @doc("Update annotations")
  Annotations: "ANNOTATIONS",
  
  @doc("Update configuration files")
  ConfigurationFiles: "CONFIGURATION_FILES",
  
  @doc("Update dependency versions")
  DependencyVersions: "DEPENDENCY_VERSIONS"
}

@doc("Options for automatic fixes")
model AutoFixOptions {
  @doc("Dry run mode")
  dryRun?: boolean = false;
  
  @doc("Create backup before applying fixes")
  createBackup?: boolean = true;
  
  @doc("Apply fixes incrementally")
  incremental?: boolean = false;
}

@doc("Response from applying automatic fixes")
model ApplyAutoFixesResponse {
  @doc("Number of fixes applied")
  fixesApplied: int32;
  
  @doc("Files that were modified")
  modifiedFiles: string[];
  
  @doc("Summary of changes made")
  changesSummary: ChangesSummary;
  
  @doc("Any issues encountered")
  issues?: FixIssue[];
}

@doc("Summary of changes made during auto-fix")
model ChangesSummary {
  @doc("Import statements updated")
  importStatementsUpdated: int32;
  
  @doc("Annotations updated")
  annotationsUpdated: int32;
  
  @doc("Configuration files updated")
  configurationFilesUpdated: int32;
  
  @doc("Dependency versions updated")
  dependencyVersionsUpdated: int32;
}

@doc("Issue encountered during auto-fix")
model FixIssue {
  @doc("Issue severity")
  severity: IssueSeverity;
  
  @doc("Issue description")
  description: string;
  
  @doc("File where issue occurred")
  file?: string;
  
  @doc("Suggested manual action")
  suggestedAction?: string;
}

@doc("Severity levels for fix issues")
enum IssueSeverity {
  @doc("Informational")
  Info: "INFO",
  
  @doc("Warning")
  Warning: "WARNING",
  
  @doc("Error")
  Error: "ERROR"
}