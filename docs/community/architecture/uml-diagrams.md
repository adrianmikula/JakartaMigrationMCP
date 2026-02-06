# UML Diagrams for Jakarta Migration MCP Core Modules

This document contains PlantUML diagrams visualizing the architecture of the three core modules.

---

## Module 1: Dependency Analysis Module

### Class Diagram

```plantuml
@startuml DependencyAnalysisModule
!theme plain
skinparam classAttributeIconSize 0

package "Dependency Analysis Module" {
    
    interface DependencyAnalysisModule {
        + analyzeProject(projectPath: String): DependencyAnalysisReport
        + identifyNamespaces(graph: DependencyGraph): NamespaceCompatibilityMap
        + detectBlockers(graph: DependencyGraph): List<Blocker>
        + recommendVersions(artifacts: List<Artifact>): List<VersionRecommendation>
        + analyzeTransitiveConflicts(graph: DependencyGraph): TransitiveConflictReport
    }
    
    class DependencyAnalyzer {
        - mavenParser: MavenParser
        - gradleParser: GradleParser
        - namespaceClassifier: NamespaceClassifier
        - compatibilityMatrix: CompatibilityMatrix
        + buildDependencyGraph(projectPath: String): DependencyGraph
        + classifyNamespaces(graph: DependencyGraph): NamespaceCompatibilityMap
    }
    
    class NamespaceClassifier {
        - rules: List<ClassificationRule>
        + classify(artifact: Artifact): NamespaceType
        + detectMixedNamespaces(artifact: Artifact): Boolean
        + checkJakartaCompatibility(artifact: Artifact): CompatibilityResult
    }
    
    class CompatibilityMatrix {
        - mappings: Map<Artifact, JakartaMapping>
        + getJakartaEquivalent(artifact: Artifact): Artifact
        + getMigrationPath(artifact: Artifact): MigrationPath
        + isKnownCompatible(artifact: Artifact): Boolean
    }
    
    class BlockerDetector {
        - mlModel: BlockerDetectionModel
        + detectBlockers(graph: DependencyGraph): List<Blocker>
        + analyzeTransitiveConflicts(graph: DependencyGraph): List<Conflict>
        + assessRisk(artifact: Artifact): RiskScore
    }
    
    class VersionRecommender {
        - mlModel: VersionRecommendationModel
        - compatibilityMatrix: CompatibilityMatrix
        + recommendVersions(artifacts: List<Artifact>): List<VersionRecommendation>
        + findJakartaVersion(artifact: Artifact): Version
    }
    
    class DependencyGraph {
        - nodes: Set<Artifact>
        - edges: Set<Dependency>
        + getArtifacts(): Set<Artifact>
        + getDependencies(artifact: Artifact): Set<Dependency>
        + getTransitiveDependencies(artifact: Artifact): Set<Artifact>
    }
    
    class Artifact {
        + groupId: String
        + artifactId: String
        + version: Version
        + namespace: NamespaceType
        + packages: Set<String>
    }
    
    class DependencyAnalysisReport {
        + dependencyGraph: DependencyGraph
        + namespaceMap: NamespaceCompatibilityMap
        + blockers: List<Blocker>
        + recommendations: List<VersionRecommendation>
        + riskAssessment: RiskAssessment
        + readinessScore: MigrationReadinessScore
    }
    
    class Blocker {
        + artifact: Artifact
        + type: BlockerType
        + reason: String
        + mitigationStrategies: List<String>
        + confidence: double
    }
    
    DependencyAnalysisModule <|.. DependencyAnalyzer
    DependencyAnalyzer --> DependencyGraph
    DependencyAnalyzer --> NamespaceClassifier
    DependencyAnalyzer --> CompatibilityMatrix
    DependencyAnalyzer --> BlockerDetector
    DependencyAnalyzer --> VersionRecommender
    NamespaceClassifier --> CompatibilityMatrix
    BlockerDetector --> DependencyGraph
    VersionRecommender --> CompatibilityMatrix
    DependencyGraph --> Artifact
    DependencyAnalysisReport --> DependencyGraph
    DependencyAnalysisReport --> Blocker
}

@enduml
```

### Sequence Diagram: Dependency Analysis Flow

```plantuml
@startuml DependencyAnalysisSequence
!theme plain

actor User
participant "MCP Client" as Client
participant "DependencyAnalysisModule" as Module
participant "DependencyAnalyzer" as Analyzer
participant "NamespaceClassifier" as Classifier
participant "BlockerDetector" as Detector
participant "VersionRecommender" as Recommender
participant "CompatibilityMatrix" as Matrix

User -> Client: analyze_jakarta_readiness(projectPath)
Client -> Module: analyzeProject(projectPath)

Module -> Analyzer: buildDependencyGraph(projectPath)
Analyzer -> Analyzer: Parse pom.xml / build.gradle
Analyzer -> Analyzer: Build dependency tree
Analyzer --> Module: DependencyGraph

Module -> Classifier: identifyNamespaces(graph)
Classifier -> Matrix: getJakartaEquivalent(artifact)
loop For each artifact
    Classifier -> Classifier: classify(artifact)
    Classifier -> Matrix: checkCompatibility(artifact)
end
Classifier --> Module: NamespaceCompatibilityMap

Module -> Detector: detectBlockers(graph)
Detector -> Detector: Analyze transitive dependencies
Detector -> Detector: Check for known blockers
Detector -> Detector: Assess risk (ML model)
Detector --> Module: List<Blocker>

Module -> Recommender: recommendVersions(artifacts)
Recommender -> Matrix: getJakartaEquivalent(artifact)
Recommender -> Recommender: Predict optimal version (ML)
Recommender --> Module: List<VersionRecommendation>

Module -> Module: Build report
Module --> Client: DependencyAnalysisReport
Client --> User: Report with blockers & recommendations

@enduml
```

---

## Module 2: Code Refactoring Module

### Class Diagram

```plantuml
@startuml CodeRefactoringModule
!theme plain
skinparam classAttributeIconSize 0

package "Code Refactoring Module" {
    
    interface CodeRefactoringModule {
        + createMigrationPlan(projectPath: String, report: DependencyAnalysisReport): MigrationPlan
        + refactorBatch(files: List<FilePath>, recipes: List<Recipe>, options: RefactoringOptions): RefactoringResult
        + getProgress(projectPath: String): MigrationProgress
        + validateRefactoring(file: FilePath, changes: RefactoringChanges): ValidationResult
        + rollback(file: FilePath, checkpointId: String): RollbackResult
    }
    
    class MigrationPlanner {
        - orderPredictor: RefactoringOrderPredictor
        - riskAssessor: RiskAssessor
        + createPlan(projectPath: String, report: DependencyAnalysisReport): MigrationPlan
        + determineOptimalOrder(files: List<FilePath>): List<FilePath>
        + calculateBatchSizes(files: List<FilePath>): Map<FilePath, Integer>
    }
    
    class RefactoringEngine {
        - javaParser: JavaParser
        - recipeExecutor: RecipeExecutor
        - changeTracker: ChangeTracker
        + refactorFile(file: FilePath, recipes: List<Recipe>): RefactoringResult
        + refactorBatch(files: List<FilePath>, recipes: List<Recipe>): RefactoringResult
        + applyRecipe(cu: CompilationUnit, recipe: Recipe): CompilationUnit
    }
    
    class RecipeExecutor {
        - recipeLibrary: RecipeLibrary
        + execute(recipe: Recipe, cu: CompilationUnit): CompilationUnit
        + validate(recipe: Recipe, cu: CompilationUnit): ValidationResult
    }
    
    class RecipeLibrary {
        - recipes: Map<String, Recipe>
        + getRecipe(name: String): Recipe
        + getJakartaRecipes(): List<Recipe>
        + registerRecipe(recipe: Recipe): void
    }
    
    class ChangeTracker {
        - checkpoints: Map<String, Checkpoint>
        + createCheckpoint(file: FilePath, changes: RefactoringChanges): String
        + getCheckpoint(checkpointId: String): Checkpoint
        + rollback(checkpointId: String): RollbackResult
    }
    
    class ProgressTracker {
        - progress: MigrationProgress
        + updateProgress(file: FilePath, status: RefactoringStatus): void
        + getProgress(): MigrationProgress
        + calculateStatistics(): Statistics
    }
    
    class RefactoringOrderPredictor {
        - mlModel: OrderPredictionModel
        + predictOptimalOrder(files: List<FilePath>, graph: DependencyGraph): List<FilePath>
        + estimateDuration(file: FilePath): Duration
    }
    
    class RiskAssessor {
        - mlModel: RiskPredictionModel
        + assessRisk(file: FilePath, recipe: Recipe): RiskScore
        + predictIssues(file: FilePath): List<PotentialIssue>
    }
    
    class MigrationPlan {
        + phases: List<RefactoringPhase>
        + fileSequence: List<FilePath>
        + estimatedDuration: Duration
        + overallRisk: RiskAssessment
    }
    
    class RefactoringResult {
        + refactoredFiles: List<FilePath>
        + failures: List<RefactoringFailure>
        + statistics: Statistics
        + checkpointId: String
        + canRollback: Boolean
    }
    
    class Recipe {
        + name: String
        + description: String
        + pattern: String
        + safety: SafetyLevel
    }
    
    CodeRefactoringModule <|.. RefactoringEngine
    RefactoringEngine --> MigrationPlanner
    RefactoringEngine --> RecipeExecutor
    RefactoringEngine --> ChangeTracker
    RefactoringEngine --> ProgressTracker
    MigrationPlanner --> RefactoringOrderPredictor
    MigrationPlanner --> RiskAssessor
    RecipeExecutor --> RecipeLibrary
    MigrationPlan --> RefactoringPhase
    RefactoringResult --> RefactoringFailure
}

@enduml
```

### Sequence Diagram: Refactoring Flow

```plantuml
@startuml RefactoringSequence
!theme plain

actor User
participant "MCP Client" as Client
participant "CodeRefactoringModule" as Module
participant "MigrationPlanner" as Planner
participant "RefactoringEngine" as Engine
participant "RecipeExecutor" as Executor
participant "ChangeTracker" as Tracker
participant "ProgressTracker" as Progress

User -> Client: refactor_namespace(projectPath, options)
Client -> Module: createMigrationPlan(projectPath, report)

Module -> Planner: createPlan(projectPath, report)
Planner -> Planner: Analyze dependency graph
Planner -> Planner: Predict optimal order (ML)
Planner -> Planner: Calculate batch sizes
Planner --> Module: MigrationPlan

Module --> Client: MigrationPlan
Client -> User: Show plan for approval

User -> Client: approve_and_start()
Client -> Module: refactorBatch(files, recipes, options)

loop For each batch
    Module -> Engine: refactorBatch(batchFiles, recipes)
    
    loop For each file
        Engine -> Executor: execute(recipe, compilationUnit)
        Executor -> Executor: Apply OpenRewrite recipe
        Executor --> Engine: Refactored compilationUnit
        
        Engine -> Tracker: createCheckpoint(file, changes)
        Tracker --> Engine: checkpointId
        
        Engine -> Progress: updateProgress(file, COMPLETED)
    end
    
    Engine -> Engine: Validate batch
    Engine --> Module: RefactoringResult
    Module -> Progress: updateStatistics(result)
end

Module -> Progress: getProgress()
Progress --> Module: MigrationProgress
Module --> Client: RefactoringResult + Progress
Client --> User: Show progress and results

@enduml
```

---

## Module 3: Runtime Verification Module

### Class Diagram

```plantuml
@startuml RuntimeVerificationModule
!theme plain
skinparam classAttributeIconSize 0

package "Runtime Verification Module" {
    
    interface RuntimeVerificationModule {
        + verifyRuntime(jarPath: String, options: VerificationOptions): VerificationResult
        + analyzeErrors(errors: List<RuntimeError>, context: MigrationContext): ErrorAnalysis
        + performStaticAnalysis(projectPath: String, graph: DependencyGraph): StaticAnalysisResult
        + instrumentClassLoading(jarPath: String, options: InstrumentationOptions): ClassLoaderAnalysisResult
        + performHealthCheck(applicationUrl: String, options: HealthCheckOptions): HealthCheckResult
    }
    
    class ProcessExecutor {
        - processBuilder: ProcessBuilder
        - timeout: Duration
        + executeJar(jarPath: String, options: ExecutionOptions): ExecutionResult
        + monitorProcess(process: Process): ProcessMetrics
        + captureOutput(process: Process): ProcessOutput
    }
    
    class ErrorAnalyzer {
        - errorClassifier: ErrorClassifier
        - rootCausePredictor: RootCausePredictor
        + analyzeErrors(errors: List<RuntimeError>): ErrorAnalysis
        + classifyError(error: RuntimeError): ErrorCategory
        + predictRootCause(error: RuntimeError, context: MigrationContext): RootCause
    }
    
    class ErrorClassifier {
        - mlModel: ErrorClassificationModel
        - patternMatcher: ErrorPatternMatcher
        + classify(error: RuntimeError): ErrorCategory
        + matchPattern(error: RuntimeError): ErrorPattern
    }
    
    class ErrorPatternMatcher {
        - patterns: Map<String, ErrorPattern>
        + match(errorMessage: String): ErrorPattern
        + isJakartaRelated(error: RuntimeError): Boolean
    }
    
    class RootCausePredictor {
        - mlModel: RootCausePredictionModel
        + predict(error: RuntimeError, context: MigrationContext): RootCause
        + findSimilarFailures(error: RuntimeError): List<SimilarFailure>
    }
    
    class StaticAnalyzer {
        - bytecodeAnalyzer: BytecodeAnalyzer
        - classpathAnalyzer: ClasspathAnalyzer
        + analyze(projectPath: String, graph: DependencyGraph): StaticAnalysisResult
        + scanBytecode(jarPath: String): BytecodeReport
        + checkClasspathConflicts(classpath: List<String>): List<Conflict>
    }
    
    class ClassLoaderInstrumenter {
        - agent: ClassLoaderAgent
        + instrument(jarPath: String, options: InstrumentationOptions): ClassLoaderAnalysisResult
        + monitorClassLoading(): ClassLoadingEvents
        + detectFailures(events: ClassLoadingEvents): List<ClassLoadingFailure>
    }
    
    class HealthCheckValidator {
        - httpClient: HttpClient
        + checkHealth(url: String): HealthCheckResult
        + validateEndpoints(endpoints: List<String>): List<EndpointStatus>
    }
    
    class VerificationResult {
        + status: VerificationStatus
        + errors: List<RuntimeError>
        + warnings: List<Warning>
        + metrics: ExecutionMetrics
        + analysis: ErrorAnalysis
        + remediationSteps: List<RemediationStep>
    }
    
    class RuntimeError {
        + type: ErrorType
        + message: String
        + stackTrace: StackTrace
        + className: String
        + timestamp: LocalDateTime
    }
    
    RuntimeVerificationModule <|.. ProcessExecutor
    ProcessExecutor --> ErrorAnalyzer
    ErrorAnalyzer --> ErrorClassifier
    ErrorAnalyzer --> RootCausePredictor
    ErrorClassifier --> ErrorPatternMatcher
    RuntimeVerificationModule <|.. StaticAnalyzer
    RuntimeVerificationModule <|.. ClassLoaderInstrumenter
    RuntimeVerificationModule <|.. HealthCheckValidator
    VerificationResult --> RuntimeError
    VerificationResult --> ErrorAnalysis
}

@enduml
```

### Sequence Diagram: Runtime Verification Flow

```plantuml
@startuml RuntimeVerificationSequence
!theme plain

actor User
participant "MCP Client" as Client
participant "RuntimeVerificationModule" as Module
participant "ProcessExecutor" as Executor
participant "ErrorAnalyzer" as Analyzer
participant "ErrorClassifier" as Classifier
participant "RootCausePredictor" as Predictor
participant "StaticAnalyzer" as Static

User -> Client: verify_runtime(jarPath, options)
Client -> Module: verifyRuntime(jarPath, options)

alt Primary: Process Execution
    Module -> Executor: executeJar(jarPath, options)
    Executor -> Executor: Start process
    Executor -> Executor: Monitor stdout/stderr
    Executor -> Executor: Capture exit code
    
    alt Errors Detected
        Executor -> Executor: Parse errors from logs
        Executor --> Module: List<RuntimeError>
        
        Module -> Analyzer: analyzeErrors(errors, context)
        Analyzer -> Classifier: classifyError(error)
        Classifier -> Classifier: Match error patterns
        Classifier -> Classifier: ML classification
        Classifier --> Analyzer: ErrorCategory
        
        Analyzer -> Predictor: predictRootCause(error, context)
        Predictor -> Predictor: ML prediction
        Predictor -> Predictor: Find similar failures
        Predictor --> Analyzer: RootCause
        
        Analyzer --> Module: ErrorAnalysis
    end
    
    Executor --> Module: ExecutionResult
else Fallback: Static Analysis
    Module -> Static: performStaticAnalysis(projectPath, graph)
    Static -> Static: Analyze bytecode
    Static -> Static: Check classpath conflicts
    Static -> Static: Detect namespace issues
    Static --> Module: StaticAnalysisResult
end

Module -> Module: Build verification result
Module --> Client: VerificationResult
Client --> User: Show errors, analysis, and remediation steps

@enduml
```

---

## Integrated Module Interaction

### Sequence Diagram: Complete Migration Flow

```plantuml
@startuml CompleteMigrationFlow
!theme plain

actor User
participant "MCP Client" as Client
participant "DependencyAnalysisModule" as DepModule
participant "CodeRefactoringModule" as RefactorModule
participant "RuntimeVerificationModule" as RuntimeModule
participant "NeuroSymbolicEngine" as Engine

User -> Client: migrate_to_jakarta(projectPath)

== Phase 1: Dependency Analysis ==
Client -> DepModule: analyzeProject(projectPath)
DepModule -> DepModule: Build dependency graph
DepModule -> DepModule: Classify namespaces
DepModule -> DepModule: Detect blockers
DepModule -> Engine: Validate predictions
Engine --> DepModule: Validated report
DepModule --> Client: DependencyAnalysisReport

== Phase 2: Migration Planning ==
Client -> RefactorModule: createMigrationPlan(projectPath, report)
RefactorModule -> Engine: Predict optimal order
Engine --> RefactorModule: Refactoring sequence
RefactorModule --> Client: MigrationPlan

== Phase 3: Incremental Refactoring ==
loop For each phase
    Client -> RefactorModule: refactorBatch(files, recipes)
    RefactorModule -> RefactorModule: Apply OpenRewrite recipes
    RefactorModule -> RefactorModule: Track changes
    RefactorModule --> Client: RefactoringResult
    
    == Phase 4: Runtime Verification ==
    Client -> RuntimeModule: verifyRuntime(jarPath)
    RuntimeModule -> RuntimeModule: Execute JAR
    RuntimeModule -> RuntimeModule: Analyze errors
    
    alt Errors Found
        RuntimeModule -> Engine: Analyze errors
        Engine -> Engine: Correlate with migration
        Engine --> RuntimeModule: ErrorAnalysis + Remediation
        RuntimeModule --> Client: VerificationResult with fixes
        
        Client -> RefactorModule: apply_fixes(remediationSteps)
        RefactorModule --> Client: Fixed
    else No Errors
        RuntimeModule --> Client: VerificationResult (SUCCESS)
    end
end

Client --> User: Migration Complete

@enduml
```

---

## Neuro-Symbolic Engine Architecture

### Class Diagram

```plantuml
@startuml NeuroSymbolicEngine
!theme plain
skinparam classAttributeIconSize 0

package "Neuro-Symbolic Engine" {
    
    class NeuroSymbolicEngine {
        - symbolicKnowledge: SymbolicKnowledgeBase
        - neuralModels: NeuralModelRegistry
        - validator: PredictionValidator
        - explainer: DecisionExplainer
        - learner: FeedbackLearner
        + validate(prediction: NeuralPrediction, rules: SymbolicRules): ValidatedPrediction
        + explain(decision: Decision, trace: RuleTrace): Explanation
        + learn(outcome: Outcome, prediction: Prediction): void
    }
    
    class SymbolicKnowledgeBase {
        - compatibilityRules: CompatibilityMatrix
        - refactoringRecipes: RecipeLibrary
        - errorPatterns: ErrorPatternLibrary
        - bestPractices: BestPracticeRules
        + getRules(context: Context): List<Rule>
        + validate(prediction: Prediction): ValidationResult
    }
    
    class NeuralModelRegistry {
        - compatibilityModel: CompatibilityModel
        - orderModel: OrderPredictionModel
        - riskModel: RiskAssessmentModel
        - errorModel: ErrorClassificationModel
        + getModel(type: ModelType): MLModel
        + updateModel(model: MLModel, feedback: Feedback): void
    }
    
    class PredictionValidator {
        + validate(prediction: NeuralPrediction, rules: SymbolicRules): ValidationResult
        + checkConsistency(prediction: Prediction, context: Context): ConsistencyResult
    }
    
    class DecisionExplainer {
        - ruleTracer: RuleTracer
        + explain(decision: Decision): Explanation
        + generateTrace(decision: Decision): RuleTrace
    }
    
    class FeedbackLearner {
        - trainingData: TrainingDataset
        + learn(outcome: Outcome, prediction: Prediction): void
        + updateModel(model: MLModel, feedback: Feedback): void
    }
    
    class ValidatedPrediction {
        + prediction: NeuralPrediction
        + validation: ValidationResult
        + confidence: double
        + explanation: Explanation
    }
    
    class Explanation {
        + decision: Decision
        + ruleTrace: RuleTrace
        + confidence: double
        + humanReadable: String
    }
    
    NeuroSymbolicEngine --> SymbolicKnowledgeBase
    NeuroSymbolicEngine --> NeuralModelRegistry
    NeuroSymbolicEngine --> PredictionValidator
    NeuroSymbolicEngine --> DecisionExplainer
    NeuroSymbolicEngine --> FeedbackLearner
    PredictionValidator --> ValidatedPrediction
    DecisionExplainer --> Explanation
}

@enduml
```

---

*Last Updated: 2026-01-27*

