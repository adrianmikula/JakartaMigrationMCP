# OWASP Dependency Check Setup

## Overview

OWASP Dependency Check scans project dependencies for known security vulnerabilities. It uses the National Vulnerability Database (NVD) to identify CVEs in your dependencies.

## NVD API Key (Recommended)

As of 2024, the NVD requires an API key for reliable access. Without an API key, you may encounter 403/404 errors when updating vulnerability data.

### Getting an NVD API Key

1. Visit: https://nvd.nist.gov/developers/request-an-api-key
2. Fill out the form (free for public use)
3. You'll receive an API key via email

### Setting the API Key

#### Local Development

**Option 1: Environment Variable**
```bash
export DC_NVD_API_KEY=your-api-key-here
./gradlew dependencyCheckAnalyze
```

**Option 2: System Property**
```bash
./gradlew dependencyCheckAnalyze -DdependencyCheck.nvd.apiKey=your-api-key-here
```

**Option 3: Gradle Properties**
Add to `~/.gradle/gradle.properties`:
```properties
dependencyCheck.nvd.apiKey=your-api-key-here
```

#### CI/CD (GitHub Actions)

1. Add the API key as a GitHub Secret:
   - Go to Repository Settings → Secrets and variables → Actions
   - Add new secret: `DC_NVD_API_KEY` with your API key value

2. The CI workflow will automatically use it (uncomment the env line in `.github/workflows/ci.yml`)

## Configuration

The OWASP Dependency Check is configured in `build.gradle.kts`:

```kotlin
dependencyCheck {
    formats = listOf("HTML", "JSON", "XML")
    failBuildOnCVSS = 7.0f // Fail on high/critical vulnerabilities
    suppressionFile = "config/owasp/suppressions.xml"
    skipProjects = listOf(":test")
}
```

### Key Settings

- **failBuildOnCVSS**: Build fails if vulnerabilities with CVSS >= 7.0 are found
- **suppressionFile**: File for suppressing false positives
- **formats**: Report formats to generate

## Running the Check

```bash
# Run dependency check
./gradlew dependencyCheckAnalyze

# View HTML report
open build/reports/dependency-check-report.html
```

## Handling Failures

### NVD Update Failures

If you see errors like:
```
Error updating the NVD Data; the NVD returned a 403 or 404 error
```

**Solutions:**
1. **Get an NVD API key** (recommended) - see above
2. **Use local data** - The check will use cached local data if available
3. **Make check optional** - Already configured to not fail build on NVD errors

### False Positives

If a vulnerability is a false positive or doesn't affect your use case:

1. Add to `config/owasp/suppressions.xml`:
```xml
<suppress>
    <notes><![CDATA[
    False positive - vulnerability doesn't affect our use case
    ]]></notes>
    <packageUrl regex="true">^pkg:maven/.*/.*@.*$</packageUrl>
    <cve>CVE-2023-XXXXX</cve>
</suppress>
```

2. Or suppress by package:
```xml
<suppress>
    <notes>Not used in production</notes>
    <packageUrl regex="true">^pkg:maven/com\.example/.*@.*$</packageUrl>
</suppress>
```

## CI Integration

The OWASP check runs as part of `codeQualityCheck` but:
- **Won't fail the build** if NVD update fails (uses local data)
- **Will fail the build** if critical vulnerabilities (CVSS >= 7.0) are found
- Reports are uploaded as artifacts for review

## Best Practices

1. **Get an API key** - Essential for reliable CI/CD
2. **Review reports regularly** - Check for new vulnerabilities
3. **Update dependencies** - Fix vulnerabilities by updating to patched versions
4. **Document suppressions** - Always document why you're suppressing a vulnerability
5. **Monitor trends** - Track vulnerability counts over time

## Troubleshooting

### Slow Updates

- Use an NVD API key to speed up updates
- The first run downloads the full database (can take 10+ minutes)
- Subsequent runs are faster (incremental updates)

### Out of Memory

If you get OOM errors:
```kotlin
dependencyCheck {
    // Increase JVM memory if needed
    // Run with: ./gradlew dependencyCheckAnalyze -Xmx2g
}
```

### Network Issues

If you're behind a proxy:
```kotlin
dependencyCheck {
    proxyServer = "proxy.example.com"
    proxyPort = 8080
}
```

## Resources

- [OWASP Dependency Check Documentation](https://jeremylong.github.io/DependencyCheck/)
- [NVD API Key Request](https://nvd.nist.gov/developers/request-an-api-key)
- [CVE Database](https://cve.mitre.org/)

