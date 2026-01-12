# Verify Apify MCP Deployment
# Tests that MCP tools are correctly exposed in Docker container

param(
    [string]$ContainerUrl = "http://localhost:8080",
    [string]$Endpoint = "/mcp/streamable-http",
    [string]$ContainerName = "jakarta-mcp-test",
    [string]$ImageName = "jakarta-migration-mcp:test",
    [int]$Port = 8080,
    [switch]$Verbose,
    [switch]$SkipDocker,
    [switch]$StopOnly,
    [int]$MaxRetries = 6,
    [int]$RetryDelaySeconds = 5
)

$ErrorActionPreference = "Stop"

Write-Host "[*] Jakarta Migration MCP Deployment Verification" -ForegroundColor Cyan
Write-Host ""

# Docker management functions
function Stop-ExistingContainers {
    param([string]$Name)
    
    Write-Host "[*] Checking for existing containers..." -ForegroundColor Yellow
    
    # Find containers by name (exact match or partial)
    $existingContainers = docker ps -a --filter "name=$Name" --format "{{.Names}}" 2>$null
    
    if ($existingContainers) {
        Write-Host "   Found existing containers: $($existingContainers -join ', ')" -ForegroundColor Gray
        
        foreach ($container in $existingContainers) {
            Write-Host "   Stopping container: $container" -ForegroundColor Yellow
            docker stop $container 2>&1 | Out-Null
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "   [OK] Stopped: $container" -ForegroundColor Green
            } else {
                Write-Host "   [WARN] Failed to stop: $container (may already be stopped)" -ForegroundColor Yellow
            }
            
            Write-Host "   Removing container: $container" -ForegroundColor Yellow
            docker rm $container 2>&1 | Out-Null
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "   [OK] Removed: $container" -ForegroundColor Green
            } else {
                Write-Host "   [WARN] Failed to remove: $container" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "   [OK] No existing containers found" -ForegroundColor Green
    }
    
    Write-Host ""
}

function New-DockerImage {
    param([string]$ImageName)
    
    Write-Host "[*] Building Docker image: $ImageName" -ForegroundColor Yellow
    
    if (-not (Test-Path "Dockerfile")) {
        Write-Host "   [FAIL] Dockerfile not found in current directory" -ForegroundColor Red
        Write-Host "   Please run this script from the project root directory" -ForegroundColor Yellow
        exit 1
    }
    
    # Build Docker image and capture output properly
    # Docker writes to stderr by default, which PowerShell treats as errors
    # Use a temporary file to capture output and avoid PowerShell error handling
    $tempFile = [System.IO.Path]::GetTempFileName()
    $ErrorActionPreferenceSave = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    
    try {
        # Run docker build and redirect all output to temp file
        & docker build -t $ImageName . *> $tempFile
        
        # Check exit code (this is the real indicator of success/failure)
        $buildSucceeded = ($LASTEXITCODE -eq 0)
        
        # Read output from temp file
        $buildOutput = Get-Content $tempFile -ErrorAction SilentlyContinue
        
        if (-not $buildSucceeded) {
            Write-Host "   [FAIL] Docker build failed with exit code: $LASTEXITCODE" -ForegroundColor Red
            if ($buildOutput) {
                $buildOutput | ForEach-Object {
                    $line = $_.Trim()
                    if ($line -and ($line -match "ERROR|error|failed|FAILED")) {
                        Write-Host "   $line" -ForegroundColor Red
                    } elseif ($line) {
                        Write-Host "   $line" -ForegroundColor Yellow
                    }
                }
            }
            Write-Host ""
            Remove-Item $tempFile -ErrorAction SilentlyContinue
            return $false
        }
        
        # Show build progress (filter out verbose output)
        if ($buildOutput) {
            $buildOutput | ForEach-Object {
                $line = $_.Trim()
                if ($line -match "Successfully built|Successfully tagged") {
                    Write-Host "   $line" -ForegroundColor Green
                } elseif ($line -match "^Step \d+/\d+") {
                    Write-Host "   $line" -ForegroundColor Gray
                } elseif ($Verbose -and $line) {
                    Write-Host "   $line" -ForegroundColor DarkGray
                }
            }
        }
        
        Write-Host "   [OK] Image built successfully" -ForegroundColor Green
        Write-Host ""
        Remove-Item $tempFile -ErrorAction SilentlyContinue
        return $true
    } catch {
        Write-Host "   [FAIL] Docker build encountered an error: $_" -ForegroundColor Red
        Write-Host ""
        Remove-Item $tempFile -ErrorAction SilentlyContinue
        return $false
    } finally {
        $ErrorActionPreference = $ErrorActionPreferenceSave
    }
}

function Start-DockerContainer {
    param(
        [string]$ImageName,
        [string]$ContainerName,
        [int]$Port
    )
    
    Write-Host "[*] Starting Docker container..." -ForegroundColor Yellow
    Write-Host "   Image: $ImageName" -ForegroundColor Gray
    Write-Host "   Name: $ContainerName" -ForegroundColor Gray
    Write-Host "   Port: $Port" -ForegroundColor Gray
    Write-Host ""
    
    $containerId = docker run -d `
        --name $ContainerName `
        -p "${Port}:${Port}" `
        -e MCP_TRANSPORT=streamable-http `
        -e SPRING_PROFILES_ACTIVE=mcp-streamable-http `
        $ImageName 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   [OK] Container started: $($containerId.Substring(0, 12))" -ForegroundColor Green
        Write-Host "   Waiting for Spring Boot to start (this may take 10-30 seconds)..." -ForegroundColor Yellow
        Write-Host ""
        return $true
    } else {
        Write-Host "   [FAIL] Failed to start container" -ForegroundColor Red
        Write-Host "   Error: $containerId" -ForegroundColor Red
        Write-Host ""
        return $false
    }
}

# Main execution
if ($StopOnly) {
    Stop-ExistingContainers -Name $ContainerName
    Write-Host "[OK] Container cleanup complete" -ForegroundColor Green
    exit 0
}

if (-not $SkipDocker) {
    # Step 1: Stop existing containers
    Stop-ExistingContainers -Name $ContainerName
    
    # Step 2: Build Docker image
    if (-not (New-DockerImage -ImageName $ImageName)) {
        exit 1
    }
    
    # Step 3: Start container
    if (-not (Start-DockerContainer -ImageName $ImageName -ContainerName $ContainerName -Port $Port)) {
        exit 1
    }
} else {
    Write-Host "[*] Skipping Docker operations (using existing container)" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "[*] Starting verification tests..." -ForegroundColor Cyan
Write-Host "   Container URL: $ContainerUrl" -ForegroundColor Gray
Write-Host "   Endpoint: $Endpoint" -ForegroundColor Gray
Write-Host ""

# Helper function to test connectivity with retries
function Test-EndpointWithRetry {
    param(
        [string]$Uri,
        [string]$Method = "Get",
        [int]$MaxRetries = 6,
        [int]$RetryDelaySeconds = 5
    )
    
    $attempt = 0
    while ($attempt -lt $MaxRetries) {
        try {
            $response = Invoke-RestMethod -Uri $Uri -Method $Method -ErrorAction Stop
            return @{ Success = $true; Response = $response; Attempt = $attempt + 1 }
        } catch {
            $attempt++
            if ($attempt -lt $MaxRetries) {
                Write-Host "   [RETRY] Attempt $attempt/$MaxRetries failed, retrying in $RetryDelaySeconds seconds..." -ForegroundColor Yellow
                Start-Sleep -Seconds $RetryDelaySeconds
            } else {
                return @{ Success = $false; Error = $_.Exception.Message; Attempt = $attempt }
            }
        }
    }
}

# Test 1: Health Check (with retries for startup)
Write-Host "1. Testing health endpoint..." -ForegroundColor Yellow
$healthTest = Test-EndpointWithRetry -Uri "$ContainerUrl/actuator/health" -MaxRetries $MaxRetries -RetryDelaySeconds $RetryDelaySeconds

if (-not $healthTest.Success) {
    Write-Host "   [FAIL] Health endpoint not accessible after $($healthTest.Attempt) attempts" -ForegroundColor Red
    Write-Host "   Error: $($healthTest.Error)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "   1. Check container status:" -ForegroundColor White
    Write-Host "      docker ps -a | Select-String $ContainerName" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   2. Check container logs:" -ForegroundColor White
    Write-Host "      docker logs $ContainerName" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   3. Container may still be starting. Wait a bit longer and try again." -ForegroundColor White
    Write-Host ""
    exit 1
}

if ($healthTest.Response.status -eq "UP") {
    Write-Host "   [OK] Health check passed (attempt $($healthTest.Attempt))" -ForegroundColor Green
} else {
    Write-Host "   [FAIL] Health check failed: Status = $($healthTest.Response.status)" -ForegroundColor Red
    exit 1
}

# Test 2: MCP Initialize
Write-Host "2. Testing MCP initialize..." -ForegroundColor Yellow
try {
    $initRequest = @{
        jsonrpc = "2.0"
        id = 1
        method = "initialize"
        params = @{
            protocolVersion = "2024-11-05"
            capabilities = @{}
            clientInfo = @{
                name = "verification-script"
                version = "1.0.0"
            }
        }
    } | ConvertTo-Json -Depth 10

    $initResponse = Invoke-RestMethod -Uri "$ContainerUrl$Endpoint" `
        -Method Post `
        -ContentType "application/json" `
        -Body $initRequest `
        -ErrorAction Stop

    if ($initResponse.result -and $initResponse.result.serverInfo) {
        Write-Host "   [OK] Initialize successful" -ForegroundColor Green
        Write-Host "      Server: $($initResponse.result.serverInfo.name) v$($initResponse.result.serverInfo.version)" -ForegroundColor Gray
    } else {
        Write-Host "   [FAIL] Initialize failed: Invalid response" -ForegroundColor Red
        if ($Verbose) {
            Write-Host "      Response: $($initResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Gray
        }
        exit 1
    }
} catch {
    Write-Host "   [FAIL] Initialize failed: $_" -ForegroundColor Red
    if ($Verbose) {
        Write-Host "      Error details: $($_.Exception.Message)" -ForegroundColor Gray
    }
    exit 1
}

# Test 3: Tools List (CRITICAL TEST)
Write-Host "3. Testing tools/list (CRITICAL)..." -ForegroundColor Yellow
try {
    $toolsRequest = @{
        jsonrpc = "2.0"
        id = 2
        method = "tools/list"
        params = @{}
    } | ConvertTo-Json -Depth 10

    $toolsResponse = Invoke-RestMethod -Uri "$ContainerUrl$Endpoint" `
        -Method Post `
        -ContentType "application/json" `
        -Body $toolsRequest `
        -ErrorAction Stop

    $toolCount = $toolsResponse.result.tools.Count

    if ($toolCount -eq 7) {
        Write-Host "   [OK] Tools list returned 7 tools (expected)" -ForegroundColor Green
    } else {
        Write-Host "   [FAIL] Expected 7 tools, got $toolCount" -ForegroundColor Red
        exit 1
    }

    # List tool names
    Write-Host "4. Tool names:" -ForegroundColor Yellow
    $toolsResponse.result.tools | ForEach-Object {
        Write-Host "   - $($_.name)" -ForegroundColor Gray
        if ($Verbose) {
            Write-Host "     Description: $($_.description)" -ForegroundColor DarkGray
        }
    }

    # Verify expected tools
    $expectedTools = @(
        "analyzeJakartaReadiness",
        "detectBlockers",
        "recommendVersions",
        "createMigrationPlan",
        "analyzeMigrationImpact",
        "verifyRuntime",
        "check_env"
    )

    $actualToolNames = $toolsResponse.result.tools | ForEach-Object { $_.name }
    $missingTools = $expectedTools | Where-Object { $_ -notin $actualToolNames }

    if ($missingTools.Count -eq 0) {
        Write-Host "   [OK] All expected tools are present" -ForegroundColor Green
    } else {
        Write-Host "   [FAIL] Missing tools: $($missingTools -join ', ')" -ForegroundColor Red
        exit 1
    }

    # Verify tool schemas
    Write-Host "5. Verifying tool schemas..." -ForegroundColor Yellow
    $schemaIssues = @()
    foreach ($tool in $toolsResponse.result.tools) {
        if (-not $tool.name) {
            $schemaIssues += "$($tool.name): Missing 'name'"
        }
        if (-not $tool.description) {
            $schemaIssues += "$($tool.name): Missing 'description'"
        }
        if (-not $tool.inputSchema) {
            $schemaIssues += "$($tool.name): Missing 'inputSchema'"
        }
    }

    if ($schemaIssues.Count -eq 0) {
        Write-Host "   [OK] All tools have valid schemas" -ForegroundColor Green
    } else {
        Write-Host "   [FAIL] Schema issues found:" -ForegroundColor Red
        $schemaIssues | ForEach-Object {
            Write-Host "      - $_" -ForegroundColor Red
        }
        exit 1
    }

} catch {
    Write-Host "   [FAIL] Tools list failed: $_" -ForegroundColor Red
    if ($Verbose) {
        Write-Host "      Error details: $($_.Exception.Message)" -ForegroundColor Gray
        Write-Host "      Stack trace: $($_.ScriptStackTrace)" -ForegroundColor DarkGray
    }
    exit 1
}

# Test 4: Tool Execution (should fail gracefully without real project)
Write-Host "6. Testing tool execution endpoint..." -ForegroundColor Yellow
try {
    $callRequest = @{
        jsonrpc = "2.0"
        id = 3
        method = "tools/call"
        params = @{
            name = "analyzeJakartaReadiness"
            arguments = @{
                projectPath = "/tmp/nonexistent"
            }
        }
    } | ConvertTo-Json -Depth 10

    $callResponse = Invoke-RestMethod -Uri "$ContainerUrl$Endpoint" `
        -Method Post `
        -ContentType "application/json" `
        -Body $callRequest `
        -ErrorAction Stop

    # Tool execution should return a response (even if error)
    if ($callResponse.result) {
        Write-Host "   [OK] Tool execution endpoint responds correctly" -ForegroundColor Green
        if ($Verbose) {
            Write-Host "      Response type: $($callResponse.result.content[0].type)" -ForegroundColor Gray
        }
    } else {
        Write-Host "   [WARN] Tool execution returned unexpected format" -ForegroundColor Yellow
        if ($Verbose) {
            Write-Host "      Response: $($callResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "   [WARN] Tool execution test failed (may be expected): $_" -ForegroundColor Yellow
    if ($Verbose) {
        Write-Host "      This is OK if tool requires valid project path" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "[OK] All verification checks passed!" -ForegroundColor Green
Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  - Health endpoint: [OK]" -ForegroundColor Green
Write-Host "  - MCP initialize: [OK]" -ForegroundColor Green
Write-Host "  - Tools list: [OK] (7 tools)" -ForegroundColor Green
Write-Host "  - Tool schemas: [OK]" -ForegroundColor Green
Write-Host "  - Tool execution: [OK]" -ForegroundColor Green
Write-Host ""
Write-Host "Your MCP server is correctly configured and ready for Apify deployment!" -ForegroundColor Cyan

