# Build premium module JAR (:jakarta-migration-mcp-premium:bootJar)
# Prefers mise-en-place when available, then gradlew, then direct gradle

$miseAvailable = $null -ne (Get-Command mise -ErrorAction SilentlyContinue)

if ($miseAvailable) {
    mise exec -- gradle :jakarta-migration-mcp-premium:bootJar
} elseif (Test-Path gradlew.bat) {
    .\gradlew.bat :jakarta-migration-mcp-premium:bootJar
} else {
    Write-Warning "mise not found and gradlew not available. Using system gradle (version may be incorrect)."
    gradle :jakarta-migration-mcp-premium:bootJar
}
