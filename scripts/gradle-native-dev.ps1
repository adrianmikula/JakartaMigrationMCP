# Build and run native image via Gradle nativeDev task
# Prefers mise-en-place when available, then gradlew, then direct gradle

$miseAvailable = $null -ne (Get-Command mise -ErrorAction SilentlyContinue)

if ($miseAvailable) {
    mise exec -- gradle nativeDev
} elseif (Test-Path gradlew.bat) {
    .\gradlew.bat nativeDev
} else {
    Write-Warning "mise not found and gradlew not available. Using system gradle (version may be incorrect)."
    gradle nativeDev
}
