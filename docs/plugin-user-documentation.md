# Jakarta Migration (javax → jakarta) IntelliJ Plugin

Technical documentation for senior engineers planning Java EE to Jakarta EE migrations.

---

## 1. Overview

### What Problem This Plugin Solves

After Oracle donated Java EE to the Eclipse Foundation, the namespace changed from `javax.*` to `jakarta.*`. This breaking change affects:
- Import statements
- Fully-qualified class references  
- Annotations (JPA, Bean Validation, CDI, etc.)
- Maven/Gradle dependencies
- Application server configurations

The plugin provides static analysis to identify migration scope, dependency conflicts, and automated refactoring capabilities via OpenRewrite.

### What It Actually Does

**Core Analysis (Community Tier):**
- Parses Maven `pom.xml` and Gradle build files to detect `javax.*` dependencies
- Queries Maven Central for Jakarta-compatible version recommendations
- Builds dependency graphs showing module relationships
- Calculates migration risk scores based on dependency complexity
- Provides migration strategy comparison (Big Bang, Incremental, Strangler Fig, etc.)

**Advanced Analysis (Premium Tier):**
- Source code scanning for 15+ Jakarta EE API categories:
  - JPA annotations (`@Entity`, `@Table`, `@Column`, etc.)
  - Bean Validation (`@NotNull`, `@Valid`, etc.)
  - Servlet/JSP API usages
  - CDI injection points (`@Inject`, `@Named`)
  - JAX-RS/JAX-WS endpoints
  - JMS messaging
  - Security annotations
  - Configuration files (`persistence.xml`, `web.xml`)
  - Third-party library compatibility detection
  - Reflection-based `javax` usage
- Application server platform detection (Tomcat, WildFly, Jetty, etc.)

**Refactoring (Premium Tier):**
- OpenRewrite recipe execution for automated code transformation
- 100+ predefined recipes covering major Jakarta EE specifications
- Recipe execution history with undo capability
- SQLite-backed persistence for tracking changes

**AI Integration:**
- MCP (Model Context Protocol) server for JetBrains AI Assistant integration
- Tools for `analyzeJakartaReadiness`, `analyzeMigrationImpact`, `applyOpenRewriteRefactoring`

---

## 2. How It Works (Technical)

### Architecture Overview

```
IntelliJ Plugin (premium-intellij-plugin/)
├── UI Layer (Swing components)
│   ├── MigrationToolWindow (JTabbedPane with 10+ tabs)
│   ├── DashboardComponent (risk score, metrics)
│   ├── DependenciesTableComponent
│   ├── DependencyGraphComponent (visualization)
│   ├── AdvancedScansComponent (premium)
│   ├── RefactorTabComponent (premium)
│   └── ...
├── Service Layer
│   ├── MigrationAnalysisService (core analysis)
│   ├── AdvancedScanningService (15+ scanners)
│   ├── DefaultMcpClientService (MCP protocol)
│   └── RecipeService (refactoring)
└── MCP Integration
    └── JakartaMcpRegistrationActivity (auto-registration)

Core Engine (premium-core-engine/ & community-core-engine/)
├── Dependency Analysis Module
│   ├── MavenDependencyGraphBuilder (parses pom.xml)
│   ├── NamespaceClassifier (JDK vs Jakarta vs safe)
│   ├── JakartaMappingService (artifact mappings)
│   └── ImprovedMavenCentralLookupService
├── Advanced Scanning Module (15 scanners)
│   ├── BaseScanner (parallel processing, memory-aware)
│   ├── JpaAnnotationScannerImpl (JavaParser-based)
│   ├── BuildConfigScannerImpl (pom.xml/gradle regex)
│   └── ...
└── Code Refactoring Module
    ├── RecipeServiceImpl
    ├── OpenRewriteRecipeExecutor (isolated classloader)
    └── RefactoringEngine
```

### Scanning Implementation

**Dependency Analysis:**
- Uses regex patterns to parse `pom.xml` and `build.gradle` files
- No Maven/Gradle API integration—file-based parsing only
- Queries Maven Central REST API for version recommendations
- Caches results with 5-minute SoftReference cache

**Source Code Scanning:**
- Uses OpenRewrite's `JavaParser` for AST-based analysis
- ThreadLocal JavaParser instances (cleaned up post-scan to prevent memory leaks)
- Bounded parallelism (max 4 threads, configurable via `advanced.scan.parallelism`)
- Memory-aware fallback: switches to sequential processing when <100MB available

**Namespace Classification:**
- `CompatibilityConfigLoader` classifies artifacts into:
  - `JDK_PROVIDED`: `javax.management`, `javax.sql`, etc. (no migration needed)
  - `JAKARTA_REQUIRED`: `javax.servlet`, `javax.persistence` (must migrate)
  - `CONTEXT_DEPENDENT`: `javax.xml.bind`, `javax.mail` (review required)
  - `UNKNOWN`: Requires manual investigation

### PSI/AST Usage

The plugin does **not** use IntelliJ's PSI (Program Structure Interface) for scanning. Instead:
- File-based parsing with JavaParser for source files
- Regex pattern matching for build files and configuration
- Direct file I/O via `Files.readString()` and `Files.walk()`

This design choice enables:
- Scanning without requiring IntelliJ indexing to complete
- Lower memory footprint for large projects
- Execution outside IntelliJ (MCP server mode)

---

## 3. Installation

**Requirements:**
- IntelliJ IDEA 2023.3+ (Community or Ultimate)
- Java 11+ for the plugin itself
- Project must use Maven or Gradle

**Installation Methods:**
1. JetBrains Marketplace (search: "Jakarta Migration")
2. Manual: Download JAR from releases, install via `Settings → Plugins → Install from Disk`

---

## 4. Usage Workflow

### Accessing the Plugin

**Primary Entry Point:**
- Tool Window: `View → Tool Windows → Jakarta Migration` (anchor: right sidebar)
- Menu Action: `Tools → Jakarta Migration` (shortcut: `Ctrl+Shift+J`)
- Editor Context Menu: `Analyze with AI Assistant`

### Standard Workflow

1. **Open Project:** Ensure project is loaded in IntelliJ with Maven/Gradle structure

2. **Launch Analysis:**
   - Click `▶ Analyze Project` in the tool window toolbar
   - Or use `Tools → Jakarta Migration → Analyze Readiness`

3. **Review Dashboard:**
   - Risk score (0-100) displayed with visual dial
   - Estimated migration time (hours/days)
   - Summary counts: total dependencies, Jakarta-compatible, blockers, no Jakarta version

4. **Inspect Dependencies Tab:**
   - Table view of all dependencies
   - Migration status per dependency (Compatible, Needs Upgrade, No Jakarta Version, etc.)
   - Maven Central lookup status (pending/failed/completed)

5. **Review Dependency Graph:**
   - Visual graph of module relationships
   - Hierarchical and force-directed layout options
   - Color-coding by Jakarta compatibility

6. **Evaluate Migration Strategy:**
   - Six strategies compared: Big Bang, Incremental, Parallel, Strangler Fig, Microservices, Hybrid
   - Each shows estimated time, risk level, pros/cons

7. **Premium: Run Advanced Scans:**
   - Click scan buttons for specific API categories
   - Results populate dashboard counters
   - Individual scan results viewable per category

8. **Premium: Apply Refactoring:**
   - Navigate to Refactor tab
   - Select OpenRewrite recipes (e.g., "Migrate JPA to Jakarta")
   - Click Apply (with confirmation dialog)
   - View changed files in History tab

9. **Verify:**
   - Re-run analysis to confirm migration progress
   - Check that risk score decreased

---

## 5. What Is Detected

### Dependencies (Build Files)

**Parsed Files:**
- `pom.xml` (Maven)
- `build.gradle`, `build.gradle.kts` (Gradle)
- `Dockerfile` (base image detection)

**Detection Patterns:**
- Maven: `<groupId>javax.xxx</groupId>` dependencies
- Gradle: `implementation 'javax.xxx:yyy:version'` syntax
- Known library mappings:
  - `javax.servlet:javax.servlet-api` → `jakarta.servlet:jakarta.servlet-api`
  - `javax.persistence:javax.persistence-api` → `jakarta.persistence:jakarta.persistence-api`
  - `javax.validation:validation-api` → `jakarta.validation:jakarta.validation-api`
  - `javax.ws.rs:javax.ws.rs-api` → `jakarta.ws.rs:jakarta.ws.rs-api`
  - `javax.jms:javax.jms-api` → `jakarta.jms:jakarta.jms-api`
  - `javax.xml.bind:jaxb-api` → `jakarta.xml.bind:jakarta.xml.bind-api`

### Source Code Annotations

**JPA (Premium):**
`@Entity`, `@Table`, `@Column`, `@Id`, `@GeneratedValue`, `@OneToMany`, `@ManyToOne`, `@JoinColumn`, `@Temporal`, `@Enumerated`, `@Lob`, `@Transient`, `@Version`, `@Embedded`, `@Embeddable`, `@MappedSuperclass`, `@Inheritance`, `@DiscriminatorColumn`, `@SequenceGenerator`, `@TableGenerator`, `@PersistenceContext`, `@PersistenceUnit`, `@Cacheable`, `@Converter`, `@Convert`, `@ElementCollection`

**Bean Validation (Premium):**
`@NotNull`, `@NotEmpty`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`, `@Digits`, `@Positive`, `@PositiveOrZero`, `@Negative`, `@NegativeOrZero`, `@Pattern`, `@Email`, `@Valid`, `@AssertTrue`, `@AssertFalse`, `@Future`, `@Past`, `@FutureOrPresent`, `@PastOrPresent`, `@Groups`, `@GroupSequence`

**CDI/Injection (Premium):**
`@Inject`, `@Named`, `@Singleton`, `@ApplicationScoped`, `@RequestScoped`, `@SessionScoped`, `@ConversationScoped`, `@Dependent`, `@Produces`, `@Qualifier`, `@Interceptor`, `@Decorator`, `@Observes`

**Servlet/JSP (Premium):**
`@WebServlet`, `@WebFilter`, `@WebListener`, `@ServletSecurity`, `@HttpConstraint`, `@HttpMethodConstraint`, `@MultipartConfig`, `@WebInitParam`

**JAX-RS (Premium):**
`@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`, `@HEAD`, `@OPTIONS`, `@PATCH`, `@Produces`, `@Consumes`, `@QueryParam`, `@PathParam`, `@HeaderParam`, `@FormParam`, `@CookieParam`, `@MatrixParam`, `@Context`, `@ApplicationPath`, `@Provider`

### Configuration Files (Premium)

**Detected Files:**
- `persistence.xml` (JPA configuration)
- `web.xml` (Servlet configuration)
- `beans.xml` (CDI configuration)
- `validation.xml` (Bean Validation configuration)
- `jax-ws-catalog.xml` (JAX-WS)
- `application.properties` / `application.yml` (Spring Boot Jakarta properties)
- `jboss-web.xml`, `jboss-deployment-structure.xml` (WildFly)
- `glassfish-web.xml` (Payara/GlassFish)
- `jetty-web.xml`, `jetty-env.xml` (Jetty)
- `context.xml` (Tomcat)

### Application Servers (Premium)

**Detected Platforms:**
- Tomcat (version detection for Jakarta compatibility)
- WildFly/JBoss EAP
- Jetty
- Payara/GlassFish
- Open Liberty
- WebSphere
- WebLogic

---

## 6. What Is NOT Supported

### Dependency Management

**Not Implemented:**
- Automatic `pom.xml`/`build.gradle` modification
- Dependency version bumping in build files
- Transitive dependency tree resolution (only direct deps scanned)
- Gradle Kotlin DSL full parsing (basic support only)

**Limitations:**
- Version recommendations are lookups from Maven Central, not calculated based on your dependency tree
- No automatic conflict resolution for version clashes
- BOM (Bill of Materials) dependencies are not specially handled

### Source Code Refactoring

**Not Implemented:**
- Fully-qualified class name replacements in non-Java files (e.g., XML, properties)
- String literals containing class names
- ClassLoader-based detection of dynamically loaded classes
- Native code (JNI) references

**OpenRewrite Limitations:**
- Requires well-formed Java source (won't fix syntax errors)
- Cannot handle Lombok-generated code reliably
- Some complex generics scenarios may not transform correctly

### Runtime Analysis

**Experimental/Premium Only:**
- Runtime tab exists but marked experimental
- No runtime bytecode analysis of deployed applications
- No integration with running application servers for live checking

### Framework-Specific Edge Cases

**Not Covered:**
- Spring Boot's `javax` property mappings (only standard Jakarta properties)
- Hibernate-specific annotations beyond JPA standard
- Framework-specific configuration (Spring XML, Dropwizard, etc.)
- MicroProfile annotations (different migration path)

### Build System Support

**Not Supported:**
- Ant-based projects
- sbt (Scala) projects
- Bazel projects
- Projects without standard Maven/Gradle structure

---

## 7. Recommended Migration Workflow (Enterprise)

### Phase 1: Assessment (Week 1)

1. **Baseline Analysis:**
   - Install plugin, run full analysis
   - Export PDF report (Premium) for stakeholder review
   - Document current risk score and blocker count

2. **Dependency Review:**
   - Sort Dependencies table by "No Jakarta Version"
   - Identify critical blockers (libraries without Jakarta equivalent)
   - Check for internal/shared libraries that need migration first

3. **Strategy Selection:**
   - Use Migration Strategy tab to compare approaches
   - For monoliths >100k LOC: recommend Incremental or Strangler Fig
   - For microservices: Big Bang may be viable per service

### Phase 2: Preparation (Weeks 2-3)

1. **Build Updates:**
   - Manually update `pom.xml`/`build.gradle` based on plugin recommendations
   - Update application server version (Tomcat 9→10, WildFly 26→27+)
   - Add Jakarta EE BOM for version management

2. **Test Infrastructure:**
   - Ensure CI/CD can build with Jakarta dependencies
   - Set up parallel test environment
   - Create rollback plan

### Phase 3: Automated Refactoring (Weeks 4-5)

1. **Recipe Application (Premium):**
   - Start with high-impact, low-risk recipes:
     - `JavaxPersistenceToJakartaPersistence`
     - `JavaxServletToJakartaServlet`
     - `JavaxValidationToJakartaValidation`
   - Apply per-module, not all at once
   - Use undo if issues detected

2. **Verification:**
   - Run analysis after each recipe batch
   - Verify risk score decreases
   - Run full test suite

### Phase 4: Manual Remediation (Weeks 6-8)

1. **Handle Edge Cases:**
   - Custom `javax` string references in code
   - Configuration files the plugin doesn't modify
   - Framework-specific migration steps

2. **Integration Testing:**
   - Full application server deployment tests
   - End-to-end workflow verification

### Phase 5: Validation (Week 9)

1. **Final Analysis:**
   - Re-run full scan, verify 0 remaining `javax` usages
   - Confirm all dependencies show "Migrated" or "Compatible"

2. **Documentation:**
   - Archive migration report
   - Document any manual workarounds applied

---

## 8. Performance Characteristics

### Scanning Performance

**Dependency Analysis:**
- Time: ~1-5 seconds for projects with <100 dependencies
- File I/O bound (reads pom.xml/build.gradle files only)
- No network calls during initial parse (Maven Central lookup is async)

**Advanced Scanning (Premium):**
- Time: 10-60 seconds depending on project size
- Memory: Bounded to 100MB threshold per scan
- Parallelism: Max 4 threads (configurable via `advanced.scan.parallelism`)
- Disk I/O: Walks entire project directory tree for `.java` files

**Memory Usage:**
- SoftReference caching for scan results (5-minute TTL)
- ThreadLocal cleanup after each scan to prevent parser accumulation
- Sequential fallback when <100MB heap available

### Refactoring Performance

**OpenRewrite Execution:**
- Time: 5-30 seconds per recipe application
- Memory: Isolated classloader prevents PermGen issues
- Disk I/O: Full project file tree walk for pattern matching

### UI Responsiveness

**Asynchronous Operations:**
- All scans run on background threads (`CompletableFuture.supplyAsync()`)
- UI updates via `ApplicationManager.getApplication().invokeLater()`
- Progress indicators on dashboard during scans

### Scaling Characteristics

| Project Size | Dependency Scan | Advanced Scan | Refactor (1 recipe) |
|--------------|-------------------|---------------|---------------------|
| Small (<10k LOC) | <1s | 5-10s | 3-5s |
| Medium (10-100k) | 1-3s | 10-30s | 5-15s |
| Large (100k-1M) | 3-5s | 30-60s | 15-30s |
| Very Large (>1M) | 5-10s | 60-120s | 30-60s |

---

## 9. Known Edge Cases

### Jackson Serialization

**Issue:** Jackson's `ObjectMapper` may be loaded from different classloaders causing `ClassCastException` during recipe execution.

**Workaround:** Plugin uses isolated classloader with `FilteringClassLoader` to prevent Jackson version conflicts. Recipes execute in sandboxed environment.

### Lombok-Generated Code

**Issue:** OpenRewrite may not transform references in Lombok-generated methods (equals, hashCode, toString).

**Mitigation:** Manual review required for Lombok-heavy classes. Delombok first if necessary.

### Custom ClassLoaders

**Issue:** Plugins or frameworks with custom ClassLoaders (OSGi, custom plugin systems) may have `javax` references the scanner cannot detect.

**Detection:** Reflection Usage Scanner attempts to catch `Class.forName()` calls with `javax` packages.

### Gradle Kotlin DSL

**Issue:** `build.gradle.kts` parsing is limited compared to Groovy DSL.

**Mitigation:** Some dependencies may be missed. Manual review of Kotlin build files recommended.

### Transitive Dependencies

**Issue:** Scanner only examines direct dependencies in build files. Transitive `javax` deps are flagged by TransitiveDependencyScanner but may produce false positives.

**Verification:** Run `mvn dependency:tree` or `gradle dependencies` to verify actual transitive tree.

### Spring Boot Properties

**Issue:** Plugin detects standard Jakarta properties in `application.properties` but may miss custom Spring-specific mappings.

**Manual Check:** Search for `spring.*.javax` property patterns.

### Multi-Module Projects

**Issue:** Analysis aggregates all modules but may not correctly identify cross-module dependencies as blockers.

**Recommendation:** Run analysis from root module, review Dependency Graph for module relationships.

---

## 10. FAQ

**Q: Does the plugin modify my source files automatically?**
A: No. The Community tier is read-only. Premium tier refactoring requires explicit user action (click Apply) with confirmation dialog.

**Q: Can I use the plugin without the MCP server?**
A: Yes. All analysis and refactoring work without the MCP server. The MCP server only enables AI Assistant integration.

**Q: Does the plugin require a paid license?**
A: No. Dependency analysis, version recommendations, and migration strategy comparison are free. Premium features (refactoring, advanced scans, PDF reports) require subscription or 7-day trial.

**Q: Will the plugin break my build?**
A: The analysis phase is non-destructive. Refactoring (Premium) modifies files—use version control and the undo feature.

**Q: How accurate are the version recommendations?**
A: Recommendations come from Maven Central metadata. Verify against your framework's documentation (Spring Boot 3.x, Jakarta EE 9/10 compatibility matrices).

**Q: Can I run this in CI/CD?**
A: The plugin is IDE-only. For CI/CD, use the underlying OpenRewrite recipes directly via Maven/Gradle plugin.

**Q: What Jakarta EE version does it target?**
A: Recipes target Jakarta EE 9+ (namespace change). Jakarta EE 10+ features are not specifically handled.

**Q: Does it handle Java EE 7 or earlier?**
A: Detection works for any `javax.*` usage, but recommendations assume Java EE 8 baseline. Earlier versions may require additional migration steps.

**Q: Can I contribute custom recipes?**
A: Recipes are loaded from `recipes.json`. Custom recipe support is a Premium feature via the `CUSTOM_RECIPES` feature flag.

**Q: Why does the scan take so long on my project?**
A: First scan builds caches. Subsequent scans use SoftReference cache (5-minute TTL). Large projects (>1M LOC) will take 1-2 minutes. Consider reducing `advanced.scan.parallelism` if memory-constrained.

---

## Configuration Reference

**System Properties:**

| Property | Default | Description |
|----------|---------|-------------|
| `jakarta.migration.premium` | `false` | Force premium mode (testing only) |
| `jakarta.mcp.server.url` | `http://localhost:8080` | MCP server endpoint |
| `jakarta.mcp.timeout.connection` | `30` | MCP connection timeout (seconds) |
| `jakarta.mcp.timeout.request` | `300` | MCP request timeout (seconds) |
| `advanced.scan.parallelism` | `4` | Max parallel scan threads |

**Feature Flags (Premium):**

Feature flags are controlled via JetBrains Marketplace licensing. Trial period is 7 days. Monthly/yearly pricing is configurable by vendor.

---

*Document Version: 1.0.11*
*Last Updated: 9th April 2026*
