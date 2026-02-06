# Code Quality Tools Integration Guide

## Overview

This project now includes comprehensive code quality checks integrated into the build pipeline. The following tools are configured:

1. **SpotBugs** - Static analysis for bug detection
2. **PMD** - Code quality and best practices
3. **Checkstyle** - Coding standards enforcement
4. **OWASP Dependency Check** - Security vulnerability scanning

## Running Code Quality Checks

### Run All Checks (Analysis Only)
```bash
./gradlew codeQualityCheck  # Runs analysis, generates reports
```

### Verify Code Quality (Fails on High-Priority Issues)
```bash
./gradlew codeQualityVerify  # Verifies results, fails on high-priority bugs
```

### Run Individual Checks
```bash
# SpotBugs
./gradlew spotbugsMain

# PMD
./gradlew pmdMain

# Checkstyle
./gradlew checkstyleMain

# OWASP Dependency Check
./gradlew dependencyCheckAnalyze
```

### Run as Part of Build
```bash
./gradlew build  # Includes code quality checks
./gradlew check  # Runs verification tasks including code quality
```

## Viewing Reports

Reports are generated in the `build/reports/` directory:

- **SpotBugs**: `build/reports/spotbugs/main.html`
- **PMD**: `build/reports/pmd/main.html`
- **Checkstyle**: `build/reports/checkstyle/main.html`
- **OWASP**: `build/reports/dependency-check-report.html`

## Configuration Files

All configuration files are in the `config/` directory:

- `config/spotbugs/exclude.xml` - SpotBugs exclusions
- `config/pmd/ruleset.xml` - PMD rules
- `config/checkstyle/checkstyle.xml` - Checkstyle rules
- `config/owasp/suppressions.xml` - OWASP suppressions

## CI Integration

The GitHub Actions workflow automatically:
1. Runs all code quality checks after tests
2. Verifies results and fails on high-priority bugs only
3. Uploads reports as artifacts
4. **Fails build on:**
   - SpotBugs: High-priority bugs (rank 1-9)
   - PMD: High-priority issues (priority 1)
   - OWASP: Critical vulnerabilities (CVSS >= 7.0)
5. **Warns but allows build to pass on:**
   - SpotBugs: Medium/low-priority bugs (rank 10-20)
   - PMD: Medium/low-priority issues (priority 2-5)
   - Checkstyle: All style issues (warnings only)

## Customizing Rules

### SpotBugs
Edit `config/spotbugs/exclude.xml` to exclude specific classes or patterns.

### PMD
Edit `config/pmd/ruleset.xml` to enable/disable specific rules or add custom rules.

### Checkstyle
Edit `config/checkstyle/checkstyle.xml` to adjust style rules.

### OWASP
Edit `config/owasp/suppressions.xml` to suppress false positives.

## Best Practices

1. **Fix Issues Early**: Address code quality issues as they're found
2. **Review Reports**: Regularly review reports to identify patterns
3. **Update Rules**: Adjust rules based on project needs
4. **Suppress Carefully**: Only suppress false positives, not real issues
5. **Team Alignment**: Ensure team understands and follows quality standards

## Troubleshooting

### Build Fails on Code Quality Checks

1. Check the HTML reports to see specific issues
2. Fix the issues or adjust rules if they're false positives
3. For OWASP, add suppressions for accepted risks

### Reports Not Generated

1. Ensure the task ran successfully
2. Check that the build completed (not failed early)
3. Verify output directory permissions

### False Positives

1. Document why it's a false positive
2. Add appropriate exclusions/suppressions
3. Consider adjusting rules if pattern is common

## Next Steps

1. Run initial baseline: `./gradlew codeQualityCheck`
2. Review reports and fix critical issues
3. Adjust rules based on project needs
4. Integrate into development workflow
5. Monitor trends over time

