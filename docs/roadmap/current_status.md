# Roadmap Implementation Status

*Last reviewed: 2026-08-08*

This document summarizes the implementation status of the features tracked in the `docs/roadmap/` folder.

## High-Level Status

| Roadmap / Feature | File | Status | Notes |
|---|---|---|---|
| Core Analysis - Bytecode JAR scanning | `deep_jar_scanning.md` | Completed | Baseline JAR scanning in place |
| Core Analysis - Transitive dependency analysis | `dependency-scanning-overhaul.md` | Partially Done | Coordinate mapping, build tool fallback and UI hookup partially complete |
| Core Analysis - Build file parsing | `dependency-scanning-overhaul.md` | Completed | Maven/Gradle parsing supported |
| Core Analysis - Platform detection | (various) | Completed | Tomcat/JBoss/WebSphere detection in place |
| Dependency tree view | `dependency_tree_view.md` | Completed | UI tree tab implemented and wired |
| HTML report generation | `redesign_pdf_reports.md` | In Progress | PDF replaced by HTML path; report generation needs templates |
| Risk score visualization | `conversion_ui_features.md` | Planned | 4 risk dimensions designed, not yet in UI |
| Professional report templates | `mosernise_reports_template_engine.md` | Planned | JTE/snippet evaluation done, no implementation yet |
| Executive summary generation | `redesign_pdf_reports.md` | Planned | Layout and sections defined, not implemented |
| Test coverage integration | `conversion_ui_features.md` | Planned | Validation confidence dimension, not implemented |
| Critical risk zone detection | `conversion_ui_features.md` | Planned | High risk + low coverage not yet surfaced |
| Migration effort estimation | `conversion_ui_features.md` | Planned | Test effort and confidence not yet in UI |
| Recipe recommendations linked to scans | `jakarta-detection-improvements.md` | Partially Done | Baseline wiring in place, full UI and scoring still open |
| Gradle Tooling API migration | `gradle-tooling-api-migration.md` | Completed | Migrated to Tooling API; legacy Gradle code removed |
| Virtual threads for deep JAR scanning | `virtual_threads_performance.md` | Blocked | Blocked on Java 21; IntelliJ Platform requires Java 17 |
| CI static analysis | `ci_static_analysis.md` | Planned | Recommendations documented, not implemented |
| UI - Scan progress bar | `scan-progress-improvements.md` | Planned | Coarse progress reporting, missing phase updates, and throttling race documented |
| Senior dev trust / positioning | `senior_dev_trust.md` | Planned | Strategy document, no product changes yet |

## Completion Summary

- **Completed:** Core analysis (Phase 1) and dependency tree view
- **In Progress / Partially Done:** Enhanced reporting, Jakarta detection accuracy, dependency scanning overhaul
- **Blocked:** Virtual threads (Java 21)
- **Planned / Not Started:** CI tooling, report templates, test coverage integration
- **Completed:** Gradle Tooling API migration (all four phases)

## Next Potential Focus Areas

1. Complete HTML report templates and executive summary (`redesign_pdf_reports.md`, `mosernise_reports_template_engine.md`)
2. Implement test coverage integration and critical risk zone (`conversion_ui_features.md`)
3. Resolve or work around Java 21 blocker for virtual threads (`virtual_threads_performance.md`)
4. Execute Gradle Tooling API migration (`gradle-tooling-api-migration.md`)
