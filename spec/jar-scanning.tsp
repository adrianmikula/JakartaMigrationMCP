import "@typespec/http";
import "@typespec/rest";
import "@typespec/openapi3";

using TypeSpec.Http;
using TypeSpec.Rest;

@service({
  title: "Jakarta Migration JAR Scanning Services",
  version: "1.0.0",
})
@doc("TypeSpec specification for deep JAR/bytecode scanning and namespace classification")
namespace JakartaMigrationJarScanning;

@doc("JAR compatibility classification result")
enum JarCompatibilityLevel {
  @doc("Fully Jakarta EE compatible (jakarta.* only)")
  JAKARTA: "JAKARTA",

  @doc("Legacy Java EE only (javax.* only)")
  JAVAX: "JAVAX",

  @doc("Mixed javax and jakarta usage, requires review")
  MIXED: "MIXED",

  @doc("Insufficient signal to determine compatibility")
  UNKNOWN: "UNKNOWN",

  @doc("Supports both javax and jakarta at runtime")
  DUAL_COMPATIBLE: "DUAL_COMPATIBLE"
}

@doc("Individual signal extracted from JAR analysis")
model JarScanSignal {
  @doc("Artifact coordinate (groupId:artifactId:version)")
  artifactCoordinate: string;

  @doc("Number of unique javax.* class references found")
  javaxClassRefs: int32;

  @doc("Number of unique jakarta.* class references found")
  jakartaClassRefs: int32;

  @doc("API usage breakdown (e.g., { servlet: 5, persistence: 3 })")
  apiUsage: Record<int32>;

  @doc("Reflection string literals found (e.g., 'javax.servlet')")
  reflectionStrings: string[];

  @doc("Maven pom.xml present inside JAR")
  hasPomMetadata: boolean;

  @doc("pom.xml indicates javax dependencies")
  pomIndicatesJavax: boolean;

  @doc("pom.xml indicates jakarta dependencies")
  pomIndicatesJakarta: boolean;

  @doc("Automatic-Module-Name from MANIFEST.MF")
  automaticModuleName?: string;

  @doc("Detected shaded/relocated packages")
  hasShadedPackages: boolean;

  @doc("Detected test-only usage patterns")
  testOnlyPatterns: string[];
}

@doc("Complete JAR compatibility assessment")
model JarCompatibilityReport {
  @doc("Artifact coordinate")
  artifactCoordinate: string;

  @doc("Classified compatibility level")
  level: JarCompatibilityLevel;

  @doc("Confidence score 0.0-1.0")
  confidence: float32;

  @doc("Human-readable explanation of classification")
  reasons: string[];

  @doc("Raw signals used for classification")
  signal: JarScanSignal;

  @doc("Analysis duration in milliseconds")
  analysisTimeMs: int32;

  @doc("Whether result is from cache")
  cached: boolean;
}

@doc("Options controlling JAR scanning behavior")
model JarScanOptions {
  @doc("Analyze Maven metadata (pom.xml, manifest)")
  analyzeMetadata: boolean = true;

  @doc("Scan constant pool for reflection strings")
  analyzeReflection: boolean = true;

  @doc("Stop early if strong signal found")
  earlyExitEnabled: boolean = true;

  @doc("Score threshold to trigger early exit")
  earlyExitThreshold: int32 = 10;

  @doc("Detect shaded/relocated packages")
  detectShaded: boolean = false;

  @doc("Detect test-scope dependencies")
  detectTestScope: boolean = true;

  @doc("Maximum classes to scan per JAR (0 = unlimited)")
  maxClassesPerJar: int32 = 0;
}

@doc("JAR compatibility scanning service")
@route("/api/jar-scanning")
interface JarCompatibilityScanner {
  @doc("Analyze a single JAR file for Jakarta compatibility")
  @post("analyze")
  analyzeJar(
    @doc("Path to JAR file")
    @query jarPath: string,
    @doc("Scanning options")
    @query options?: JarScanOptions
  ): JarCompatibilityReport;

  @doc("Batch analyze multiple JAR files")
  @post("analyze-batch")
  analyzeJars(
    @doc("List of JAR file paths")
    @query jarPaths: string[],
    @doc("Scanning options")
    @query options?: JarScanOptions
  ): JarCompatibilityReport[];

  @doc("Get cached result if available")
  @get("cached/{artifactCoordinate}")
  getCachedResult(
    @doc("Artifact coordinate (groupId:artifactId:version)")
    @path artifactCoordinate: string
  ): JarCompatibilityReport?;

  @doc("Clear the analysis cache")
  @post("cache/clear")
  clearCache(): boolean;

  @doc("Get cache statistics")
  @get("cache/stats")
  getCacheStats(): Record<string>;
}

@doc("Classification result with detailed reasoning")
model ClassificationResult {
  @doc("The artifact")
  artifact: Artifact;

  @doc("Classified namespace")
  namespace: Namespace;

  @doc("Confidence score")
  confidence: float32;

  @doc("Why this classification was made")
  reasoning: string[];

  @doc("Whether deep scanning was used")
  deepScanUsed: boolean;

  @doc("Full compatibility report if scanned")
  report?: JarCompatibilityReport;
}

@doc("Enhanced namespace classifier that uses deep JAR scanning")
interface BytecodeNamespaceClassifier extends NamespaceClassifier {
  @doc("Classify artifact using deep bytecode analysis if needed")
  @post("classify")
  classifyWithScanning(
    @doc("Artifact to classify")
    artifact: Artifact,
    @doc("Force deep scan even if coordinate-based classification is available")
    @query forceDeepScan: boolean = false
  ): ClassificationResult;

  @doc("Batch classify with optional deep scanning")
  @post("classify-batch")
  classifyAllWithScanning(
    @doc("Artifacts to classify")
    artifacts: Artifact[],
    @query forceDeepScan: boolean = false
  ): ClassificationResult[];
}
