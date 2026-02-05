# Premium Core Module

**License**: Proprietary - All rights reserved

This module contains premium features for the Jakarta Migration tool. These features are not part of the open core and require a commercial license.

## Premium Features

### 1. Bytecode Analysis
- Advanced bytecode analysis using ASM
- Class file format validation
- Bytecode optimization detection
- Custom bytecode instrumentation

**Source Location**: `runtimeverification/service/impl/AsmBytecodeAnalyzer.java`

### 2. Runtime Verification
- Application health monitoring
- Runtime error detection and analysis
- Execution metrics collection
- Migration context tracking

**Source Location**: `runtimeverification/` (entire package)

### 3. Migration Phase/Strategy Logic
- Migration planning and strategy
- Phased migration execution
- Progress tracking and reporting
- Rollback capabilities

**Source Location**: `coderefactoring/` (entire package)

## Module Structure

```
premium-core/
├── build.gradle.kts         # Build configuration
├── src/main/java/
│   └── adrianmikula/jakartamigration/
│       ├── coderefactoring/         # Migration strategy (PREMIUM)
│       │   ├── domain/              # Domain models
│       │   └── service/            # Services
│       └── runtimeverification/     # Runtime verification (PREMIUM)
│           ├── domain/              # Domain models
│           └── service/             # Services
└── src/test/java/                  # Tests
```

## Building

```bash
# Build with premium features
mise run build-premium

# Build with tests
mise run build-premium-all
```

## Integration

The premium-core module depends on the community `migration-core` module:

```kotlin
dependencies {
    implementation(project(":migration-core"))
}
```

## License

This module is proprietary software. Unauthorized copying, distribution, or use is strictly prohibited.
