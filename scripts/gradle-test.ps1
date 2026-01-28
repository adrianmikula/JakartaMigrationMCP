# Helper script for Gradle test
# Prefers mise-en-place when available, then gradlew, then direct gradle
param([switch]$UnitOnly, [switch]$ComponentOnly, [switch]$E2EOnly)

# Check if mise is available and in PATH
$miseAvailable = $null -ne (Get-Command mise -ErrorAction SilentlyContinue)

if ($miseAvailable) {
    # Use mise exec to run gradle (mise exec sets up the environment and tool versions)
    if ($UnitOnly) {
        mise exec -- gradle test --tests "*Test" --exclude-tests "*ComponentTest" --exclude-tests "*E2ETest" --exclude-tests "*e2e.*"
    } elseif ($ComponentOnly) {
        mise exec -- gradle test --tests "adrianmikula.jakartamigration.component.*"
    } elseif ($E2EOnly) {
        mise exec -- gradle test --tests "adrianmikula.jakartamigration.e2e.*"
    } else {
        mise exec -- gradle test
    }
} elseif (Test-Path gradlew.bat) {
    # Fall back to gradlew if mise is not available
    if ($UnitOnly) {
        .\gradlew.bat test --tests "*Test" --exclude-tests "*ComponentTest" --exclude-tests "*E2ETest" --exclude-tests "*e2e.*"
    } elseif ($ComponentOnly) {
        .\gradlew.bat test --tests "adrianmikula.jakartamigration.component.*"
    } elseif ($E2EOnly) {
        .\gradlew.bat test --tests "adrianmikula.jakartamigration.e2e.*"
    } else {
        .\gradlew.bat test
    }
} else {
    # Last resort: direct gradle (may not have correct version)
    Write-Warning "mise not found and gradlew not available. Using system gradle (version may be incorrect)."
    if ($UnitOnly) {
        gradle test --tests "*Test" --exclude-tests "*ComponentTest" --exclude-tests "*E2ETest" --exclude-tests "*e2e.*"
    } elseif ($ComponentOnly) {
        gradle test --tests "adrianmikula.jakartamigration.component.*"
    } elseif ($E2EOnly) {
        gradle test --tests "adrianmikula.jakartamigration.e2e.*"
    } else {
        gradle test
    }
}

