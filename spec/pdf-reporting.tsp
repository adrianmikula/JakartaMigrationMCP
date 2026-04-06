import "@typespec/http";
import "@typespec/rest";
import "@typespec/openapi3";
import "./intellij-plugin-ui.tsp";
import "./advanced-scanning.tsp";

using TypeSpec.Http;
using TypeSpec.Rest;

@service({
  title: "Jakarta Migration PDF Reporting Services",
  version: "1.0.0",
})
@doc("TypeSpec specification for Jakarta Migration PDF reporting and analysis services")
namespace JakartaMigrationReporting;

@doc("PDF report configuration options")
model PdfReportConfig {
  @doc("Include dependency tree with only organization dependencies")
  includeDependencyTree: boolean = true;
  
  @doc("Include Maven dependency list")
  includeMavenDependencyList: boolean = true;
  
  @doc("Include Maven dependency tree")
  includeMavenDependencyTree: boolean = true;
  
  @doc("Include advanced scan results")
  includeAdvancedScanResults: boolean = true;
  
  @doc("Include support links footer")
  includeSupportFooter: boolean = true;
  
  @doc("Custom report title")
  title?: string;
  
  @doc("Custom report description")
  description?: string;
  
  @doc("Output file path")
  outputPath?: string;
}

@doc("Dependency tree information for PDF report")
model DependencyTreeInfo {
  @doc("Organization dependencies only")
  organizationDependencies: DependencyInfo[];
  
  @doc("Total dependency count")
  totalDependencies: int32;
  
  @doc("Dependency depth levels")
  depthLevels: int32;
  
  @doc("Tree visualization data")
  treeData: DependencyTreeNode[];
}

@doc("Dependency tree node for visualization")
model DependencyTreeNode {
  @doc("Dependency identifier")
  id: string;
  
  @doc("Dependency name")
  name: string;
  
  @doc("Group ID")
  groupId: string;
  
  @doc("Artifact ID") 
  artifactId: string;
  
  @doc("Version")
  version: string;
  
  @doc("Child nodes")
  children: DependencyTreeNode[];
  
  @doc("Migration status")
  migrationStatus: string;
  
  @doc("Risk level")
  riskLevel: string;
}

@doc("Maven dependency list information")
model MavenDependencyList {
  @doc("List of dependencies")
  dependencies: MavenDependency[];
  
  @doc("Total count")
  totalCount: int32;
  
  @doc("Generated timestamp")
  generatedAt: utcDateTime;
}

@doc("Maven dependency information")
model MavenDependency {
  @doc("Group ID")
  groupId: string;
  
  @doc("Artifact ID")
  artifactId: string;
  
  @doc("Version")
  version: string;
  
  @doc("Scope")
  scope: string;
  
  @doc("Jakarta compatible version")
  jakartaVersion?: string;
  
  @doc("Migration status")
  migrationStatus: string;
  
  @doc("Notes")
  notes?: string;
}

@doc("Maven dependency tree information")
model MavenDependencyTree {
  @doc("Root dependency")
  root: DependencyTreeNode;
  
  @doc("Total nodes")
  totalNodes: int32;
  
  @doc("Maximum depth")
  maxDepth: int32;
  
  @doc("Conflict information")
  conflicts: DependencyConflict[];
}

@doc("Dependency conflict information")
model DependencyConflict {
  @doc("Conflict dependency")
  dependency: MavenDependency;
  
  @doc("Conflicting versions")
  conflictingVersions: string[];
  
  @doc("Resolution")
  resolution?: string;
  
  @doc("Severity")
  severity: "LOW" | "MEDIUM" | "HIGH";
}

@doc("Advanced scan results summary for PDF")
model AdvancedScanResultsSummary {
  @doc("JPA scan results")
  jpaResults?: JpaProjectScanResult;
  
  @doc("Bean validation scan results")
  beanValidationResults?: BeanValidationProjectScanResult;
  
  @doc("Servlet/JSP scan results")
  servletJspResults?: ServletJspProjectScanResult;
  
  @doc("Other scan results")
  otherResults: Record<GenericProjectScanResult>;
  
  @doc("Total issues found")
  totalIssuesFound: int32;
  
  @doc("Scan completion time")
  scanCompletedAt: utcDateTime;
}

@doc("Support links for PDF footer")
model SupportLinks {
  @doc("Documentation URL")
  documentationUrl: string;
  
  @doc("Support email")
  supportEmail: string;
  
  @doc("GitHub repository")
  githubUrl: string;
  
  @doc("Community forum")
  communityForumUrl?: string;
  
  @doc("Bug tracker")
  bugTrackerUrl?: string;
}

@doc("PDF generation request")
model GeneratePdfReportRequest {
  @doc("Project path")
  projectPath: string;
  
  @doc("Report configuration")
  config: PdfReportConfig;
  
  @doc("Dependency tree data")
  dependencyTree?: DependencyTreeInfo;
  
  @doc("Maven dependency list")
  mavenDependencyList?: MavenDependencyList;
  
  @doc("Maven dependency tree")
  mavenDependencyTree?: MavenDependencyTree;
  
  @doc("Advanced scan results")
  advancedScanResults?: AdvancedScanResultsSummary;
  
  @doc("Support links")
  supportLinks: SupportLinks;
}

@doc("PDF generation response")
model GeneratePdfReportResponse {
  @doc("Success status")
  success: boolean;
  
  @doc("Generated file path")
  filePath?: string;
  
  @doc("File size in bytes")
  fileSize?: int64;
  
  @doc("Generation duration in milliseconds")
  durationMs: int32;
  
  @doc("Error message if failed")
  errorMessage?: string;
  
  @doc("Warnings")
  warnings: string[];
}

@doc("PDF report service interface")
@route("/api/pdf-reports")
interface PdfReportService {
  @doc("Generate PDF report for project")
  @post("generate")
  generatePdfReport(
    @doc("PDF generation request")
    @body request: GeneratePdfReportRequest
  ): GeneratePdfReportResponse;
  
  @doc("Get available report templates")
  @get("templates")
  getReportTemplates(): ReportTemplate[];
  
  @doc("Validate report configuration")
  @post("validate-config")
  validateReportConfig(
    @doc("Configuration to validate")
    @body config: PdfReportConfig
  ): ValidationResult;
}

@doc("Report template information")
model ReportTemplate {
  @doc("Template ID")
  id: string;
  
  @doc("Template name")
  name: string;
  
  @doc("Template description")
  description: string;
  
  @doc("Template type")
  type: "STANDARD" | "DETAILED" | "SUMMARY" | "CUSTOM";
  
  @doc("Supported sections")
  supportedSections: ReportSection[];
  
  @doc("Template preview URL")
  previewUrl?: string;
}

@doc("Report section types")
enum ReportSection {
  @doc("Title and overview")
  Title: "TITLE",
  
  @doc("Executive summary")
  ExecutiveSummary: "EXECUTIVE_SUMMARY",
  
  @doc("Dependency tree")
  DependencyTree: "DEPENDENCY_TREE",
  
  @doc("Maven dependency list")
  MavenDependencyList: "MAVEN_DEPENDENCY_LIST",
  
  @doc("Maven dependency tree")
  MavenDependencyTree: "MAVEN_DEPENDENCY_TREE",
  
  @doc("Advanced scan results")
  AdvancedScanResults: "ADVANCED_SCAN_RESULTS",
  
  @doc("Recommendations")
  Recommendations: "RECOMMENDATIONS",
  
  @doc("Support information")
  SupportInfo: "SUPPORT_INFO"
}

@doc("Validation result")
model ValidationResult {
  @doc("Validation status")
  valid: boolean;
  
  @doc("Validation errors")
  errors: ValidationError[];
  
  @doc("Validation warnings")
  warnings: ValidationWarning[];
}

@doc("Validation error")
model ValidationError {
  @doc("Error code")
  code: string;
  
  @doc("Error message")
  message: string;
  
  @doc("Field path")
  field?: string;
  
  @doc("Error severity")
  severity: "ERROR" | "WARNING" | "INFO";
}

@doc("Validation warning")
model ValidationWarning {
  @doc("Warning code")
  code: string;
  
  @doc("Warning message")
  message: string;
  
  @doc("Field path")
  field?: string;
}
