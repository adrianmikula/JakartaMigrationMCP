import "@typespec/http";
import "@typespec/rest";
import "@typespec/openapi3";
import "./intellij-plugin-ui.tsp";

using TypeSpec.Http;
using TypeSpec.Rest;

@service({
  title: "Jakarta Migration HTML-to-PDF Report Services",
  version: "1.0.0",
})
@doc("TypeSpec specification for HTML-to-PDF report generation replacing PDFBox approach")
namespace JakartaMigrationHtmlToPdfReporting;

/**
 * HTML-based report generation request
 */
@doc("Request to generate an HTML-based PDF report")
model HtmlToPdfReportRequest {
  @doc("Report title")
  title: string;

  @doc("Project information")
  projectInfo: ProjectInfo;

  @doc("Migration analysis results")
  migrationAnalysis: MigrationAnalysisSummary;

  @doc("Test coverage analysis results")
  testCoverageAnalysis?: EnhancedTestCoverageAnalysis;

  @doc("Report generation options")
  options: HtmlReportOptions;
}

/**
 * Project information for reports
 */
@doc("Basic project information included in reports")
model ProjectInfo {
  @doc("Project name")
  name: string;

  @doc("Project path")
  path: string;

  @doc("Analysis timestamp")
  timestamp: string;

  @doc("Plugin version used")
  pluginVersion: string;
}

/**
 * Migration analysis summary for reports
 */
@doc("Summary of migration analysis for report generation")
model MigrationAnalysisSummary {
  @doc("Overall risk score")
  riskScore: float32;

  @doc("Risk category")
  riskCategory: RiskLevel;

  @doc("Validation confidence score")
  validationConfidence: float32;

  @doc("Migration effort estimate")
  effortEstimate: EffortEstimate;

  @doc("Top blockers")
  topBlockers: string[];

  @doc("Dependency analysis summary")
  dependencySummary: DependencySummary;

  @doc("Platform analysis summary")
  platformSummary: PlatformSummary;

  @doc("Advanced scan results summary")
  advancedScanSummary: AdvancedScanSummary;
}

/**
 * Effort estimate
 */
@doc("Migration effort estimation")
model EffortEstimate {
  @doc("Estimated days")
  days: int32;

  @doc("Effort category")
  category: EffortCategory;

  @doc("Factors affecting effort")
  factors: string[];
}

/**
 * Effort categories
 */
enum EffortCategory {
  @doc("Low effort - less than 3 days")
  Low: "LOW",

  @doc("Medium effort - 3-7 days")
  Medium: "MEDIUM",

  @doc("High effort - 7-14 days")
  High: "HIGH",

  @doc("Very high effort - more than 14 days")
  VeryHigh: "VERY_HIGH",
}

/**
 * Dependency analysis summary
 */
@doc("Summary of dependency analysis results")
model DependencySummary {
  @doc("Total dependencies analyzed")
  totalDependencies: int32;

  @doc("Dependencies requiring upgrade")
  dependenciesToUpgrade: int32;

  @doc("Compatible dependencies")
  compatibleDependencies: int32;

  @doc("Dependencies with no Jakarta equivalent")
  noEquivalentDependencies: int32;

  @doc("Organisational dependencies")
  organisationalDependencies: int32;
}

/**
 * Platform analysis summary
 */
@doc("Summary of platform compatibility analysis")
model PlatformSummary {
  @doc("Detected platforms")
  detectedPlatforms: string[];

  @doc("Platform compatibility score")
  compatibilityScore: float32;

  @doc("Deployment artifacts found")
  deploymentArtifacts: DeploymentArtifactCount;
}

/**
 * Deployment artifact counts
 */
@doc("Count of different deployment artifacts")
model DeploymentArtifactCount {
  @doc("WAR files")
  warFiles: int32;

  @doc("EAR files")
  earFiles: int32;

  @doc("JAR files")
  jarFiles: int32;

  @doc("Total artifacts")
  total: int32;
}

/**
 * Advanced scan results summary
 */
@doc("Summary of advanced scanning results")
model AdvancedScanSummary {
  @doc("Total issues found")
  totalIssues: int32;

  @doc("Issues by category")
  issuesByCategory: Record<string, int32>;

  @doc("Top issue categories")
  topCategories: string[];
}

/**
 * HTML report generation options
 */
@doc("Options for HTML report generation")
model HtmlReportOptions {
  @doc("Report template to use")
  template: ReportTemplate = ReportTemplate.Professional;

  @doc("Include executive summary")
  includeExecutiveSummary: boolean = true;

  @doc("Include detailed analysis")
  includeDetailedAnalysis: boolean = true;

  @doc("Include recommendations")
  includeRecommendations: boolean = true;

  @doc("Color scheme")
  colorScheme: ColorScheme = ColorScheme.Professional;

  @doc("Page layout options")
  layout: PageLayout = PageLayout.Standard;
}

/**
 * Report templates
 */
enum ReportTemplate {
  @doc("Professional template with executive summary")
  Professional: "PROFESSIONAL",

  @doc("Technical template with detailed analysis")
  Technical: "TECHNICAL",

  @doc("Minimal template for quick reports")
  Minimal: "MINIMAL",
}

/**
 * Color schemes for reports
 */
enum ColorScheme {
  @doc("Professional blue and gray color scheme")
  Professional: "PROFESSIONAL",

  @doc("Corporate color scheme")
  Corporate: "CORPORATE",

  @doc("High contrast for accessibility")
  HighContrast: "HIGH_CONTRAST",
}

/**
 * Page layout options
 */
enum PageLayout {
  @doc("Standard A4 layout")
  Standard: "STANDARD",

  @doc("Compact layout for more content")
  Compact: "COMPACT",

  @doc("Wide layout for detailed tables")
  Wide: "WIDE",
}

/**
 * HTML report generation response
 */
@doc("Response from HTML report generation")
model HtmlToPdfReportResponse {
  @doc("Generated PDF file path")
  pdfPath: string;

  @doc("Generated HTML content (for debugging)")
  htmlContent?: string;

  @doc("Report metadata")
  metadata: ReportMetadata;

  @doc("Generation statistics")
  statistics: ReportGenerationStatistics;
}

/**
 * Report metadata
 */
@doc("Metadata about the generated report")
model ReportMetadata {
  @doc("Report ID")
  id: string;

  @doc("Generation timestamp")
  generatedAt: string;

  @doc("Report version")
  version: string;

  @doc("Template used")
  template: ReportTemplate;

  @doc("Total pages")
  pageCount: int32;
}

/**
 * Report generation statistics
 */
@doc("Statistics about report generation process")
model ReportGenerationStatistics {
  @doc("HTML generation time in milliseconds")
  htmlGenerationTimeMs: int32;

  @doc("PDF conversion time in milliseconds")
  pdfConversionTimeMs: int32;

  @doc("Total generation time in milliseconds")
  totalTimeMs: int32;

  @doc("PDF file size in bytes")
  pdfSizeBytes: int32;
}

/**
 * HTML template definition
 */
@doc("Definition of an HTML report template")
model HtmlTemplate {
  @doc("Template name")
  name: string;

  @doc("Template description")
  description: string;

  @doc("CSS styles for the template")
  styles: string;

  @doc("HTML structure template")
  htmlStructure: string;

  @doc("Supported sections")
  supportedSections: ReportSection[];
}

/**
 * Report sections
 */
enum ReportSection {
  @doc("Executive summary section")
  ExecutiveSummary: "EXECUTIVE_SUMMARY",

  @doc("Risk assessment section")
  RiskAssessment: "RISK_ASSESSMENT",

  @doc("Dependency analysis section")
  DependencyAnalysis: "DEPENDENCY_ANALYSIS",

  @doc("Platform analysis section")
  PlatformAnalysis: "PLATFORM_ANALYSIS",

  @doc("Advanced scanning results")
  AdvancedScanning: "ADVANCED_SCANNING",

  @doc("Test coverage analysis")
  TestCoverage: "TEST_COVERAGE",

  @doc("Migration recommendations")
  Recommendations: "RECOMMENDATIONS",

  @doc("Detailed findings")
  DetailedFindings: "DETAILED_FINDINGS",
}

/**
 * HTML-to-PDF conversion service
 */
@route("/api/reports/html-to-pdf")
@doc("Service for generating professional PDF reports using HTML templates")
interface HtmlToPdfReportService {
  @doc("Generate a comprehensive PDF report using HTML templates")
  @route("generate")
  @post
  generateReport(request: HtmlToPdfReportRequest): HtmlToPdfReportResponse;

  @doc("Get available report templates")
  @route("templates")
  @get
  getTemplates(): HtmlTemplate[];

  @doc("Preview HTML report before PDF conversion")
  @route("preview")
  @post
  previewReport(request: HtmlToPdfReportRequest): HtmlReportPreviewResponse;

  @doc("Validate report data before generation")
  @route("validate")
  @post
  validateReportData(request: HtmlToPdfReportRequest): ReportValidationResponse;
}

/**
 * HTML report preview response
 */
@doc("Response containing HTML preview of the report")
model HtmlReportPreviewResponse {
  @doc("Generated HTML content")
  htmlContent: string;

  @doc("CSS styles used")
  styles: string;

  @doc("Preview metadata")
  metadata: ReportMetadata;

  @doc("Estimated PDF page count")
  estimatedPageCount: int32;
}

/**
 * Report validation response
 */
@doc("Response from report data validation")
model ReportValidationResponse {
  @doc("Whether the report data is valid")
  isValid: boolean;

  @doc("Validation errors")
  errors: ReportValidationError[];

  @doc("Validation warnings")
  warnings: ReportValidationWarning[];

  @doc("Data quality score")
  dataQualityScore: float32;
}

/**
 * Report validation error
 */
@doc("Error found during report validation")
model ReportValidationError {
  @doc("Error type")
  type: string;

  @doc("Error message")
  message: string;

  @doc("Field that caused the error")
  field?: string;

  @doc("Severity of the error")
  severity: ValidationErrorSeverity;
}

/**
 * Report validation warning
 */
@doc("Warning from report validation")
model ReportValidationWarning {
  @doc("Warning type")
  type: string;

  @doc("Warning message")
  message: string;

  @doc("Suggested fix")
  suggestion?: string;
}

/**
 * Validation error severity
 */
enum ValidationErrorSeverity {
  @doc("Critical error - report cannot be generated")
  Critical: "CRITICAL",

  @doc("Error - report generation may fail")
  Error: "ERROR",

  @doc("Warning - report may be incomplete")
  Warning: "WARNING",
}</content>
<parameter name="filePath">spec/html-to-pdf-reporting.tsp