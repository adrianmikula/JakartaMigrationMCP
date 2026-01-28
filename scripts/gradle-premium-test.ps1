# Run tests for premium module only (:jakarta-migration-mcp-premium:test)
# Prefers mise-en-place when available, then gradlew, then direct gradle

$miseAvailable = $null -ne (Get-Command mise -ErrorAction SilentlyContinue)

if ($miseAvailable) {
    mise exec -- gradle :jakarta-migration-mcp-premium:test
} elseif (Test-Path gradlew.bat) {
    .\gradlew.bat :jakarta-migration-mcp-premium:test
} else {
    Write-Warning "mise not found and gradlew not available. Using system gradle (version may be incorrect)."
    gradle :jakarta-migration-mcp-premium:test
}
