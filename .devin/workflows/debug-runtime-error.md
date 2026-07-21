# Debug Runtime Error

## Workflow
1. **Check prerequisites** — Verify services are running:
   - `docker compose ps` or `curl http://localhost:8080/actuator/health`
2. **Gather diagnostics** — Collect error state:
   - Use MCP tools if available: `get_errors` → `get_logs`
   - Enable debug mode: `LOG_LEVEL=debug`
   - Check Docker logs: `docker compose logs -f`
   - Review known issues: `docs/troubleshooting/COMMON_ISSUES.md`
3. **Analyze root cause** — Identify the source from logs
4. **Apply fix** — Implement the correction
5. **Verify** — Confirm the fix:
   - Run fast tests: `./gradlew :community-core-engine:fastTest`
   - Verify service health: `curl http://localhost:8080/actuator/health`

## MCP Server Access
Configure MCP servers in `.devin/config.json`:
- `gradle` — Build/test/lint
- `github` — Issue/PR context
- `filesystem` — File operations
- `postgres` — Database queries
- `codebase-indexer` — Code search
