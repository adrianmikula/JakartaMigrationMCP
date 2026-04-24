import "@typespec/http";
import "@typespec/rest";
import "@typespec/openapi3";
import "./intellij-plugin-ui.tsp";

using TypeSpec.Http;
using TypeSpec.Rest;

@service({
  title: "Jakarta Migration Test Coverage Analysis Services",
  version: "1.0.0",
})
@doc("TypeSpec specification for enhanced test coverage analysis and validation confidence scoring")
namespace JakartaMigrationTestCoverage;

/**
 * Test coverage analysis result
 */
@doc("Comprehensive test coverage analysis for a project")
model TestCoverageAnalysis {
  @doc("Overall validation confidence score (0-100)")
  validationConfidenceScore: float32;

  @doc("Unit test coverage percentage")
  unitTestCoverage: float32;

  @doc("Integration test coverage percentage")
  integrationTestCoverage: float32;

  @doc("Critical modules coverage percentage")
  criticalModulesCoverage: float32;

  @doc("Test coverage breakdown by module")
  moduleCoverage: ModuleTestCoverage[];

  @doc("Test framework detection results")
  detectedFrameworks: TestFramework[];

  @doc("Risk assessment based on coverage levels")
  coverageRiskAssessment: CoverageRiskAssessment;

  @doc("Recommendations for improving test coverage")
  recommendations: TestCoverageRecommendation[];
}

/**
 * Test coverage for a specific module
 */
@doc("Test coverage information for a single module")
model ModuleTestCoverage {
  @doc("Module name or path")
  moduleName: string;

  @doc("Number of source files in the module")
  sourceFileCount: int32;

  @doc("Number of test files in the module")
  testFileCount: int32;

  @doc("Calculated coverage percentage")
  coveragePercentage: float32;

  @doc("Whether this module is considered critical")
  isCritical: boolean;

  @doc("Test types present (unit, integration, etc.)")
  testTypes: TestType[];
}

/**
 * Detected test framework
 */
@doc("Information about detected test frameworks")
model TestFramework {
  @doc("Framework name (JUnit, TestNG, etc.)")
  name: string;

  @doc("Framework version if detectable")
  version?: string;

  @doc("Number of test files using this framework")
  testFileCount: int32;

  @doc("Confidence in framework detection")
  confidence: float32;
}

/**
 * Test type enumeration
 */
@doc("Types of tests detected")
enum TestType {
  @doc("Unit tests")
  Unit: "UNIT",

  @doc("Integration tests")
  Integration: "INTEGRATION",

  @doc("System tests")
  System: "SYSTEM",

  @doc("Performance tests")
  Performance: "PERFORMANCE",

  @doc("End-to-end tests")
  EndToEnd: "END_TO_END",
}

/**
 * Risk assessment based on test coverage
 */
@doc("Risk assessment derived from test coverage analysis")
model CoverageRiskAssessment {
  @doc("Overall risk level")
  riskLevel: RiskLevel;

  @doc("Risk score (0-100, higher is riskier)")
  riskScore: float32;

  @doc("Critical modules with low coverage")
  criticalModulesAtRisk: string[];

  @doc("Estimated additional effort needed for testing")
  estimatedTestEffortDays: int32;

  @doc("Migration strategy impact")
  migrationStrategyImpact: MigrationStrategyImpact;
}

/**
 * Test coverage recommendation
 */
@doc("Specific recommendation for improving test coverage")
model TestCoverageRecommendation {
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
}

/**
 * Recommendation priority levels
 */
enum RecommendationPriority {
  @doc("Critical - must address before migration")
  Critical: "CRITICAL",

  @doc("High - should address before migration")
  High: "HIGH",

  @doc("Medium - nice to have")
  Medium: "MEDIUM",

  @doc("Low - optional improvement")
  Low: "LOW",
}

/**
 * Impact on migration strategy
 */
@doc("How test coverage affects migration strategy recommendations")
model MigrationStrategyImpact {
  @doc("Whether incremental migration is recommended")
  requiresIncrementalMigration: boolean;

  @doc("Recommended migration phases")
  recommendedPhases: string[];

  @doc("Additional testing phases needed")
  additionalTestingPhases: string[];

  @doc("Risk mitigation strategies")
  riskMitigationStrategies: string[];
}

/**
 * Test coverage analysis service interface
 */
@route("/api/test-coverage")
@doc("Service for analyzing test coverage in Jakarta migration projects")
interface TestCoverageAnalysisService {
  @doc("Analyze test coverage for a project")
  @route("analyze")
  @post
  analyzeTestCoverage(request: TestCoverageAnalysisRequest): TestCoverageAnalysisResponse;

  @doc("Get test coverage recommendations")
  @route("recommendations")
  @get
  getRecommendations(projectPath: string): TestCoverageRecommendation[];

  @doc("Validate test coverage against migration requirements")
  @route("validate")
  @post
  validateCoverage(request: CoverageValidationRequest): CoverageValidationResponse;
}

/**
 * Test coverage analysis request
 */
@doc("Request to analyze test coverage")
model TestCoverageAnalysisRequest {
  @doc("Project root path")
  projectPath: string;

  @doc("Specific modules to analyze (optional)")
  modules?: string[];

  @doc("Analysis options")
  options?: TestCoverageAnalysisOptions;
}

/**
 * Test coverage analysis options
 */
@doc("Configuration options for test coverage analysis")
model TestCoverageAnalysisOptions {
  @doc("Include integration tests in analysis")
  includeIntegrationTests: boolean = true;

  @doc("Include performance tests in analysis")
  includePerformanceTests: boolean = false;

  @doc("Minimum coverage threshold for critical modules")
  criticalModuleThreshold: float32 = 80.0;

  @doc("Test framework detection sensitivity")
  frameworkDetectionSensitivity: float32 = 0.8;
}

/**
 * Test coverage analysis response
 */
@doc("Response containing test coverage analysis results")
model TestCoverageAnalysisResponse {
  @doc("Analysis results")
  analysis: TestCoverageAnalysis;

  @doc("Analysis metadata")
  metadata: AnalysisMetadata;

  @doc("Any errors encountered during analysis")
  errors?: AnalysisError[];
}

/**
 * Coverage validation request
 */
@doc("Request to validate test coverage against requirements")
model CoverageValidationRequest {
  @doc("Test coverage analysis results")
  coverageAnalysis: TestCoverageAnalysis;

  @doc("Migration requirements")
  requirements: MigrationRequirements;

  @doc("Validation options")
  options?: CoverageValidationOptions;
}

/**
 * Migration requirements for validation
 */
@doc("Requirements that test coverage must meet for safe migration")
model MigrationRequirements {
  @doc("Minimum overall validation confidence score")
  minimumValidationConfidence: float32 = 60.0;

  @doc("Minimum critical module coverage")
  minimumCriticalModuleCoverage: float32 = 80.0;

  @doc("Required test types that must be present")
  requiredTestTypes: TestType[];

  @doc("Maximum allowed risk score")
  maximumRiskScore: float32 = 70.0;
}

/**
 * Coverage validation options
 */
@doc("Options for coverage validation")
model CoverageValidationOptions {
  @doc("Whether to be strict about requirements")
  strictValidation: boolean = false;

  @doc("Custom risk thresholds")
  customThresholds?: Record<string, float32>;
}

/**
 * Coverage validation response
 */
@doc("Response from coverage validation")
model CoverageValidationResponse {
  @doc("Whether coverage meets requirements")
  isValid: boolean;

  @doc("Validation results by requirement")
  validationResults: RequirementValidationResult[];

  @doc("Overall validation confidence")
  overallConfidence: float32;

  @doc("Recommendations if validation fails")
  recommendations?: TestCoverageRecommendation[];
}

/**
 * Individual requirement validation result
 */
@doc("Result of validating a specific requirement")
model RequirementValidationResult {
  @doc("Requirement name")
  requirement: string;

  @doc("Whether requirement is met")
  met: boolean;

  @doc("Actual value")
  actualValue: float32;

  @doc("Required value")
  requiredValue: float32;

  @doc("Severity of failure")
  severity?: ValidationSeverity;
}

/**
 * Validation severity levels
 */
enum ValidationSeverity {
  @doc("Validation passed")
  Pass: "PASS",

  @doc("Minor issue, migration can proceed with caution")
  Warning: "WARNING",

  @doc("Significant issue, migration should be delayed")
  Error: "ERROR",

  @doc("Critical issue, migration not recommended")
  Critical: "CRITICAL",
}

/**
 * Analysis metadata
 */
@doc("Metadata about the analysis process")
model AnalysisMetadata {
  @doc("Analysis timestamp")
  timestamp: string;

  @doc("Analysis duration in milliseconds")
  durationMs: int32;

  @doc("Version of analysis engine")
  engineVersion: string;

  @doc("Analysis configuration used")
  configuration: Record<string, unknown>;
}

/**
 * Analysis error
 */
@doc("Error encountered during analysis")
model AnalysisError {
  @doc("Error type")
  type: string;

  @doc("Error message")
  message: string;

  @doc("Module where error occurred")
  module?: string;

  @doc("Severity of error")
  severity: ErrorSeverity;
}

/**
 * Error severity levels
 */
enum ErrorSeverity {
  @doc("Warning - analysis can continue")
  Warning: "WARNING",

  @doc("Error - analysis may be incomplete")
  Error: "ERROR",

  @doc("Critical - analysis failed")
  Critical: "CRITICAL",
}</content>
<parameter name="filePath">spec/test-coverage-analysis.tsp