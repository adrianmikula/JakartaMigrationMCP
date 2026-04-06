import "@typespec/http";
import "@typespec/rest";
import "@typespec/openapi3";
import "./intellij-plugin-ui.tsp";

using TypeSpec.Http;
using TypeSpec.Rest;

@service({
  title: "Jakarta Migration Advanced Scanning Services",
  version: "1.0.0",
})
@doc("TypeSpec specification for Jakarta Migration advanced scanning and recipe recommendation services")
namespace JakartaMigrationAdvancedScanning;

@doc("Advanced scanning result types")
enum ScanType {
  @doc("JPA annotation scanning")
  JpaAnnotation: "JPA_ANNOTATION_SCANNER",
  
  @doc("Bean validation scanning")
  BeanValidation: "BEAN_VALIDATION_SCANNER",
  
  @doc("Servlet/JSP scanning")
  ServletJsp: "SERVLET_JSP_SCANNER",
  
  @doc("CDI injection scanning")
  CdiInjection: "CDI_INJECTION_SCANNER",
  
  @doc("Build configuration scanning")
  BuildConfig: "BUILD_CONFIG_SCANNER",
  
  @doc("REST/SOAP API scanning")
  RestSoap: "REST_SOAP_SCANNER",
  
  @doc("Security API scanning")
  SecurityApi: "SECURITY_API_SCANNER",
  
  @doc("JMS messaging scanning")
  JmsMessaging: "JMS_MESSAGING_SCANNER",
  
  @doc("Transitive dependency scanning")
  TransitiveDependency: "TRANSITIVE_DEPENDENCY_SCANNER",
  
  @doc("Configuration file scanning")
  ConfigFile: "CONFIG_FILE_SCANNER",
  
  @doc("Deprecated API scanning")
  DeprecatedApi: "DEPRECATED_API_SCANNER",
  
  @doc("Third-party library scanning")
  ThirdPartyLib: "THIRD_PARTY_LIB_SCANNER",
  
  @doc("Unit test scanning")
  UnitTest: "UNIT_TEST_SCANNER",
  
  @doc("Test containers scanning")
  TestContainers: "TEST_CONTAINERS_SCANNER",
  
  @doc("Integration points scanning")
  IntegrationPoints: "INTEGRATION_POINTS_SCANNER",
  
  @doc("Application server scanning")
  AppServer: "APP_SERVER_SCANNER",
  
  @doc("Classloader/module scanning")
  ClassloaderModule: "CLASSLOADER_MODULE_SCANNER",
  
  @doc("Logging/metrics scanning")
  LoggingMetrics: "LOGGING_METRICS_SCANNER",
  
  @doc("Serialization/cache scanning")
  SerializationCache: "SERIALIZATION_CACHE_SCANNER"
}

@doc("JPA annotation usage information")
model JpaAnnotationUsage {
  @doc("Full annotation class name")
  annotationName: string;
  
  @doc("Target class where annotation is used")
  targetClass: string;
  
  @doc("Line number in source file")
  lineNumber: int32;
  
  @doc("Jakarta equivalent annotation name")
  jakartaEquivalent: string;
}

@doc("JPA scan result for a single file")
model JpaScanResult {
  @doc("File path relative to project root")
  filePath: string;
  
  @doc("List of JPA annotations found in file")
  annotations: JpaAnnotationUsage[];
  
  @doc("Total number of JPA annotations found")
  annotationCount: int32;
}

@doc("JPA scan result for entire project")
model JpaProjectScanResult {
  @doc("Results for each file")
  fileResults: JpaScanResult[];
  
  @doc("Total files scanned")
  totalFilesScanned: int32;
  
  @doc("Files with javax.persistence usage")
  totalFilesWithJavaxUsage: int32;
  
  @doc("Total annotations found")
  totalAnnotationsFound: int32;
  
  @doc("True if any javax.persistence usage was found")
  hasJavaxUsage: boolean;
}

@doc("Bean validation annotation usage")
model BeanValidationUsage {
  @doc("Annotation class name")
  annotationName: string;
  
  @doc("Target class or method")
  target: string;
  
  @doc("Line number")
  lineNumber: int32;
  
  @doc("Jakarta equivalent")
  jakartaEquivalent: string;
}

@doc("Bean validation scan result for file")
model BeanValidationScanResult {
  @doc("File path")
  filePath: string;
  
  @doc("Validation annotations found")
  annotations: BeanValidationUsage[];
  
  @doc("Annotation count")
  annotationCount: int32;
}

@doc("Bean validation scan result for project")
model BeanValidationProjectScanResult {
  @doc("File results")
  fileResults: BeanValidationScanResult[];
  
  @doc("Total files scanned")
  totalFilesScanned: int32;
  
  @doc("Files with javax.validation usage")
  totalFilesWithJavaxUsage: int32;
  
  @doc("Total annotations found")
  totalAnnotationsFound: int32;
}

@doc("Servlet/JSP usage information")
model ServletJspUsage {
  @doc("Servlet/JSP class or tag")
  className: string;
  
  @doc("Usage type (servlet, filter, listener, jsp tag)")
  usageType: string;
  
  @doc("Line number")
  lineNumber: int32;
  
  @doc("Jakarta equivalent")
  jakartaEquivalent: string;
}

@doc("Servlet/JSP scan result for file")
model ServletJspScanResult {
  @doc("File path")
  filePath: string;
  
  @doc("Servlet/JSP usages found")
  usages: ServletJspUsage[];
  
  @doc("Usage count")
  usageCount: int32;
}

@doc("Servlet/JSP scan result for project")
model ServletJspProjectScanResult {
  @doc("File results")
  fileResults: ServletJspScanResult[];
  
  @doc("Total files scanned")
  totalFilesScanned: int32;
  
  @doc("Files with javax.servlet usage")
  totalFilesWithJavaxUsage: int32;
  
  @doc("Total usages found")
  totalUsagesFound: int32;
}

@doc("Generic scan result for other scanner types")
model GenericScanResult {
  @doc("File path")
  filePath: string;
  
  @doc("Issues found")
  issues: string[];
  
  @doc("Issue count")
  issueCount: int32;
  
  @doc("Scanner-specific metadata")
  metadata: Record<string>;
}

@doc("Generic project scan result")
model GenericProjectScanResult {
  @doc("File results")
  fileResults: GenericScanResult[];
  
  @doc("Total files scanned")
  totalFilesScanned: int32;
  
  @doc("Files with issues")
  totalFilesWithIssues: int32;
  
  @doc("Total issues found")
  totalIssuesFound: int32;
  
  @doc("Scanner type")
  scanType: ScanType;
}

@doc("Recipe category enumeration")
enum RecipeCategory {
  @doc("Namespace migration recipes")
  NamespaceMigration: "NAMESPACE_MIGRATION",
  
  @doc("Database and JPA recipes")
  Database: "DATABASE",
  
  @doc("CDI and injection recipes")
  Cdi: "CDI",
  
  @doc("REST and SOAP API recipes")
  Api: "API",
  
  @doc("Web-tier recipes")
  Web: "WEB",
  
  @doc("Security API recipes")
  Security: "SECURITY",
  
  @doc("Configuration file recipes")
  Configuration: "CONFIGURATION",
  
  @doc("Annotation migration recipes")
  Annotations: "ANNOTATIONS"
}

@doc("Recipe type enumeration")
enum RecipeType {
  @doc("OpenRewrite recipe")
  OpenRewrite: "OPENREWRITE",
  
  @doc("Regex-based recipe")
  Regex: "REGEX",
  
  @doc("Custom recipe implementation")
  Custom: "CUSTOM"
}

@doc("Recipe definition")
model RecipeDefinition {
  @doc("Recipe ID")
  id?: int64;
  
  @doc("Recipe name")
  name: string;
  
  @doc("Recipe description")
  description: string;
  
  @doc("Recipe category")
  category: RecipeCategory;
  
  @doc("Recipe type")
  recipeType: RecipeType;
  
  @doc("OpenRewrite recipe class name")
  openRewriteRecipeName?: string;
  
  @doc("Regex pattern for matching")
  pattern?: string;
  
  @doc("Replacement string")
  replacement?: string;
  
  @doc("File pattern for regex recipes")
  filePattern?: string;
  
  @doc("Whether recipe is reversible")
  reversible: boolean;
  
  @doc("Creation timestamp")
  createdAt?: utcDateTime;
  
  @doc("Last run date")
  lastRunDate?: utcDateTime;
  
  @doc("Recipe status")
  status?: RecipeStatus;
}

@doc("Recipe execution status")
enum RecipeStatus {
  @doc("Never run")
  NeverRun: "NEVER_RUN",
  
  @doc("Run successfully")
  RunSuccess: "RUN_SUCCESS",
  
  @doc("Run and undone")
  RunUndone: "RUN_UNDONE",
  
  @doc("Run failed")
  RunFailed: "RUN_FAILED"
}

@doc("Recipe recommendation with confidence score")
model RecipeRecommendation {
  @doc("Recommended recipe")
  recipe: RecipeDefinition;
  
  @doc("Confidence score (0.0-1.0)")
  confidenceScore: float32;
  
  @doc("Reason for recommendation")
  reason: string;
  
  @doc("Affected files")
  affectedFiles: string[];
}

@doc("Advanced scanning service interface")
@route("/api/advanced-scanning")
interface AdvancedScanningService {
  @doc("Scan project for JPA annotations")
  @post("scan/jpa")
  scanJpaAnnotations(
    @doc("Project path")
    @query projectPath: string
  ): JpaProjectScanResult;
  
  @doc("Scan single file for JPA annotations")
  @post("scan/jpa/file")
  scanJpaAnnotationsFile(
    @doc("File path")
    @query filePath: string
  ): JpaScanResult;
  
  @doc("Scan project for bean validation annotations")
  @post("scan/bean-validation")
  scanBeanValidation(
    @doc("Project path")
    @query projectPath: string
  ): BeanValidationProjectScanResult;
  
  @doc("Scan project for servlet/JSP usage")
  @post("scan/servlet-jsp")
  scanServletJsp(
    @doc("Project path")
    @query projectPath: string
  ): ServletJspProjectScanResult;
  
  @doc("Run comprehensive advanced scan")
  @post("scan/comprehensive")
  runComprehensiveScan(
    @doc("Project path")
    @query projectPath: string,
    @doc("Scanner types to include")
    @query scannerTypes: ScanType[]
  ): ComprehensiveScanResults;
}

@doc("Recipe recommendation service interface")
@route("/api/recipe-recommendations")
interface RecipeRecommendationService {
  @doc("Get recipe recommendations for scan results")
  @post("recommendations")
  getRecipeRecommendations(
    @doc("Project path")
    @query projectPath: string,
    @doc("Scan results")
    scanResults: ScanResultsMap
  ): RecipeRecommendation[];
  
  @doc("Get recipe recommendations for specific file")
  @post("recommendations/file")
  getRecipeRecommendationsForFile(
    @doc("File path")
    @query filePath: string,
    @doc("Scan type")
    @query scanType: ScanType
  ): RecipeRecommendation[];
  
  @doc("Get all recipes organized by scan type")
  @get("recipes/by-scan-type")
  getRecipesByScanType(): RecipesByScanTypeMap;
}

@doc("Comprehensive scan results container")
model ComprehensiveScanResults {
  @doc("JPA scan results")
  jpaResults?: JpaProjectScanResult;
  
  @doc("Bean validation scan results")
  beanValidationResults?: BeanValidationProjectScanResult;
  
  @doc("Servlet/JSP scan results")
  servletJspResults?: ServletJspProjectScanResult;
  
  @doc("Generic scan results for other scanners")
  genericResults: Record<GenericProjectScanResult>;
  
  @doc("Scan timestamp")
  scanTimestamp: utcDateTime;
  
  @doc("Total scan duration in milliseconds")
  scanDurationMs: int32;
}

@doc("Map of scan type to scan results")
model ScanResultsMap {
  @doc("JPA scan results")
  "JPA_ANNOTATION_SCANNER"?: JpaProjectScanResult;
  
  @doc("Bean validation scan results")
  "BEAN_VALIDATION_SCANNER"?: BeanValidationProjectScanResult;
  
  @doc("Servlet/JSP scan results")
  "SERVLET_JSP_SCANNER"?: ServletJspProjectScanResult;
  
  @doc("Generic scan results")
  [key: string]: any;
}

@doc("Map of scan type to recipes")
model RecipesByScanTypeMap {
  @doc("Recipes for each scan type")
  [key: string]: RecipeDefinition[];
}
