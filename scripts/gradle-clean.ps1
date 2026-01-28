# Helper script for Gradle clean
# Prefers mise-en-place when available, then gradlew, then direct gradle

# Check if mise is available and in PATH
$miseAvailable = $null -ne (Get-Command mise -ErrorAction SilentlyContinue)

if ($miseAvailable) {
    # Use mise exec to run gradle (mise exec sets up the environment and tool versions)
    mise exec -- gradle clean
} elseif (Test-Path gradlew.bat) {
    # Fall back to gradlew if mise is not available
    .\gradlew.bat clean
} else {
    # Last resort: direct gradle (may not have correct version)
    Write-Warning "mise not found and gradlew not available. Using system gradle (version may be incorrect)."
    gradle clean
}

