# IntelliJ Marketplace Publishing Guide

**Date:** 2026-02-04  
**Purpose:** Checklist of requirements to publish the Jakarta Migration IntelliJ plugin to the JetBrains Marketplace

---

## Prerequisites

### 1. JetBrains Account
- [ ] Create a JetBrains Account at https://account.jetbrains.com
- [ ] Verify email address
- [ ] Accept the Marketplace Agreement

### 2. Organization/Developer Profile
- [ ] Set up developer profile with:
  - Developer name (e.g., "Adrian Mikula")
  - Logo/icon (256x256px PNG)
  - Website URL
  - Contact email

### 3. Plugin Metadata
- [ ] Unique plugin ID (e.g., `com.adrianmikula.jakarta-migration`)
- [ ] Plugin name: "Jakarta Migration"
- [ ] Vendor name matching your JetBrains account
- [ ] Plugin description (max 4000 characters)
- [ ] Change notes for this version

### 4. Screenshots/Videos
- [ ] 3-5 screenshots showing key features:
  - Migration analysis results
  - Dependency blocker detection
  - Migration plan visualization
  - Auto-fix results
- [ ] Optional: Demo video link

---

## Technical Requirements

### 1. Plugin Structure (intellij-plugin module)

#### Required Files:
- [ ] `plugin.xml` - Plugin configuration
- [ ] `META-INF/plugin.xml` - Embedded in JAR
- [ ] Icon files (optional but recommended):
  - `icons/pluginIcon.svg` (24x24)
  - `icons/pluginIcon_dark.svg` (24x24)

#### Plugin Icon Requirements:
- SVG format preferred
- 24x24 pixels at minimum
- Transparent background
- Not include version number

### 2. Plugin Configuration (plugin.xml)

```xml
<idea-plugin>
    <id>com.adrianmikula.jakarta-migration</id>
    <vendor email="your@email.com" name="Your Name"/>
    
    <product-descriptor code="JAKMIG" version="1.0.0"/>
    
    <description>
        <b>Jakarta Migration Plugin</b><br>
        <br>
        Analyzes Java projects for Jakarta EE migration readiness,
        detects blockers, and provides automated fixes for javax to jakarta namespace migration.
    </description>
    
    <change-notes>
        <b>Version 1.0.0</b><br>
        <ul>
            <li>Initial release</li>
            <li>Jakarta readiness analysis</li>
            <li>Dependency blocker detection</li>
            <li>Version recommendations</li>
            <li>Migration plan generation</li>
        </ul>
    </change-notes>
    
    <!-- Actions -->
    <actions>
        <!-- Register plugin actions here -->
    </actions>
    
    <!-- Extension points -->
</idea-plugin>
```

### 3. Build Configuration

#### Gradle Plugin:
```kotlin
// intellij-plugin/build.gradle.kts
plugins {
    id("org.jetbrains.intellij") version "1.17.4"
}

intellij {
    version.set("2024.1")
    type.set("IC") // IntelliJ IDEA Community
    
    plugins.set(listOf(
        "java",
        "javaStride"
    ))
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

### 4. Versioning Strategy

- **Semantic versioning**: `1.0.0`, `1.1.0`, `1.2.0`
- **Build number**: Auto-incremented by Gradle plugin
- **Compatible IntelliJ versions**: Specify minimum IDE build

#### Compatible IDE Versions:
- IntelliJ IDEA 2024.1+
- Android Studio (Arctic Fox+)
- Other JetBrains IDEs with Java support

---

## Pre-Submission Checklist

### 1. Testing
- [ ] Tests pass: `./gradlew test`
- [ ] Plugin builds successfully: `./gradlew buildPlugin`
- [ ] Integration tests pass
- [ ] Smoke test in IDEA Community Edition

### 2. Code Quality
- [ ] No unresolved Checkstyle issues
- [ ] No unresolved PMD issues
- [ ] Code coverage > 50% (recommended)
- [ ] No hardcoded credentials

### 3. Security
- [ ] No vulnerable dependencies (run `./gradlew dependencyCheck`)
- [ ] No malicious code
- [ ] No obfuscated code
- [ ] Third-party libraries listed

### 4. Documentation
- [ ] README.md updated with:
  - Plugin description
  - Installation instructions
  - Usage guide
  - Screenshots
- [ ] Changelog updated
- [ ] LICENSE file present (Apache 2.0)

---

## Submission Process

### 1. Prepare Release
```bash
# 1. Update version in gradle.properties
echo "version=1.0.0" >> gradle.properties

# 2. Update change notes in plugin.xml

# 3. Build plugin
./gradlew buildPlugin

# 4. Verify output
ls -la build/distributions/
```

### 2. Create GitHub Release (Optional but Recommended)
```bash
# Tag the release
git tag v1.0.0
git push origin v1.0.0

# Create release on GitHub with:
# - ZIP file from build/distributions/
# - Release notes
# - SHA-256 checksum
```

### 3. Upload to Marketplace

1. Go to https://plugins.jetbrains.com/author/dashboard
2. Click "Add New Plugin"
3. Upload the plugin ZIP file
4. Fill in metadata:
   - Plugin name
   - Description
   - Change notes
   - Screenshots
5. Set visibility:
   - Public (for release)
   - Beta (for testing)
   - Restricted (for private plugins)
6. Submit for review

### 4. Wait for Review
- **Typical review time:** 1-3 business days
- **For paid plugins:** Up to 5 business days
- You may receive feedback/request changes

---

## Post-Submission

### 1. Monitor Feedback
- Check plugin page for user reviews
- Monitor GitHub issues
- Set up support email

### 2. Handle Reviews
- Respond to user questions professionally
- Fix reported bugs promptly
- Consider feature requests

### 3. Plan Updates
- Regular maintenance releases
- Monitor IntelliJ SDK updates
- Test against new IDE versions

---

## Optional: Paid Plugin Considerations

### 1. Licensing Integration
- [ ] Implement license validation
- [ ] Add JetBrains Marketplace API integration
- [ ] Support subscription model

### 2. Premium Features
- [ ] Clearly distinguish free vs premium
- [ ] Implement feature gating
- [ ] Add upgrade prompts

### 3. Pricing
- Typical pricing for migration tools:
  - Personal license: $49-$99/year
  - Team license: $199-$499/year
  - Enterprise: Contact sales

---

## Useful Links

| Resource | URL |
|----------|-----|
| Marketplace Dashboard | https://plugins.jetbrains.com/author/dashboard |
| Plugin SDK Docs | https://plugins.jetbrains.com/docs/intellij |
| Gradle Plugin | https://github.com/JetBrains/gradle-intellij-plugin |
| Marketplace Agreement | https://jetbrains.com/legal/marketplace-agreement |
| Icon Guidelines | https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html |

---

## Quick Commands

```bash
# Build plugin
./gradlew buildPlugin

# Run IDE with plugin
./gradlew runIde

# Run tests
./gradlew test

# Check dependencies
./gradlew dependencies

# Create release ZIP
./gradlew jar
```

---

## Files to Update Before Publishing

| File | What to Update |
|------|----------------|
| `plugin.xml` | Description, change notes, vendor email |
| `gradle.properties` | version=x.x.x |
| `README.md` | Screenshots, features, install guide |
| `CHANGELOG.md` | Version 1.0.0 release notes |

---

## Current Project Status

| Requirement | Status | Notes |
|------------|--------|-------|
| Plugin ID | ✅ | `com.adrianmikula.jakarta-migration` |
| plugin.xml | ⚠️ | Needs description & change notes |
| Icons | ❌ | Need to create SVG icons |
| Build config | ✅ | Gradle plugin configured |
| README | ⚠️ | Needs IntelliJ-specific docs |
| Tests | ✅ | Test coverage exists |
| License | ✅ | Apache 2.0 |
