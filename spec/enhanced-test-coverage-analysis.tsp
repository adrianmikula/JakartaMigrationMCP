import "@typespec/http";
import "@typespec/rest";
import "@typespec/openapi3";
import "./intellij-plugin-ui.tsp";

using TypeSpec.Http;
using TypeSpec.Rest;

@service({
  title: "Jakarta Migration Enhanced Test Coverage Analysis Services",
  version: "1.0.0",
})
@doc("TypeSpec specification for enhanced test coverage analysis building on existing file counting functionality")
namespace JakartaMigrationEnhancedTestCoverage;

/**
 * Enhanced test coverage analysis result that builds on existing file counting
 */
@doc("Enhanced test coverage analysis using existing file counting infrastructure")
model EnhancedTestCoverageAnalysis {
  @doc("Overall validation confidence score (0-100)")
  validationConfidenceScore: float32;

  @doc("Unit test coverage percentage (existing formula: testFiles * 1000 / totalFiles)")
  unitTestCoverage: float32;

  @doc("Integration test coverage percentage (estimated as 20% of test files)")
  integrationTestCoverage: float32;

  @doc("Critical modules coverage percentage")
  criticalModulesCoverage: float32;

  @doc("Detailed breakdown by module (enhanced from existing logic)")
  moduleCoverage: EnhancedModuleCoverage[];

  @doc("Test framework detection results")
  detectedFrameworks: TestFramework[];

  @doc("Risk assessment based on coverage levels")
  coverageRiskAssessment: CoverageRiskAssessment;

  @doc("Enhanced recommendations for improving test coverage")
  recommendations: EnhancedTestCoverageRecommendation[];

  @doc("Critical risk zones - modules with migration issues AND low coverage")
  criticalRiskZones: CriticalRiskZone[];
}

/**
 * Enhanced module coverage with more detailed analysis
 */
@doc("Enhanced module coverage information")
model EnhancedModuleCoverage {
  @doc("Module name or path")
  moduleName: string;

  @doc("Number of source files in the module")
  sourceFileCount: int32;

  @doc("Number of test files in the module (from existing scanner)")
  testFileCount: int32;

  @doc("Calculated coverage percentage using existing formula")
  coveragePercentage: float32;

  @doc("Whether this module is considered critical")
  isCritical: boolean;

  @doc("Test types present (enhanced detection)")
  testTypes: TestType[];

  @doc("Has Jakarta migration issues (for critical risk zone detection)")
  hasMigrationIssues: boolean;

  @doc("Risk score for this module")
  moduleRiskScore: float32;
}

/**
 * Critical risk zone - combination of migration issues and low test coverage
 */
@doc("Module with both migration issues and insufficient test coverage")
model CriticalRiskZone {
  @doc("Module name")
  moduleName: string;

  @doc("Migration issues found in this module")
  migrationIssues: string[];

  @doc("Test coverage percentage")
  coveragePercentage: float32;

  @doc("Combined risk score")
  combinedRiskScore: float32;

  @doc("Priority for addressing this risk zone")
  priority: RiskZonePriority;

  @doc("Recommended actions")
  recommendedActions: string[];
}

/**
 * Risk zone priority levels
 */
enum RiskZonePriority {
  @doc("Immediate attention required - high risk combination")
  Critical: "CRITICAL",

  @doc("High priority - significant risk")
  High: "HIGH",

  @doc("Medium priority - moderate risk")
  Medium: "MEDIUM",

  @doc("Low priority - acceptable risk")
  Low: "LOW",
}

/**
 * Enhanced test coverage recommendation
 */
@doc("Enhanced recommendation with more specific guidance")
model EnhancedTestCoverageRecommendation {
  @doc("Recommendation priority")
  priority: RecommendationPriority;

  @doc("Recommendation title")
  title: string;

  @doc("Detailed description")
  description: string;

  @doc("Affected modules")
  affectedModules: string[];

  @doc("Estimated effort in hours")
  estimatedEffortHours: int32;

  @doc("Expected impact on validation confidence")
  expectedImpact: float32;

  @doc("Specific test types to add")
  testTypesToAdd: TestType[];

  @doc("Related to critical risk zones")
  addressesCriticalRiskZone: boolean;
}

/**
 * Enhanced test coverage analysis service
 */
@route("/api/enhanced-test-coverage")
@doc("Service for enhanced test coverage analysis using existing file counting infrastructure")
interface EnhancedTestCoverageAnalysisService {
  @doc("Perform enhanced test coverage analysis")
  @route("analyze")
  @post
  analyzeEnhancedCoverage(request: EnhancedTestCoverageRequest): EnhancedTestCoverageResponse;

  @doc("Get critical risk zones")
  @route("critical-zones")
  @get
  getCriticalRiskZones(projectPath: string): CriticalRiskZone[];

  @doc("Validate coverage for migration readiness")
  @route("validate-migration-readiness")
  @post
  validateMigrationReadiness(request: MigrationReadinessValidationRequest): MigrationReadinessValidationResponse;
}

/**
 * Enhanced test coverage analysis request
 */
@doc("Request for enhanced test coverage analysis")
model EnhancedTestCoverageRequest {
  @doc("Project root path")
  projectPath: string;

  @doc("Migration analysis results to correlate with coverage")
  migrationAnalysisResults?: MigrationAnalysisResults;

  @doc("Analysis options")
  options?: EnhancedAnalysisOptions;
}

/**
 * Enhanced analysis options
 */
@doc("Options for enhanced analysis")
model EnhancedAnalysisOptions {
  @doc("Include module-level breakdown")
  includeModuleBreakdown: boolean = true;

  @doc("Detect critical risk zones")
  detectCriticalRiskZones: boolean = true;

  @doc("Custom risk thresholds")
  customRiskThresholds?: CoverageRiskThresholds;

  @doc("Minimum coverage for critical modules")
  criticalModuleThreshold: float32 = 80.0;
}

/**
 * Coverage risk thresholds
 */
@doc("Customizable risk thresholds for coverage analysis")
model CoverageRiskThresholds {
  @doc("Low coverage threshold")
  lowCoverageThreshold: float32 = 30.0;

  @doc("Medium coverage threshold")
  mediumCoverageThreshold: float32 = 60.0;

  @doc("High coverage threshold")
  highCoverageThreshold: float32 = 80.0;
}

/**
 * Migration analysis results for correlation
 */
@doc("Migration analysis results to correlate with test coverage")
model MigrationAnalysisResults {
  @doc("Modules with migration issues")
  modulesWithIssues: Record<string, string[]>;

  @doc("Overall migration risk score")
  migrationRiskScore: float32;

  @doc("Blockers identified")
  blockers: string[];
}

/**
 * Enhanced test coverage response
 */
@doc("Response with enhanced test coverage analysis")
model EnhancedTestCoverageResponse {
  @doc("Enhanced analysis results")
  analysis: EnhancedTestCoverageAnalysis;

  @doc("Analysis metadata")
  metadata: AnalysisMetadata;

  @doc("Integration with existing dashboard")
  dashboardIntegration: DashboardIntegrationData;
}

/**
 * Dashboard integration data
 */
@doc("Data for integrating with existing dashboard UI")
model DashboardIntegrationData {
  @doc("Validation confidence gauge value")
  validationConfidenceValue: float32;

  @doc("Risk explanation bullets for UI")
  riskExplanationBullets: string[];

  @doc("Critical risk zones for highlighting")
  criticalRiskZones: CriticalRiskZone[];
}

/**
 * Migration readiness validation request
 */
@doc("Request to validate if project is ready for migration based on coverage")
model MigrationReadinessValidationRequest {
  @doc("Enhanced coverage analysis")
  coverageAnalysis: EnhancedTestCoverageAnalysis;

  @doc("Migration requirements")
  requirements: MigrationReadinessRequirements;
}

/**
 * Migration readiness requirements
 */
@doc("Requirements for migration readiness")
model MigrationReadinessRequirements {
  @doc("Minimum validation confidence for migration")
  minimumValidationConfidence: float32 = 60.0;

  @doc("Maximum allowed critical risk zones")
  maxCriticalRiskZones: int32 = 3;

  @doc("Required coverage for high-risk modules")
  highRiskModuleCoverage: float32 = 80.0;
}

/**
 * Migration readiness validation response
 */
@doc("Response indicating migration readiness")
model MigrationReadinessValidationResponse {
  @doc("Whether project is ready for migration")
  migrationReady: boolean;

  @doc("Readiness score")
  readinessScore: float32;

  @doc("Blocking issues")
  blockingIssues: string[];

  @doc("Recommended preparation steps")
  preparationSteps: string[];
}</content>
<parameter name="filePath">spec/enhanced-test-coverage-analysis.tsp