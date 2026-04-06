# Technical Debt

This document tracks technical debt and known limitations in the Jakarta Migration plugin.

---

## IntelliJ Platform Compatibility

### product-descriptor Element Not Supported in IntelliJ 2023.x

**Date:** 2026-03-02

**Issue:**  
The `<product-descriptor>` element in `plugin.xml` (required for freemium/paid plugins on JetBrains Marketplace) is only supported in IntelliJ IDEA **2025.1+**.

**Current Configuration:**
- Target IntelliJ version: 2023.3.4
- Freemium implementation: Runtime licensing via `CheckLicense.java` using `LicensingFacade` API

**Impact:**  
- Plugin works correctly on all IDE versions via runtime licensing
- Marketplace shows freemium badge at platform level (not plugin level)
- No `<product-descriptor>` element in plugin.xml for now

**Resolution:**  
Current approach is correct. Runtime licensing via `LicensingFacade` API is the recommended approach per JetBrains documentation. The `<product-descriptor>` element is optional - it provides enhanced marketplace integration but is not required.

**Future Consideration:**  
If we want to use `<product-descriptor>`, we would need to:
1. Upgrade target IntelliJ version to 2025.1+
2. This would exclude users on older IDE versions (2023.x, 2024.x)

**Reference:**
- [JetBrains Plugin Configuration File Docs](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html)
- [Freemium Plugins Documentation](https://plugins.jetbrains.com/docs/marketplace/freemium.html)

---

## Gradle/Java Compatibility Issues

### Java 25 Not Supported by Gradle 8.5 Kotlin Compiler

**Date:** 2026-03-02

**Issue:**  
Gradle 8.5 bundled Kotlin compiler cannot parse Java 25 version strings (e.g., "25.0.2").

**Error:**
```
java.lang.IllegalArgumentException: 25.0.2
    at org.jetbrains.kotlin.com.intellij.util.lang.JavaVersion.parse(JavaVersion.java:307)
```

**Current Configuration:**
- Gradle: 8.5
- Java for Gradle: 21
- Java for compilation: 17 (via toolchain)

**Workaround:**  
Set `JAVA_HOME` to Java 21 when running Gradle builds:
```bash
JAVA_HOME="C:\Program Files\Java\jdk-21.0.10" ./gradlew build
```

---

### IntelliJ Platform Gradle Plugin v1.x Incompatible with Gradle 9.0

**Date:** 2026-03-02

**Issue:**  
When attempting to upgrade to Gradle 9.0 to support Java 25, the IntelliJ Platform Gradle Plugin 1.x (`org.jetbrains.intellij`) fails with:

```
Type org.gradle.api.internal.plugins.DefaultArtifactPublicationSet not present
```

**Attempted Solutions:**

1. **Upgrade to IntelliJ Platform Gradle Plugin 2.x**
   - Changed to `org.jetbrains.intellij.platform` plugin
   - Updated configuration syntax from `intellij {}` to `intellijPlatform {}`
   - Updated to plugin version 2.11.0
   - This worked but introduced new issues

2. **Upgrade to Gradle 8.13/8.14**
   - Gradle 8.13 has Kotlin 2.0.21 which supports Java 25
   - However, introduced new compatibility issues

**Current Resolution:**  
Stayed with Gradle 8.5 and Java 21 for running Gradle. This is the stable working configuration.

**Future Consideration:**  
When Java 25 support is more mature:
- Upgrade to Gradle 9.0+
- Upgrade to IntelliJ Platform Gradle Plugin 2.x
- Target IntelliJ 2024.3+ (which has native Java 21/25 support)

---

### Tested Version Combinations

| Gradle | IntelliJ Plugin | Java (Gradle) | Java (Compile) | Result |
|--------|-----------------|---------------|----------------|--------|
| 8.5    | 1.17.2         | 21            | 17             | ✅ Working |
| 8.13   | 1.17.2         | 21            | 17             | ❌ Kotlin version issue |
| 8.13   | 2.11.0         | 21            | 17             | ❌ New issues |
| 8.14   | 2.0.0          | 21            | 17             | ❌ New issues |
| 8.5    | 1.17.2         | 25            | 17             | ❌ Kotlin parse error |
| 8.13   | 2.11.0         | 25            | 17             | ❌ New issues |

**Recommendation:** Stay with Gradle 8.5 + IntelliJ Plugin 1.17.2 + Java 21 until ecosystem stabilizes.

**Reference:**  
See `docs/standards/COMMON_ISSUES.md` for more details.

---

## Build Configuration

### Single Build for All IntelliJ Versions

**Decision:**  
We use a single plugin build that targets IntelliJ 2023.3.4 with open-ended compatibility (`until-build` not set). This allows the plugin to work on all IntelliJ versions from 2023.3 onwards.

**Rationale:**
- Simpler build/release process
- Single artifact to maintain
- Runtime API handles version-specific features

---

*Last updated: 2026-03-02*
