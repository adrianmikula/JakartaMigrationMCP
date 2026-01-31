# Antigravity Rules for JakartaMigrationMCP

These rules are persistent for the Antigravity agent in this repository.

1. **Build Tool Selection**:
   - Windows: Use Gradle (`.\gradlew.bat`).
   - Linux/macOS: Use Mill (`./mill`).
   - Prefer `mise run <task>` (e.g., `mise run build`, `mise run test`).

2. **Windows File Locks**:
   - Before any build or test run on Windows, or if "file is in use" errors occur, run:
     `.\scripts\kill-gradle-java.ps1 -Force`

3. **Java Version**:
   - Target Java 21. Use absolute path to Java if necessary, especially in integration tests starting sub-processes.

4. **MCP Communication**:
   - Stdio transport must have a "clean" stdout (JSON-RPC only).
   - All logging and banners must be redirected to stderr.

5. **Test Stability**:
   - Integration tests starting the server should wait for the "Enable completions capabilities" log message before sending requests to avoid race conditions.
