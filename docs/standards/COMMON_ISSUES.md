# Common Issues and Gotchas

This document catalogs common issues, gotchas, and pitfalls that developers should be aware of when working on the Jakarta Migration project.

## Java/Lombok Issues

### Duplicate Constructors with @AllArgsConstructor

**Problem:**  
When a Java class has both `@AllArgsConstructor` annotation from Lombok AND explicit constructors defined in the class, the compiler generates an error like:

```
error: constructor ClassName(...) is already defined in class ClassName
```

**Cause:**  
Lombok's `@AllArgsConstructor` generates a constructor with all fields as parameters. If you also define explicit constructors in the class, you'll have duplicate constructors with the same signature.

**Solution:**  
Remove the `@AllArgsConstructor` annotation and keep only the explicit constructors:

```java
// WRONG - causes duplicate constructor error
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor  // <-- REMOVE THIS
public class ExampleClass {
    private final String field1;
    private final int field2;
    
    public ExampleClass(String field1, int field2) {
        this.field1 = field1;
        this.field2 = field2;
    }
}

// CORRECT - explicit constructors only
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class ExampleClass {
    private final String field1;
    private final int field2;
    
    public ExampleClass(String field1, int field2) {
        this.field1 = field1;
        this.field2 = field2;
    }
}
```

**Files Affected in This Project:**
- `ClassloaderModuleScanResult.java`
- `ConfigFileScanResult.java`
- `JmsMessagingScanResult.java`
- `SerializationCacheScanResult.java`
- `ClassloaderModuleProjectScanResult.java`
- `LoggingMetricsProjectScanResult.java`
- `LoggingMetricsScanResult.java`
- `ThirdPartyLibProjectScanResult.java`
- `SecurityApiProjectScanResult.java`
- `SecurityApiScanResult.java`
- `AppServerProjectScanResult.java`

**Prevention:**  
- Avoid using `@AllArgsConstructor` when you need custom constructor logic
- Use `@RequiredArgsConstructor` for constructors with only `final` fields
- If you need both `@AllArgsConstructor` and custom constructors, remove the custom ones and use the generated one

---

## JSON/Jackson Issues

### JSON Deserialization Error - Unrecognized Property

**Problem:**  
When loading saved plugin state, Jackson throws an error like:

```
com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: Unrecognized field "jakartaCompatible" 
(class adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact)
```

**Cause:**  
The saved JSON contains fields that no longer exist in the current class definitions. This happens when:
1. The domain model changes (fields added/removed)
2. Old saved state still contains the old field names
3. Jackson tries to deserialize and fails on unknown properties

**Solution:**  
Add `@JsonIgnoreProperties(ignoreUnknown = true)` to domain classes:

```java
// CORRECT - Jackson will ignore unknown properties
@JsonIgnoreProperties(ignoreUnknown = true)
public record Artifact(
        String groupId,
        String artifactId,
        String version,
        String scope,
        boolean transitive) {
    // ...
}
```

**Files That Should Have This Annotation:**
- All domain model classes that are serialized/deserialized
- `Artifact.java`
- `DependencyGraph.java`  
- `DependencyAnalysisReport.java`
- Any record/class used in persistence

**Alternative Solutions:**
1. Add the missing field to the class (if it's still relevant)
2. Use Jackson's versioned annotations (`@JsonVersion`)
3. Clear saved state when schema changes significantly

**Prevention:**
- Always add `@JsonIgnoreProperties(ignoreUnknown = true)` to new domain classes
- Document schema changes in CHANGELOG
- Consider using schema versioning for persisted data

**Reference:**  
See `docs/community/standards/json-deserialization-errors.md` for detailed guidance.

---

## IntelliJ Plugin Issues

### [To be added]

---

## Gradle Build Issues

### IntelliJ Platform Gradle Plugin v1.x Incompatible with Java 25 / Gradle 9.0

**Problem:**  
When using Java 25 with Gradle 8.5 and IntelliJ Platform Gradle Plugin 1.x, the build fails with:

```
Type org.gradle.api.internal.plugins.DefaultArtifactPublicationSet not present
```

Or with Java 25:
```
JavaVersion.parse() failure with JDK 25
```

**Cause:**  
1. The IntelliJ Platform Gradle Plugin 1.x (`org.jetbrains.intellij`) is incompatible with Gradle 9.0+
2. Java 25 has breaking changes in `java.lang.System` that cause warnings/errors
3. Java 25's `JavaVersion.parse()` may fail with certain version strings

**Solution (Current Working Configuration):**  
Use Gradle 8.5 with Java 21 (not Java 25):

1. Set JAVA_HOME to Java 21 when running Gradle:
```bash
JAVA_HOME="C:\Program Files\Java\jdk-21.0.10" ./gradlew build
```

2. Or configure in `gradle.properties`:
```properties
org.gradle.java.home=C\:\\Program Files\\Java\\jdk-21.0.10
```

**Note:** Java 25 support requires:
- Gradle 8.13+ (which has Kotlin 2.0+)
- IntelliJ Platform Gradle Plugin 2.x
- This combination was tested but introduced new issues

---

### Kotlin Compiler Incompatible with Java 25

**Problem:**  
When running Gradle with Java 25, the Kotlin compiler fails with:
```
java.lang.IllegalArgumentException: 25.0.2
    at org.jetbrains.kotlin.com.intellij.util.lang.JavaVersion.parse(JavaVersion.java:307)
```

**Cause:**  
The Kotlin compiler bundled with Gradle 8.5/8.13 uses an older version of `JavaVersion.parse()` that doesn't handle Java 25's version string format (25.0.2).

**Solution:**  
Use Java 21 for running Gradle builds. The project code can still target Java 17/21 via toolchain configuration.

---

### [To be added]

---

## Testing Issues

---

## Related Documentation

See also:
- [Tech Debt Documentation](../techdebt/README.md) - Known limitations and technical debt
- [IntelliJ Compatibility Research](../premium/investigations/intellij-compatibility-range.md) - IntelliJ version compatibility details