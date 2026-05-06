# Jakarta Migration Deep JAR Scanning - Implementation Summary

## Overview
Implemented deep JAR/bytecode scanning feature for Jakarta EE migration analysis, following the TypeSpec specification in `spec/jar-scanning.tsp`.

## Implementation Details

### Module: premium-core-engine
**Package**: `adrianmikula.jakartamigration.jaranalysis`

### 1. Domain Models (`jaranalysis/domain/`)
- **`JarCompatibilityLevel.java`**: Enum (JAKARTA, JAVAX, MIXED, UNKNOWN, DUAL_COMPATIBLE) with `toNamespace()` conversion
- **`JarScanSignal.java`**: Immutable record with Builder pattern for extracted JAR signals
  - javax/jakarta class counts
  - API usage breakdown
  - Reflection strings
  - POM metadata flags
  - Automatic-Module-Name
  - Shaded packages detection
  - Test-only patterns
- **`JarCompatibilityReport.java`**: Complete JAR assessment result
  - Classification level with confidence score
  - Human-readable reasoning
  - Raw scan signal
  - Analysis timing
  - Cache flag
- **`JarScanOptions.java`**: Configurable scanning behavior
  - Metadata analysis toggle
  - Reflection analysis toggle
  - Early exit with thresholds
  - Shaded package detection
  - Test scope detection
  - Max classes per jar limit

### 2. Configuration (`jaranalysis/config/`)
- **`JarScanningConfig.java`**: Singleton config loader from YAML
  - Loads from `jar-scanning-weights.yaml`
  - Scoring weights (jakartaClassRef, javaxClassRef, apiCriticality, etc.)
  - Cache configuration (size, TTL, max JAR size)
  - Performance tuning (parallelism, max classes)
  - Feature flags for deep scanning
  - Methods: `createScanOptions()`, `isDeepScanningEnabled()`, etc.

### 3. Services (`jaranalysis/service/`)

#### **`BytecodeSignalExtractor.java`**
- ASM-based bytecode analysis
- Reuses patterns from `AsmBytecodeAnalyzer`
- Collects:
  - javax/jakarta class references
  - Critical API usage with categorization (servlet, persistence, cdi, etc.)
  - Reflection strings (LDC instructions)
- Visitor pattern for class scanning
- Respects maxClassesPerJar limit

#### **`MetadataSignalExtractor.java`**
- JAR metadata analysis
- Scans META-INF/maven/**/pom.xml and pom.properties
- Reads MANIFEST.MF for Automatic-Module-Name
- Detects javax/jakarta dependencies via text pattern matching
- Enhances existing JarScanSignal with metadata

#### **`ScoringEngine.java`**
- Configurable weighted scoring
- Formula: `(classRefs × weight) + (apiUsage × criticality) + metadataBonuses`
- Determines `JarCompatibilityLevel` based on thresholds
- Calculates confidence score (0.0-1.0)
- Generates human-readable reasons
- Internal `ScoringResult` record

#### **`DefaultJarCompatibilityScanner.java`**
- Orchestrates scanning pipeline
- **Caching**: Guava Cache with configurable size/TTL
- **Parallel processing**: ForkJoinPool with bounded parallelism
- **Memory-aware**: Falls back to sequential if low memory
- Methods:
  - `analyzeJar()` - single JAR analysis
  - `analyzeJars()` - batch parallel analysis  
  - `analyzeArtifact()` - resolve + scan by Maven coordinate
  - `getCachedResult()` / `clearCache()` / `getCacheStats()`
  - `resolveJar()` - JAR resolution via existing JarResolver
- Graceful error handling (corrupt JARs → UNKNOWN)

#### **`JarCompatibilityScanner.java`**
- TypeSpec interface implementation
- Methods: `analyzeJar()`, `analyzeJars()`, `getCachedResult()`, `clearCache()`, `getCacheStats()`

### 4. Classifier (`jaranalysis/classifier/`)
- **`BytecodeNamespaceClassifier.java`**
  - Implements `NamespaceClassifier` (community interface)
  - **Two-phase classification**:
    1. Fast coordinate-based (SimpleNamespaceClassifier)
    2. Deep bytecode scanning if needed (UNKNOWN/MIXED/forced)
  - Translation: `JarCompatibilityLevel` → `Namespace`
  - Configurable deep scanning (feature flags, system properties)
  - In-memory result cache
  - Methods:
    - `classify()` - standard NamespaceClassifier
    - `classifyWithScanning()` - with detailed ClassificationResult
    - `classifyAll()` / `classifyAllWithScanning()` - batch operations
  - **Inner class**: `ClassificationResult`
    - Artifact, namespace, confidence
    - Reasoning list
    - Deep scan flag
    - Optional JarCompatibilityReport

### 5. Feature Flag
- **`FeatureFlag.java`** (community-core-engine): Added `JAR_SCANNING` enum value
  - Key: `"jar-scanning"`
  - Name: "Deep JAR Scanning"
  - Tier: PREMIUM

### 6. Plugin Integration
- **`MigrationAnalysisService.java`** (premium-intellij-plugin):
  - Replaced `SimpleNamespaceClassifier` with `BytecodeNamespaceClassifier`
  - Updated log message to indicate deep bytecode scanning

### 7. TypeSpec Specification
- **`spec/jar-scanning.tsp`**: Complete API specification
  - Models: JarCompatibilityLevel, JarScanSignal, JarCompatibilityReport, JarScanOptions, ClassificationResult
  - Interfaces: JarCompatibilityScanner, BytecodeNamespaceClassifier
  - Aligned with Java implementation

### 8. Configuration File
- **`jar-scanning-weights.yaml`** (resources/)
  - Scoring weights for all signal types
  - API criticality multipliers (servlet=2.0, persistence=2.0, cdi=1.5, etc.)
  - Thresholds: JAKARTA≥5, JAVAX≤-5, MIXED_MAX<4
  - Feature flags: caching, parallelism, early exit, shaded detection
  - Cache config: 1000 entries, 24h TTL, 50MB max JAR
  - Performance: 4 parallel threads, unlimited classes/jar

## Design Decisions

1. **Reuse Existing Code**: Bytecode extraction reuses ASM patterns from `AsmBytecodeAnalyzer`
2. **Immutable Models**: All domain objects use records/final fields with Builders
3. **Graceful Degradation**: Missing JARs, corrupt files → UNKNOWN with explanation
4. **Caching Strategy**: Two-level (in-memory Guava cache + artifact+sha1 key)
5. **Feature Flag Integration**: Deep scanning controlled via `FeatureFlag.JAR_SCANNING` and config
6. **Non-Breaking**: Community `NamespaceClassifier` interface unchanged
7. **Premium-Only**: All new code in premium-core-engine / premium-intellij-plugin

## Build Verification
- ✅ `premium-core-engine:compileJava` - SUCCESS
- ✅ `premium-intellij-plugin:compileJava` - SUCCESS
- ✅ All new files follow project conventions
- ✅ No dependencies on premium modules from community code
- ✅ Feature flag properly integrated

## Key Features
- Multi-signal analysis (bytecode + metadata)
- Configurable scoring weights (YAML)
- Parallel batch processing
- Intelligent caching
- Graceful error handling
- Feature flag controlled
- Premium feature (enterprise licensing)
- TypeSpec specification for API documentation
