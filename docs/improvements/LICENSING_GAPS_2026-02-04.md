# Open-Core Licensing Gaps & Next Steps

**Date:** 2026-02-04  
**Based on:** [`docs/research/licensing-research.md`](../research/licensing-research.md)  
**Related:** [`OPENCORE_LICENSING_PLAN_2026-02-04.md`](OPENCORE_LICENSING_PLAN_2026-02-04.md)

---

## Implementation Status Summary

The open-core licensing structure is **strongly aligned** with the research recommendations:

| Recommendation | Status |
|----------------|--------|
| Apache License 2.0 | ✅ Complete |
| Clear contributor policy | ✅ Complete |
| Community/Premium separation | ✅ Complete |
| Enterprise governance ready | ✅ Complete |
| Premium upsell path | ✅ Complete |
| JetBrains Marketplace integration | ✅ Complete |

---

## Remaining Gaps (Priority Order)

### 1. Copyright Headers in Source Files ⚠️ MEDIUM

**Issue:** Source files lack Apache 2.0 copyright headers for stronger IP protection.

**Recommended Headers:**

```java
/*
 * Copyright 2026 Adrian Mikula
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

**Files to Update:**
- `migration-core/src/main/java/**/*.java` (high priority)
- `mcp-server/src/main/java/**/*.java` (high priority)
- `intellij-plugin/src/main/java/**/*.java` (high priority)

**Effort:** Medium | **Priority:** MEDIUM

---

### 2. NOTICE File ⚠️ LOW

**Issue:** Enterprise users often expect a NOTICE file per Apache 2.0 conventions.

**Recommended Content:**

```markdown
# NOTICE

Jakarta Migration MCP
Copyright 2026 Adrian Mikula

This product includes software developed at
Adrian Mikula (https://github.com/adrianmikula)
```

**Location:** [`NOTICE`](NOTICE)

**Effort:** LOW | **Priority:** LOW

---

### 3. Contributor License Agreement (CLA) Template ⚠️ MEDIUM

**Issue:** CONTRIBUTING.md mentions CLA but lacks a template or link.

**Options:**

1. **Simple approach:** Add a link to a Google Form or GitHub Issue template for CLA
2. **Dedicated CLA document:** Create [`CLA.md`](CLA.md) with Individual CLA template

**Recommended CLA Content:**

```markdown
# Individual Contributor License Agreement

Thank you for your contribution to Jakarta Migration MCP.

By signing this agreement, you grant Adrian Mikula a license to use your
contribution under the Apache License 2.0.

**Agreement:**

1. You represent that you are legally entitled to grant the license.

2. Your contribution is your original work or you have the right to submit.

3. You grant Adrian Mikula a perpetual, worldwide, non-exclusive,
   royalty-free license under Apache License 2.0.

**Signature:** ____________________

**Date:** ____________________

**Full Name:** ____________________
```

**Effort:** MEDIUM | **Priority:** MEDIUM

---

### 4. Premium Module Directory Structure ⚠️ LOW

**Issue:** `settings.gradle.kts` has commented premium modules but no directories.

**Recommendation:** Create empty placeholder directories:

```
premium-engine/
├── src/main/java/
│   └── adrianmikula/jakartamigration/premium/
│       └── README.md  # "Proprietary - Do not contribute"
└── build.gradle.kts

premium-intellij/
├── src/main/java/
│   └── adrianmikula/jakartamigration/premium/
│       └── README.md  # "Proprietary - Do not contribute"
└── build.gradle.kts
```

**Effort:** LOW | **Priority:** LOW

---

## Priority Matrix

| Task | Effort | Priority | Impact |
|------|--------|----------|--------|
| Copyright headers | Medium | MEDIUM | IP protection |
| CLA template | Medium | MEDIUM | Legal clarity |
| NOTICE file | Low | LOW | Enterprise trust |
| Premium directories | Low | LOW | Code organization |

---

## Recommended Next Steps

1. **Week 1:** Add copyright headers to core domain classes
2. **Week 2:** Create CLA template and link from CONTRIBUTING.md
3. **Week 3:** Add NOTICE file and premium module placeholders

---

## References

- [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0)
- [Apache Foundation NOTICE Files](https://www.apache.org/dev/licensing-howto.html)
- [Research: Apache 2.0 vs MIT/BSD](../research/licensing-research.md)
