$javaPath = "C:\Program Files\Java\jdk-21.0.10\bin\java.exe"
$jarPath = "build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar"
$json = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}'

Write-Host "Starting JAR: $jarPath"
Write-Host "Using Java: $javaPath"

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $javaPath
$psi.Arguments = "-jar $jarPath --spring.profiles.active=mcp-stdio --spring.ai.mcp.server.transport=stdio --spring.main.web-application-type=none --spring.main.banner-mode=off"
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.UseShellExecute = $false
$psi.EnvironmentVariables["MCP_TRANSPORT"] = "stdio"

$p = [System.Diagnostics.Process]::Start($psi)
if ($null -eq $p) {
    Write-Host "Failed to start process" -ForegroundColor Red
    exit 1
}

$stdin = $p.StandardInput
$stdout = $p.StandardOutput
$stderr = $p.StandardError

# Read stderr and stdout in a loop
Write-Host "Monitoring output..."
$timeout = [DateTime]::Now.AddSeconds(30)
$sent = $false
while ([DateTime]::Now -lt $timeout) {
    if ($p.HasExited) {
        Write-Host "Process exited!" -ForegroundColor Red
        break
    }

    $hasOutput = $false
    
    if ($stderr.Peek() -ge 0) {
        $line = $stderr.ReadLine()
        Write-Host "STDERR: $line" -ForegroundColor Gray
        if ($line -match "Enable completions capabilities") {
            if (!$sent) {
                Write-Host "Server Ready. Sending Initialize JSON..." -ForegroundColor Cyan
                $stdin.WriteLine($json)
                $stdin.Flush()
                $sent = $true
            }
        }
        $hasOutput = $true
    }

    if ($stdout.Peek() -ge 0) {
        $line = $stdout.ReadLine()
        Write-Host "STDOUT: $line" -ForegroundColor Green
        if ($line -match "result") {
            Write-Host "Initialize SUCCESS!" -ForegroundColor Green
            break
        }
        $hasOutput = $true
    }

    if (!$hasOutput) {
        Start-Sleep -Milliseconds 100
    }
}
Write-Host "Debug finished."
if (!$p.HasExited) {
    $p.Kill()
}
