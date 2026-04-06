# IntelliJ Plugin Compatibility Range Investigation

## Summary

Problem: The IntelliJ plugin's compatibility range showed as "233.0 — 233.*" on the JetBrains Marketplace, which was worse than the original v1.0.0 (233.0 — 243.*).

## What We Know

1. **Build Number Format**: IntelliJ uses branch numbers:
   - 233 = 2023.1+
   - 241 = 2024.1, 242 = 2024.2, 243 = 2024.3
   - 251 = 2025.1, 252 = 2025.2, 253 = 2025.3

2. **Original v1.0.0**: Had compatibility range `233.0 — 243.*`

3. **Marketplace Limit**: IntelliJ Marketplace enforces a maximum `until-build` of `243.*`. Values greater than this are rejected.

4. **Open-Ended Compatibility**: To support all future versions (2024, 2025, 2026+), the `until-build` attribute should be omitted entirely from plugin.xml.

## What We Tried

| Approach | Result |
|----------|--------|
| Setting `intellij.untilBuild=243.*` | Rejected by Marketplace (claimed max was 243.*, but upload failed) |
| Setting `intellij.untilBuild=253.*` | Error: "You cannot set an until-build value greater than 243.*" |
| Removing `intellij.untilBuild` from gradle.properties | Gradle plugin defaulted to `233.*` in generated XML |
| Using `providers.gradleProperty(...).orNull?.let { }` | Same default behavior - plugin defaults to `233.*` |
| Setting `untilBuild.set("")` (empty string) | **Success** - removes attribute entirely |

## Root Cause

The IntelliJ Gradle Plugin (`org.jetbrains.intellij`) has a default value for `untilBuild` of `233.*` when the property is not explicitly set to something other than null/empty.

Simply not setting the property in gradle.properties does NOT result in no attribute - it results in the default `233.*`.

## Solution

In `premium-intellij-plugin/build.gradle.kts`, explicitly set `untilBuild` to an empty string:

```kotlin
patchPluginXml {
    sinceBuild.set(providers.gradleProperty("intellij.sinceBuild").orElse("233"))
    untilBuild.set(providers.gradleProperty("intellij.untilBuild").orElse(""))
}
```

This generates:
```xml
<idea-version since-build="233" />
```

Instead of:
```xml
<idea-version since-build="233" until-build="233.*" />
```

The empty string causes the attribute to be omitted entirely, giving open-ended compatibility with all future IntelliJ versions.

## Configuration (gradle.properties)

```properties
# IntelliJ Plugin Version Compatibility
intellij.sinceBuild=233
# intellij.untilBuild intentionally not set - handled via empty string in build.gradle.kts
```

## References

- [Build Number Ranges - IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html)
- [Gradle IntelliJ Plugin - patchPluginXml](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#patchpluginxml)
