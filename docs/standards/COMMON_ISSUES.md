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

## IntelliJ Plugin Issues

### [To be added]

---

## Gradle Build Issues

### [To be added]

---

## Testing Issues

### [To be added]

