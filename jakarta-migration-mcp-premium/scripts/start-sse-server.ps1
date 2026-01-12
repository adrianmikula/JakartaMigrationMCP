# Start Jakarta Migration MCP Server with SSE Transport
# This script starts the server in the background and shows the URL

Write-Host "Starting Jakarta Migration MCP Server with SSE transport..." -ForegroundColor Cyan

$jarPath = "build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: JAR file not found at $jarPath" -ForegroundColor Red
    Write-Host "Please build the JAR first: .\gradlew.bat bootJar" -ForegroundColor Yellow
    exit 1
}

Write-Host "Starting server on http://localhost:8080/mcp/sse" -ForegroundColor Green
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""

# Start the server
java -jar $jarPath --spring.profiles.active=mcp-sse

