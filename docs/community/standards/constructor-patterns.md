# Constructor Patterns: Use and Avoid

This document outlines critical patterns to use and avoid when working with constructors in Java, particularly for Lombok-annotated classes and data transfer objects.

## 📋 Table of Contents
1. [Problem Context](#problem-context)
2. [Patterns to Use](#patterns-to-use)
3. [Patterns to Avoid](#patterns-to-avoid)
4. [Best Practices](#best-practices)
5. [Common Gotchas](#common-gotchas)
6. [Verification Checklist](#verification-checklist)

## 🔧 Problem Context

We encountered a critical constructor issue with `TransitiveDependencyUsage` where:
- Lombok's `@AllArgsConstructor` was generating a constructor
- Manual constructor definitions conflicted with Lombok
- Instantiation calls in `TransitiveDependencyScannerImpl` didn't match expected signatures
- Final fields required proper initialization

This caused compilation failures and runtime errors.

## 🛠️ Patterns to Use

### ✅ Use Lombok Annotations Correctly
```java
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TransitiveDependencyUsage {
    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String javaxPackage;
    private final String severity;
    private final String recommendation;
}
```

**Why:** Lombok handles constructor generation consistently and avoids manual conflicts.

### ✅ Use Explicit Constructor When Needed
```java
@Getter
public class TransitiveDependencyUsage {
    public TransitiveDependencyUsage(String artifactId, String groupId, String version,
                                    String javaxPackage, String severity, String recommendation) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        this.javaxPackage = javaxPackage;
        this.severity = severity;
        this.recommendation = recommendation;
    }
    
    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String javaxPackage;
    private final String severity;
    private final String recommendation;
}
```

**Why:** Full control over constructor implementation and validation.

### ✅ Handle Optional Fields Properly
```java
public TransitiveDependencyUsage(String artifactId, String groupId, String version,
                                String javaxPackage, String severity, String recommendation) {
    this.artifactId = artifactId;
    this.groupId = groupId;
    this.version = version != null ? version : "unknown";
    this.javaxPackage = javaxPackage;
    this.severity = severity;
    this.recommendation = recommendation;
}
```

**Why:** Prevents null pointer exceptions and provides defaults.

### ✅ Use Builder Pattern for Complex Objects
```java
@Builder
@Getter
public class TransitiveDependencyUsage {
    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String javaxPackage;
    private final String severity;
    private final String recommendation;
}
```

**Why:** Clean syntax for complex object creation with optional fields.

## 🚫 Patterns to Avoid

### ❌ Don't Mix Lombok and Manual Constructors
```java
// AVOID - This causes conflicts
@Getter
@AllArgsConstructor
public class TransitiveDependencyUsage {
    public TransitiveDependencyUsage() {} // Manual no-arg constructor conflicts
    private final String artifactId;
    private final String groupId;
    // ... other fields
}
```

**Why:** Lombok generates constructors automatically, leading to duplicate or conflicting constructors.

### ❌ Don't Use Final Fields Without Proper Initialization
```java
// AVOID - Final fields must be initialized
@Getter
public class TransitiveDependencyUsage {
    private final String artifactId; // ERROR: Not initialized
    private final String groupId;    // ERROR: Not initialized
}
```

**Why:** Final fields require initialization in constructors or at declaration.

### ❌ Don't Call Constructors with Wrong Parameter Count
```java
// AVOID - Constructor signature mismatch
TransitiveDependencyUsage usage = new TransitiveDependencyUsage(
    artifactId, groupId, version, severity, recommendation
); // ERROR: 5 args, needs 6
```

**Why:** Compilation fails when argument count doesn't match constructor signature.

### ❌ Don't Use Null for Mandatory Fields
```java
// AVOID - Null for mandatory fields
TransitiveDependencyUsage usage = new TransitiveDependencyUsage(
    null, null, null, null, null, null
); // ERROR: Mandatory fields cannot be null
```

**Why:** Business logic typically requires valid values for mandatory fields.

## 🔧 Best Practices

### 🔗 Consistency
- Use consistent constructor patterns across similar classes
- Document constructor requirements in class Javadoc
- Follow project's Lombok usage guidelines

### 🔄 Testing
- Write unit tests for constructors
- Test null handling and validation
- Verify constructor behavior with edge cases

### 📝 Documentation
- Document constructor parameters and their requirements
- Note any special validation or transformation
- Include examples of proper instantiation

### 🔒 Validation
- Validate constructor parameters for business rules
- Throw meaningful exceptions for invalid inputs
- Consider using validation frameworks for complex rules

## 🔄 Common Gotchas

### 📁 Import Conflicts
- Ensure correct Lombok imports
- Avoid mixing manual and Lombok-generated code
- Check for duplicate constructors

### 🔧 IDE Synchronization
- Refresh IDE after Lombok changes
- Ensure Lombok plugin is installed and enabled
- Check for IDE-specific compilation issues

### 📈 Build Tool Configuration
- Verify Lombok is in compile classpath
- Check annotation processor configuration
- Ensure build tools recognize Lombok annotations

### 🔄 Testing Environment
- Include Lombok in test dependencies
- Configure test annotation processing
- Verify test compilation works correctly

## 🔍 Verification Checklist

Before committing constructor-related code:

### ✅ Basic Verification
- [ ] Constructor signature matches usage
- [ ] All final fields initialized
- [ ] No duplicate constructors
- [ ] Proper null handling

### ✅ Lombok Verification
- [ ] Correct Lombok annotations used
- [ ] IDE Lombok plugin enabled
- [ ] Build tool recognizes Lombok

### ✅ Usage Verification
- [ ] All instantiations match constructor signature
- [ ] Parameter types are correct
- [ ] Optional fields handled properly

### ✅ Testing Verification
- [ ] Constructor unit tests exist
- [ ] Null cases tested
- [ ] Edge cases covered

### ✅ Documentation Verification
- [ ] Constructor documented
- [ ] Parameter requirements clear
- [ ] Examples provided

## 🚀 Quick Reference

| Pattern | When to Use | When to Avoid |
|--------|-------------|---------------|
| Lombok @AllArgsConstructor | Simple DTOs, quick implementation | Complex validation, custom logic |
| Manual constructor | Custom validation, complex logic | Simple objects, quick implementation |
| Builder pattern | Objects with many optional fields | Simple objects with few fields |
| No-arg constructor | Framework requirements, serialization | Business objects with mandatory fields |

## 🔄 Related Patterns

- [Builder Pattern](https://refactoring.guru/design-patterns/builder)
- [Factory Pattern](https://refactoring.guru/design-patterns/factory-method)
- [Validation Patterns](https://refactoring.guru/design-patterns/strategy)

## 📚 References

- [Lombok Documentation](https://projectlombok.org/features/all)
- [Effective Java - Item 2: Consider a builder when faced with many constructor parameters](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/)
- [Java Constructor Best Practices](https://www.baeldung.com/java-constructor-best-practices)

---

**Last Updated:** 2026-02-27  
**Version:** 1.0  
**Author:** Jakarta Migration Team  
**Reviewers:** Adrian Mikula, Kilo AI Assistant