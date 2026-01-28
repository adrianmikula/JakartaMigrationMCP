# Helper script for Gradle build
# Prefers mise-en-place when available, then gradlew, then direct gradle
param([switch]$WithTests)

# Check if mise is available and in PATH
$miseAvailable = $null -ne (Get-Command mise -ErrorAction SilentlyContinue)

if ($miseAvailable) {
    # Use mise exec to run gradle (mise exec sets up the environment and tool versions)
    if ($WithTests) {
        mise exec -- gradle build
    } else {
        mise exec -- gradle build -x test
    }
} elseif (Test-Path gradlew.bat) {
    # Fall back to gradlew if mise is not available
    if ($WithTests) {
        .\gradlew.bat build
    } else {
        .\gradlew.bat build -x test
    }
} else {
    # Last resort: direct gradle (may not have correct version)
    Write-Warning "mise not found and gradlew not available. Using system gradle (version may be incorrect)."
    if ($WithTests) {
        gradle build
    } else {
        gradle build -x test
    }
}

