# Jakarta Migration IntelliJ Plugin - Amazon Q Development Rules

## 🎯 TypeSpec-First Development Methodology

### Core Principle: TypeSpec as Single Source of Truth
Amazon Q must enforce that ALL code implementation strictly follows the TypeSpec specifications located in the `spec/` directory. These specifications define the authoritative contracts for data models, interfaces, and component structures.

### 📋 Mandatory TypeSpec Compliance Rules

#### Rule 1: Pre-Implementation TypeSpec Consultation
- **BEFORE writing any Java class, interface, or enum, ALWAYS reference the corresponding TypeSpec file**
- **NEVER implement any data structure without first locating its TypeSpec definition**
- **If a TypeSpec definition doesn't exist, CREATE it first before implementation**

#### Rule 2: Exact TypeSpec Mapping
```java
// ✅ REQUIRED: Exact field mapping from TypeSpec
// From spec/intellij-plugin-ui.tsp: MigrationDashboard model
public class MigrationDashboard {
    @JsonProperty("readinessScore")
    private Integer readinessScore;  // TypeSpec: int32
    
    @JsonProperty("status")
    private MigrationStatus status;  // TypeSpec: MigrationStatus enum
    
    @JsonProperty("dependencySummary")
    private DependencySummary dependencySummary;  // TypeSpec: DependencySummary model
    
    @JsonProperty("currentPhase")
    private MigrationPhase currentPhase;  // TypeSpec: MigrationPhase model
    
    @JsonProperty("lastAnalyzed")
    private Instant lastAnalyzed;  // TypeSpec: utcDateTime
}
```

#### Rule 3: Enum String Value Compliance
```java
// ✅ REQUIRED: Exact enum values from TypeSpec
// From spec/intellij-plugin-ui.tsp: MigrationStatus enum
public enum MigrationStatus {
    NOT_ANALYZED("NOT_ANALYZED"),
    READY("READY"),
    HAS_BLOCKERS("HAS_BLOCKERS"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");
    
    private final String value;
    
    MigrationStatus(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
}
```

### 🧪 Test-Driven Development with TypeSpec Validation

#### TDD Rule 1: TypeSpec-Based Test Cases
```java
// ✅ REQUIRED: Tests must validate TypeSpec compliance
@Test
void testDependencyInfoMatchesTypeSpec() {
    // Test all fields from spec/intellij-plugin-ui.tsp: DependencyInfo model
    DependencyInfo dependency = new DependencyInfo();
    dependency.setGroupId("com.example");
    dependency.setArtifactId("example-lib");
    dependency.setCurrentVersion("1.0.0");
    dependency.setRecommendedVersion("2.0.0");
    dependency.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
    dependency.setBlocker(true);
    dependency.setRiskLevel(RiskLevel.MEDIUM);
    dependency.setMigrationImpact("Breaking changes in API");
    
    // Validate all TypeSpec-defined fields are present and correct
    assertThat(dependency.getGroupId()).isEqualTo("com.example");
    assertThat(dependency.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);
    assertThat(dependency.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    assertThat(dependency.isBlocker()).isTrue();
}
```

### 🔒 Implementation Enforcement Rules

#### Enforcement Rule 1: MCP Integration Compliance
```java
// ✅ REQUIRED: MCP client must match spec/mcp-integration.tsp
public interface McpClientService {
    // Exact method signatures from TypeSpec
    AnalyzeJakartaReadinessResponse analyzeJakartaReadiness(AnalyzeJakartaReadinessRequest request);
    DetectBlockersResponse detectBlockers(DetectBlockersRequest request);
    RecommendVersionsResponse recommendVersions(RecommendVersionsRequest request);
    AnalyzeMigrationImpactResponse analyzeMigrationImpact(AnalyzeMigrationImpactRequest request);
    CreateMigrationPlanResponse createMigrationPlan(CreateMigrationPlanRequest request);
    ExecuteMigrationPlanResponse executeMigrationPlan(ExecuteMigrationPlanRequest request);
    ApplyAutoFixesResponse applyAutoFixes(ApplyAutoFixesRequest request);
}
```

### 🚫 Prohibited Practices

#### Prohibition 1: No Custom Fields
```java
// ❌ FORBIDDEN: Adding fields not in TypeSpec
public class MigrationDashboard {
    private Integer readinessScore;  // ✅ In TypeSpec
    private String customField;     // ❌ NOT in TypeSpec - FORBIDDEN
}
```

### 📋 Amazon Q Code Review Checklist

When reviewing or generating code, Amazon Q must verify:

#### ✅ Data Model Compliance:
- [ ] Every Java class has corresponding TypeSpec model
- [ ] All field names match TypeSpec exactly
- [ ] All field types match TypeSpec types (int32 → Integer, utcDateTime → Instant)
- [ ] All optional fields are properly marked with @Nullable or Optional<>
- [ ] All required fields are present and non-null

#### ✅ Enum Compliance:
- [ ] All enum values use exact TypeSpec string representations
- [ ] All TypeSpec enum values are implemented
- [ ] No additional enum values not in TypeSpec
- [ ] Proper @JsonValue annotation for serialization

### 🎯 Success Metrics

#### Code is TypeSpec-compliant when:
- ✅ 100% of data models have TypeSpec definitions
- ✅ All enum values match TypeSpec strings exactly
- ✅ All component structures follow TypeSpec models
- ✅ All MCP operations match TypeSpec interfaces
- ✅ Tests validate TypeSpec compliance

---

**CRITICAL: The TypeSpec files in `spec/` are the AUTHORITATIVE SOURCE for all implementation. Amazon Q must never generate code that deviates from these specifications without explicit TypeSpec updates first.**