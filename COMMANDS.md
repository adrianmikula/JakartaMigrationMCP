# Commands

## Fast iteration (inner loop — run after every change)

| Category | Command | Expected time |
|----------|---------|---------------|
| Compile check | `./gradlew :community-core-engine:compileJava` | ~0.8s |
| Fast tests (single module) | `./gradlew :community-core-engine:fastTest` | ~0.76s |
| Fast tests (all modules) | `mise run fast-test` | ~2.0s (cold) / ~1.9s (warm) |
| Direct Gradle (all modules) | `./gradlew :community-core-engine:fastTest :premium-core-engine:fastTest :premium-experiment-engine:fastTest :community-mcp-server:fastTest :premium-mcp-server:fastTest --configure-on-demand` | ~2.0s (cold) / ~1.9s (warm) |

## Full validation (CI — run before commit/push)

| Category | Command | Expected time |
|----------|---------|---------------|
| Full test suite | `mise run test` or `./gradlew test` | ~2min |
| Build all | `mise run build` | ~30s |
| Coverage | `mise run coverage` or `./gradlew jacocoTestReport` | ~3min |
| Plugin verification | `mise run verify-plugin` | ~5min |

## Running

| Purpose | Command |
|---------|---------|
| MCP server (stdio) | `mise run run` or `./gradlew :community-mcp-server:bootRun` |
| MCP server (SSE) | `MCP_TRANSPORT=sse mise run run` |
| IntelliJ plugin (dev) | `mise run run-ide` or `./gradlew :premium-intellij-plugin:runIdeDev` |
| IntelliJ plugin (demo) | `mise run run-ide-demo` |
| Docker services | `mise run start-services` or `docker compose up -d` |
| Health check | `mise run health` or `curl http://localhost:8080/actuator/health` |

## Packaging

| Purpose | Command |
|---------|---------|
| MCP server JAR | `mise run assembly` or `./gradlew :community-mcp-server:bootJar` |
| IntelliJ plugin ZIP | `mise run build-plugin-zip` or `./gradlew :premium-intellij-plugin:buildPlugin` |

## Debugging

| Purpose | Command |
|---------|---------|
| View Docker logs | `mise run docker-logs` or `docker compose logs -f` |
| Run with debug logging | `LOG_LEVEL=debug ./gradlew :community-mcp-server:bootRun` |
| Check Docker status | `mise run docker-ps` or `docker compose ps` |
| Clean build artifacts | `mise run clean` or `./gradlew clean` |
| Fast test (single module) | `./gradlew :community-core-engine:fastTest` |
| Fast test (all modules) | `./gradlew :community-core-engine:fastTest :premium-core-engine:fastTest :premium-experiment-engine:fastTest :community-mcp-server:fastTest :premium-mcp-server:fastTest --configure-on-demand` |
| Profile build overhead | `./gradlew :community-core-engine:fastTest --profile --no-configuration-cache` |

## CRaC (Coordinated Restore at Checkpoint)

| Purpose | Command |
|---------|---------|
| Start server with checkpoint enabled | `mise run crac-start` |
| Trigger checkpoint | `mise run crac-checkpoint` |
| Restore from checkpoint | `mise run crac-restore` |
| JDK path | `/usr/lib/jvm/zulu-21-crac-amd64` |

## Common gotchas

- Gradle commands with `--no-configuration-cache` are needed for validation tasks (license headers, module boundaries).
- `MCP_TRANSPORT` environment variable selects the transport mode: `stdio` (default), `sse`, or `streamable-http`.
- Set `LOG_LEVEL=debug` before running for verbose diagnostic output.
- MCP stdio mode requires all logs go to stderr — stdout is reserved for JSON-RPC protocol messages.
- Docker services (PostgreSQL, Redis) are required for bug bounty features but NOT for MCP server operation.
