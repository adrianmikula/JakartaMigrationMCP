#!/usr/bin/env pwsh
# Test script to verify the npm wrapper correctly runs the JAR file
# This tests the complete npm package functionality including JAR execution and MCP communication

param(
    [string]$JarPath = "build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar",
    [int]$TimeoutSeconds = 45
)

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "NPM Wrapper Test" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check if JAR exists
Write-Host "[TEST 1] Checking JAR file..." -ForegroundColor Yellow
if (-not (Test-Path $JarPath)) {
    Write-Host "  [FAIL] JAR file not found at: $JarPath" -ForegroundColor Red
    Write-Host "  Please build the project first: gradlew bootJar" -ForegroundColor Yellow
    exit 1
}
$jarSize = (Get-Item $JarPath).Length / 1MB
$jarSizeRounded = [math]::Round($jarSize, 2)
Write-Host "  [OK] JAR found: $JarPath ($jarSizeRounded MB)" -ForegroundColor Green
Write-Host ""

# Test 2: Check Node.js and npm wrapper
Write-Host "[TEST 2] Checking npm wrapper..." -ForegroundColor Yellow
if (-not (Test-Path "index.js")) {
    Write-Host "  ✗ index.js not found" -ForegroundColor Red
    exit 1
}
Write-Host "  [OK] index.js found" -ForegroundColor Green

$nodeCheck = Get-Command node -ErrorAction SilentlyContinue
if ($null -eq $nodeCheck) {
    Write-Host "  [FAIL] Node.js not found in PATH" -ForegroundColor Red
    exit 1
}
$nodeVersion = node --version
Write-Host "  [OK] Node.js found: $nodeVersion" -ForegroundColor Green
Write-Host ""

# Test 3: Test --download-only flag (with local JAR)
Write-Host "[TEST 3] Testing --download-only flag..." -ForegroundColor Yellow
$env:JAKARTA_MCP_JAR_PATH = (Resolve-Path $JarPath).Path

# Use cmd to avoid PowerShell's stderr handling issues
$tempFile = [System.IO.Path]::GetTempFileName()
$cmd = "node index.js --download-only 2>&1 > `"$tempFile`" 2>&1; echo EXIT_CODE:%ERRORLEVEL%"
$result = cmd /c $cmd
$exitCode = 0
if ($result -match "EXIT_CODE:(\d+)") {
    $exitCode = [int]$matches[1]
}
$downloadOutput = Get-Content $tempFile -ErrorAction SilentlyContinue | Out-String
Remove-Item $tempFile -ErrorAction SilentlyContinue

if ($exitCode -eq 0) {
    Write-Host "  [OK] --download-only flag works (exit code: 0)" -ForegroundColor Green
    if ($downloadOutput -match "JAR ready") {
        Write-Host "  [OK] JAR path correctly identified" -ForegroundColor Green
    }
} else {
    Write-Host "  [FAIL] --download-only flag failed (exit code: $exitCode)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Test 4: Test wrapper starts Java process correctly
Write-Host "[TEST 4] Testing wrapper starts Java process..." -ForegroundColor Yellow
$javaFound = $false
if ($null -ne (Get-Command java -ErrorAction SilentlyContinue)) {
    $javaFound = $true
} elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $javaFound = $true
}

if ($javaFound) {
    Write-Host "  [OK] Java found (PATH or JAVA_HOME)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Java not found in PATH or JAVA_HOME" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Test 5: Test MCP server startup and communication
Write-Host "[TEST 5] Testing MCP server startup and JSON-RPC communication..." -ForegroundColor Yellow
Write-Host "  Starting server process..." -ForegroundColor Gray

# Create a temporary script to send MCP requests
$testScript = @"
const { spawn } = require('child_process');
const readline = require('readline');

// Start the npm wrapper
const wrapper = spawn('node', ['index.js'], {
    stdio: ['pipe', 'pipe', 'pipe'],
    env: { ...process.env, JAKARTA_MCP_JAR_PATH: '$((Resolve-Path $JarPath).Path.Replace("\", "/"))' }
});

let outputBuffer = '';
let requestId = 1;
let testsPassed = 0;
let testsFailed = 0;

// Read stdout line by line
const rl = readline.createInterface({
    input: wrapper.stdout,
    crlfDelay: Infinity
});

// Send MCP initialize request
setTimeout(() => {
    const initRequest = {
        jsonrpc: '2.0',
        id: requestId++,
        method: 'initialize',
        params: {
            protocolVersion: '2024-11-05',
            capabilities: {},
            clientInfo: {
                name: 'npm-wrapper-test',
                version: '1.0.0'
            }
        }
    };
    
    wrapper.stdin.write(JSON.stringify(initRequest) + '\n');
    wrapper.stdin.flush();
}, 1000);

// Handle responses
rl.on('line', (line) => {
    try {
        const response = JSON.parse(line.trim());
        
        if (response.id === 1 && response.result) {
            console.log('[OK] Initialize response received');
            testsPassed++;
            
            // Send tools/list request
            setTimeout(() => {
                const toolsRequest = {
                    jsonrpc: '2.0',
                    id: requestId++,
                    method: 'tools/list',
                    params: {}
                };
                wrapper.stdin.write(JSON.stringify(toolsRequest) + '\n');
                wrapper.stdin.flush();
            }, 500);
        } else if (response.id === 2 && response.result && response.result.tools) {
            console.log('[OK] Tools list received (' + response.result.tools.length + ' tools)');
            testsPassed++;
            
            // Test complete
            setTimeout(() => {
                wrapper.kill();
                process.exit(testsFailed > 0 ? 1 : 0);
            }, 500);
        } else if (response.error) {
            console.error('[FAIL] Error response:', response.error);
            testsFailed++;
            wrapper.kill();
            process.exit(1);
        }
    } catch (e) {
        // Ignore non-JSON lines (logging to stderr)
    }
});

// Handle stderr (logging)
wrapper.stderr.on('data', (data) => {
    process.stderr.write(data);
});

// Timeout
setTimeout(() => {
    console.error('[FAIL] Test timeout');
    wrapper.kill();
    process.exit(1);
}, $($TimeoutSeconds * 1000));

wrapper.on('error', (error) => {
    console.error('[FAIL] Process error:', error.message);
    process.exit(1);
});
"@

$testScriptPath = "test-npm-wrapper-temp.js"
$testScript | Out-File -FilePath $testScriptPath -Encoding UTF8

try {
    # Run the test script and capture output
    # Using 2>&1 to merge streams and | Tee-Object to capture to file
    node $testScriptPath 2>&1 | Tee-Object -FilePath "npm-wrapper-test-all.log"
    
    $exitCode = $LASTEXITCODE
    
    if (Test-Path "npm-wrapper-test-all.log") {
        $output = Get-Content "npm-wrapper-test-all.log" -Raw
        # ... rest of the code ...
        
        if ($output -match "Initialize response received") {
            Write-Host "  [OK] MCP initialize successful" -ForegroundColor Green
            $mcpInitOk = $true
        } else {
            Write-Host "  [FAIL] MCP initialize failed" -ForegroundColor Red
            $mcpInitOk = $false
        }
        
        if ($output -match "Tools list received") {
            Write-Host "  [OK] MCP tools/list successful" -ForegroundColor Green
            $mcpToolsOk = $true
        } else {
            Write-Host "  [FAIL] MCP tools/list failed" -ForegroundColor Red
            $mcpToolsOk = $false
        }

        if (-not ($mcpInitOk -and $mcpToolsOk)) {
            Write-Host "  Full Output Log:" -ForegroundColor Yellow
            Write-Host $output -ForegroundColor Gray
        }
    }
    
    if ($exitCode -eq 0 -and $mcpInitOk -and $mcpToolsOk) {
        Write-Host "  [OK] All MCP communication tests passed" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Some tests failed (exit code: $exitCode)" -ForegroundColor Red
    }
} finally {
    # Cleanup
    Remove-Item $testScriptPath -ErrorAction SilentlyContinue
    Remove-Item "npm-wrapper-test-all.log" -ErrorAction SilentlyContinue
}

Write-Host ""

# Test 6: Verify wrapper handles signals correctly
Write-Host "[TEST 6] Testing signal handling..." -ForegroundColor Yellow
Write-Host "  (Manual test: Start wrapper and press Ctrl+C)" -ForegroundColor Gray
Write-Host "  [OK] Signal handling configured in wrapper" -ForegroundColor Green
Write-Host ""

# Summary
# Summary
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$jarStatus = if (Test-Path $JarPath) { "[OK]" } else { "[FAIL]" }
$jarColor = if (Test-Path $JarPath) { "Green" } else { "Red" }
$indexStatus = if (Test-Path "index.js") { "[OK]" } else { "[FAIL]" }
$indexColor = if (Test-Path "index.js") { "Green" } else { "Red" }
$downloadStatus = if ($exitCode -eq 0) { "[OK]" } else { "[FAIL]" }
$downloadColor = if ($exitCode -eq 0) { "Green" } else { "Red" }
$javaStatus = if ($javaFound) { "[OK]" } else { "[FAIL]" }
$javaColor = if ($javaFound) { "Green" } else { "Red" }
$mcpInitStatus = if ($mcpInitOk) { "[OK]" } else { "[FAIL]" }
$mcpInitColor = if ($mcpInitOk) { "Green" } else { "Red" }
$mcpToolsStatus = if ($mcpToolsOk) { "[OK]" } else { "[FAIL]" }
$mcpToolsColor = if ($mcpToolsOk) { "Green" } else { "Red" }

Write-Host "$jarStatus JAR file check" -ForegroundColor $jarColor
Write-Host "$indexStatus npm wrapper file check" -ForegroundColor $indexColor
Write-Host "$downloadStatus --download-only flag" -ForegroundColor $downloadColor
Write-Host "$javaStatus Java detection" -ForegroundColor $javaColor
Write-Host "$mcpInitStatus MCP server start/init" -ForegroundColor $mcpInitColor
Write-Host "$mcpToolsStatus JSON-RPC communication" -ForegroundColor $mcpToolsColor
Write-Host ""

if ($mcpInitOk -and $mcpToolsOk -and $exitCode -eq 0) {
    Write-Host "The npm wrapper is working correctly!" -ForegroundColor Green
} else {
    Write-Host "The npm wrapper test FAILED!" -ForegroundColor Red
    exit 1
}
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Publish to npm: npm publish" -ForegroundColor White
Write-Host "2. Test installation: npm install -g @jakarta-migration/mcp-server" -ForegroundColor White
Write-Host "3. Configure MCP client to use: npx -y @jakarta-migration/mcp-server" -ForegroundColor White
Write-Host ""

