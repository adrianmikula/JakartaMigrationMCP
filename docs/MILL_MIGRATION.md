# Mill Build Migration

This project uses **Mill** as the build tool (replacing Gradle) for faster startup and incremental builds.

## Quick reference

| Action | Command |
|--------|--------|
| Compile | `mill jakartaMigrationMcp.compile` |
| Run tests | `mill jakartaMigrationMcp.test` |
| Run app | `mill jakartaMigrationMcp.run` |
| Build fat JAR | `mill jakartaMigrationMcp.assembly` |
| Clean | `mill clean` |

With **mise**: `mise run test`, `mise run build`, `mise run assembly`, etc.

## Install Mill

- **Via mise** (recommended): `mise install` in the project root (see `.mise.toml`).
- **Bootstrap script** (Unix):  
  `curl -L https://repo1.maven.org/maven2/com/lihaoyi/mill-dist/0.12.17/mill-dist-0.12.17-mill.sh -o mill && chmod +x mill`  
  Then run `./mill` (it will use `.mill-version`).
- **Windows**: Use `mill.bat` in the project root (zero-install; downloads Mill on first run), or install via scoop/chocolatey.

### Testing Mill (compile, test, assembly)

Run these to verify Mill works for all dev tasks:

```bash
mill jakartaMigrationMcp.compile    # or .\mill.bat on Windows
mill jakartaMigrationMcp.test
mill jakartaMigrationMcp.assembly
```

On **Windows**, if you see `value ivy is not a member of StringContext` when compiling the build, Mill’s build-script compilation may be using a limited classpath on this platform. Use **WSL** or rely on **CI** (Linux) for full compile/test/assembly; the same `build.sc` runs in GitHub Actions.

## What changed

- **Build file**: `build.sc` (Scala) replaces `build.gradle.kts`. Dependencies and tasks are in one place.
- **Output**: Mill writes to `out/` (e.g. `out/jakartaMigrationMcp/assembly.dest/jakarta-migration-mcp-1.0.0.jar`). Gradle’s `build/` is no longer used by Mill.
- **CI / Release**: GitHub Actions use the Mill bootstrap script and run `mill jakartaMigrationMcp.test` and `mill jakartaMigrationMcp.assembly`.
- **Code quality / coverage**: Gradle’s checkstyle, PMD, SpotBugs, JaCoCo, and OWASP checks are not yet ported to Mill. They can be re-added later via Mill’s linting/coverage support or external scripts.

## Gradle removal (optional)

After you’re happy with Mill:

You can keep Gradle files temporarily and run both tools if you need a fallback.

### Checklist

**Root:**
- [ ] `build.gradle.kts`
- [ ] `settings.gradle.kts`
- [ ] `gradle.properties`
- [ ] `gradle/` (wrapper and wrapper properties)
- [ ] `gradlew`, `gradlew.bat`

**Scripts (Gradle-only):**
- [ ] `scripts/gradle-build.ps1`
- [ ] `scripts/gradle-clean.ps1`
- [ ] `scripts/gradle-code-quality.ps1`, `scripts/gradle-code-quality.sh`
- [ ] `scripts/gradle-coverage.ps1`
- [ ] `scripts/gradle-run.ps1`
- [ ] `scripts/gradle-test.ps1`

**Config (optional):**  
`config/checkstyle/`, `config/pmd/`, `config/spotbugs/`, `config/owasp/` are used by Gradle code-quality. Keep them if you plan to reintroduce checks via Mill or external tools; remove if you no longer need them.

**.gitignore:**  
Remove or simplify Gradle-related lines (e.g. `.gradle/`, `build/`, JaCoCo paths) once you no longer use Gradle.

## Version for release

The version is defined in `build.sc` as `def version = "1.0.0"`. The release workflow overwrites it from the git tag (e.g. `v1.0.0`) before building the JAR.
