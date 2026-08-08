# Testing Guide

This document is the high-level entry point for testing in the Jakarta Migration project. For the full specification of packages, naming, and tags see [Test Organization Patterns](patterns/test-organization.md).

## Quick Reference

| Test kind | Package root | Class suffix | JUnit tag | Gradle task |
|---|---|---|---|---|
| Unit tests | `unit.<domain>` | `*UnitTest` | none | `fastTest` |
| Service integration (no external resources) | `integration.<domain>` | `*ServiceIntegrationTest` or `*BuildToolTest` | `slow` | `slowTest` |
| Real repository / network tests | `realrepo.<domain>` | `*RealRepositoryTest` | `slow` | `slowTest` |
| Memory / performance tests | `memory.<domain>` | `*MemoryTest` | `slow` | `slowTest` |

## Principles

- Test packages mirror the kind of test, then the production domain.
- Class names describe what external thing the test hits (real repo, build tool, database, memory) rather than the generic word "Integration".
- Keep `slow` / `integration` tags in sync with the class purpose so `fastTest` and `slowTest` tasks select the right subset.

## See also

- [Test Organization Patterns](patterns/test-organization.md) — full layout, naming, and anti-patterns.
- [skills/real-repo-integration-test/SKILL.md](../skills/real-repo-integration-test/SKILL.md) — guide for writing real-repo integration tests.