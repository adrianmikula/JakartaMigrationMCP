# Open-Core Licensing Implementation Plan

**Date:** 2026-02-04  
**Based on:** [`docs/research/licensing-research.md`](../research/licensing-research.md)

---

## Executive Summary

The research recommends **Apache License 2.0** for the open-source core with premium features gated via JetBrains Marketplace subscription. Current codebase already has:
- ✅ Apache 2.0 license ([`LICENSE.md`](LICENSE.md))
- ✅ Feature flags system ([`FeatureFlag.java`](migration-core/src/main/java/adrianmikula/jakartamigration/config/FeatureFlag.java))
- ✅ COMMUNITY/PREMIUM tier structure ([`FeatureFlagsProperties.java`](migration-core/src/main/java/adrianmikula/jakartamigration/config/FeatureFlagsProperties.java))
- ⚠️ Premium module empty (`jakarta-migration-mcp-premium/`)
- ⚠️ Repository not structured for open-core

---

## Current State Analysis

### ✅ Already Implemented

| Component | Status | Location |
|-----------|--------|----------|
| Apache 2.0 License | ✅ | [`LICENSE.md`](LICENSE.md) |
| Feature Flag System | ✅ | [`FeatureFlag.java`](migration-core/src/main/java/adrianmikula/jakartamigration/config/FeatureFlag.java) |
| License Tiers | ✅ | [`FeatureFlagsProperties.java`](migration-core/src/main/java/adrianmikula/jakartamigration/config/FeatureFlagsProperties.java) |
| Pricing ($49/mo, $399/yr) | ✅ | [`FeatureFlagsProperties.java:34`](migration-core/src/main/java/adrianmikula/jakartamigration/config/FeatureFlagsProperties.java:34) |
| 7-Day Free Trial | ✅ | [`FeatureFlagsProperties.java:40`](migration-core/src/main/java/adrianmikula/jakartamigration/config/FeatureFlagsProperties.java:40) |
| Community Features | ✅ | `analyzeJakartaReadiness`, `detectBlockers`, `recommendVersions` |

### ⚠️ Needs Attention

| Component | Gap | Priority |
|-----------|-----|----------|
| Module Structure | Premium module empty, not in `settings.gradle.kts` | HIGH |
| Code Splitting | Premium features mixed in `JakartaMigrationTools` | HIGH |
| Marketplace Integration | License validation endpoint not implemented | MEDIUM |
| Community Boundaries | No clear API interface for open-core | MEDIUM |
| Documentation | No LICENSE file in each module | LOW |

---

## Recommended Open-Core Structure

### Phase 1: Repository Structure (Before v1 Release)

```
jakarta-migration-parent/          # Apache 2.0
├── LICENSE.md                       # Root Apache 2.0
├── settings.gradle.kts
├── build.gradle.kts
│
├── migration-core/                 # Apache 2.0 (Community)
│   ├── LICENSE                    # Apache 2.0
│   ├── src/main/java/...
│   │   └── adrianmikula/jakartamigration/
│   │       ├── config/            # Feature flags (PUBLIC)
│   │       ├── sourcecodescanning/ # AST scanning (COMMUNITY)
│   │       ├── dependencyanalysis/  # Dependency analysis (COMMUNITY)
│   │       └── coderefactoring/    # Basic refactoring (COMMUNITY)
│   └── build.gradle.kts
│
├── mcp-server/                     # Apache 2.0 (Community)
│   ├── LICENSE                    # Apache 2.0
│   ├── src/main/java/...
│   │   └── adrianmikula/jakartamigration/
│   │       └── mcp/
│   │           └── JakartaMigrationTools.java # Community tools only
│   └── build.gradle.kts
│
└── intellij-plugin/               # Apache 2.0 (Community)
    ├── LICENSE                    # Apache 2.0
    ├── src/main/java/...
    └── build.gradle.kts
```

### Phase 2: Premium Module (Proprietary)

```
jakarta-migration-premium/         # PROPRIETARY (NOT in public repo)
├── LICENSE                        # PROPRIETARY
├── settings.gradle.kts
│
├── premium-engine/                # Proprietary rules & orchestration
│   ├── src/main/java/...
│   │   └── adrianmikula/jakartamigration/premium/
│   │       ├── rules/            # Advanced migration rules
│   │       ├── orchestration/    # One-click migration
│   │       └── compliance/       # Enterprise compliance
│   └── build.gradle.kts
│
└── premium-intellij/              # JetBrains Marketplace paid plugin
    ├── src/main/java/...
    │   └── adrianmikula/jakartamigration/premium/
    │       ├── licensing/        # Marketplace validation
    │       └── advanced-ui/      # Premium UI features
    └── build.gradle.kts
```

---

## Implementation Steps (Completed)

### ✅ Step 1: Add LICENSE Files to Each Module

**Completed:** 2026-02-04

- [`migration-core/LICENSE`](migration-core/LICENSE) - Apache 2.0
- [`mcp-server/LICENSE`](mcp-server/LICENSE) - Apache 2.0
- [`intellij-plugin/LICENSE`](intellij-plugin/LICENSE) - Apache 2.0

---

### ✅ Step 2: Split Community vs Premium Tools

**Completed:** 2026-02-04

- Created [`CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java) with:
  - `analyzeJakartaReadiness` ✅
  - `detectBlockers` ✅
  - `recommendVersions` ✅

- Updated [`JakartaMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java) to:
  - Delegate community tools to `CommunityMigrationTools`
  - Keep premium tools (createMigrationPlan, analyzeMigrationImpact, verifyRuntime, applyAutoFixes, executeMigrationPlan)
  - Add license checks for premium tools

---

### ✅ Step 3: Implement Marketplace License Validation

**Completed:** 2026-02-04

- Created [`MarketplaceLicenseService.java`](migration-core/src/main/java/adrianmikula/jakartamigration/config/MarketplaceLicenseService.java)
- Includes license validation simulation for development
- Ready for integration with JetBrains Plugin Repository API

---

### ✅ Step 4: Document Community Boundaries

**Completed:** 2026-02-04

- Created [`COMMUNITY_FEATURES.md`](../../COMMUNITY_FEATURES.md) with:
  - Community features documentation
  - MCP tool descriptions and examples
  - Premium features comparison
  - Pricing information

---

### ✅ Step 6: Contributor Policy

**Completed:** 2026-02-04

- Created [`CONTRIBUTING.md`](../../CONTRIBUTING.md) with:
  - Contribution guidelines
  - Community vs Premium module clarification
  - Licensing terms

---

## Remaining Steps

### Step 5: Update Gradle Configuration

**Priority:** MEDIUM | **Effort:** LOW

TODO: Update [`settings.gradle.kts`](../../settings.gradle.kts) to conditionally include premium modules.

---

## Feature Matrix

| Feature | Community | Premium | Implementation | Status |
|---------|-----------|---------|----------------|--------|
| `analyzeJakartaReadiness` | ✅ | ✅ | `CommunityMigrationTools` | ✅ Complete |
| `detectBlockers` | ✅ | ✅ | `CommunityMigrationTools` | ✅ Complete |
| `recommendVersions` | ✅ | ✅ | `CommunityMigrationTools` | ✅ Complete |
| `createMigrationPlan` | ❌ | ✅ | `JakartaMigrationTools` | ✅ Premium |
| `analyzeMigrationImpact` | ❌ | ✅ | `JakartaMigrationTools` | ✅ Premium |
| `verifyRuntime` | ❌ | ✅ | `JakartaMigrationTools` | ✅ Premium |
| `applyAutoFixes` | ❌ | ✅ | `JakartaMigrationTools` | ✅ Premium |
| `executeMigrationPlan` | ❌ | ✅ | `JakartaMigrationTools` | ✅ Premium |
| Marketplace Validation | ❌ | ✅ | `MarketplaceLicenseService` | ✅ Complete |
| Trial Management | ✅ | ✅ | `FeatureFlagsProperties` | ✅ Existing |

---

## Testing Checklist

Before v1 release:

- [x] All Apache 2.0 LICENSE files in place
- [x] Community tools accessible without license
- [x] Premium tools return upgrade prompt without license
- [ ] Integrate `MarketplaceLicenseService` with `FeatureFlagsService`
- [ ] Trial activation flow works
- [ ] Marketplace validation endpoint ready (sandbox)
- [ ] IntelliJ plugin displays upgrade banner
- [ ] Documentation clearly states licensing terms
- [ ] No proprietary code in community modules

---

## Timeline

| Phase | Tasks | Duration |
|-------|-------|----------|
| 1 | Add LICENSE files, update CONTRIBUTING | 1 day |
| 2 | Split JakartaMigrationTools | 2 days |
| 3 | Marketplace validation | 3 days |
| 4 | Documentation & testing | 2 days |

**Total estimated:** ~1 week before v1 release

## JetBrains Marketplace Integration Details

### API Endpoint

```
POST https://plugins.jetbrains.com/api/license/validate
Content-Type: application/json

{
    "licenseId": "YOUR_LICENSE_KEY",
    "pluginId": "YOUR_PLUGIN_ID",
    "userId": "OPTIONAL_USER_ID"
}
```

### Response Format

```json
{
    "licenseId": "...",
    "status": "VALID|EXPIRED|INVALID",
    "customerName": "...",
    "customerEmail": "...",
    "orderId": "...",
    "saleId": "...",
    "type": "COMMERCIAL|PERSONAL|SUBSCRIPTION",
    "expirationDate": "2027-01-01T00:00:00Z",
    "renewalDate": "2026-12-01T00:00:00Z",
    "gracePeriodEndDate": "2027-01-15T00:00:00Z"
}
```

### Implementation Notes

1. **Sandbox Testing:** Use JetBrains sandbox environment for testing
2. **Caching:** Cache validation results for 1 hour to reduce API calls
3. **Grace Period:** Handle `gracePeriodEndDate` for expired subscriptions
4. **Offline Mode:** Support offline validation with cached license data

**Reference:** [JetBrains Plugin License Validation](https://plugins.jetbrains.com/docs/marketplace/license-validation.html)

---

## References

- [Research: Apache 2.0 vs MIT/BSD](../research/licensing-research.md)
- [JetBrains Marketplace Documentation](https://plugins.jetbrains.com/docs/marketplace/)
- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
