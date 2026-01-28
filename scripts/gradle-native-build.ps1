# Build GraalVM native image (nativeCompile -x test)
# Prefers mise-en-place when available, then gradlew, then direct gradle

$miseAvailable = $null -ne (Get-Command mise -ErrorAction SilentlyContinue)

if ($miseAvailable) {
    mise exec -- gradle nativeCompile -x test
} elseif (Test-Path gradlew.bat) {
    .\gradlew.bat nativeCompile -x test
} else {
    Write-Warning "mise not found and gradlew not available. Using system gradle (version may be incorrect)."
    gradle nativeCompile -x test
}
