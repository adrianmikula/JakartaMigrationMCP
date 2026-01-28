# Build both free and premium JARs (bootJar + :jakarta-migration-mcp-premium:bootJar)
# Prefers mise-en-place when available, then gradlew, then direct gradle

$miseAvailable = $null -ne (Get-Command mise -ErrorAction SilentlyContinue)

if ($miseAvailable) {
    mise exec -- gradle bootJar :jakarta-migration-mcp-premium:bootJar --no-daemon
} elseif (Test-Path gradlew.bat) {
    .\gradlew.bat bootJar :jakarta-migration-mcp-premium:bootJar --no-daemon
} else {
    Write-Warning "mise not found and gradlew not available. Using system gradle (version may be incorrect)."
    gradle bootJar :jakarta-migration-mcp-premium:bootJar --no-daemon
}
