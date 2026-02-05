


Yes — **you can**, but **only if you structure it correctly**.
Doing this *wrong* will accidentally grant Apache-2.0 rights to your premium plugin. Doing it *right* is common and defensible.

Below is the **safe, industry-standard open-core layout**, tailored for **IntelliJ plugins**.

---

## The core rule (don’t violate this)

> **Apache-2.0 applies to every file it covers.**

If a source file is Apache-licensed, you **cannot later restrict it**.

So the premium plugin code must **never** be covered by Apache-2.0 headers or implied scope.

---

# ✅ Safe patterns (use one of these)

## **Option A — Single repo, hard license boundary (recommended)**

This is the most common open-core approach.

```
jakarta-migration/
├─ LICENSE            # Apache-2.0 (root)
├─ NOTICE
├─ open-core/
│  ├─ LICENSE         # Apache-2.0 (explicit)
│  └─ src/...
├─ intellij-plugin-core/
│  ├─ LICENSE         # Apache-2.0
│  └─ src/...
├─ intellij-plugin-premium/
│  ├─ LICENSE         # Proprietary / Commercial
│  ├─ README.md       # Explicit license statement
│  └─ src/...
```

### Why this works

* Apache license **does not bleed upward**
* Each module declares its own license
* Courts and marketplaces recognize directory-scoped licenses

### Required additions

In `intellij-plugin-premium/README.md`:

```text
This module is NOT licensed under Apache-2.0.
All rights reserved.
```

---

## **Option B — Same repo, premium code as a binary-only module**

Useful if you want *visibility* but not source access.

```
intellij-plugin-premium/
├─ build.gradle
├─ premium-api.jar   # closed source
├─ LICENSE           # Commercial
```

✔ Legal
✔ Marketplace-safe
✔ Harder to copy

---

## **Option C — Separate repo (cleanest, but not required)**

```
jakarta-migration-open/
jakarta-migration-intellij-premium/
```

Lowest risk, but slower iteration.

---

# ❌ What you must NOT do

### 🚫 1. Put premium code under Apache headers

If a file contains:

```java
// Licensed under the Apache License, Version 2.0
```

It is **irreversibly Apache-licensed**.

---

### 🚫 2. Rely on “README says proprietary” alone

License scanners ignore README-only claims.

---

### 🚫 3. Mix Apache + proprietary files in the same package

This creates derivative-work ambiguity.

Keep **separate packages and modules**.

---

# IntelliJ Plugin–specific guidance

## plugin.xml

You may reference Apache modules from proprietary ones.

✔ Allowed
✔ Normal

Apache-2.0 explicitly permits this.

---

## Marketplace compliance

JetBrains allows:

* Open-core plugins
* Freemium features
* License-gated functionality

They **do not** require source disclosure.

---

# Recommended license text for premium module

`intellij-plugin-premium/LICENSE`:

```text
Copyright © 2026 Adrian Mikula

All rights reserved.

This software is proprietary and may not be used, copied,
modified, or distributed except under the terms of a
separate commercial license agreement.
```

---

# Contributor PR safety

To avoid accidental contamination:

* Accept PRs **only** into Apache modules
* Do **not** accept PRs into premium modules
* Optional: add `CONTRIBUTING.md` stating this explicitly

---

# TL;DR decision table

| Setup                                | Safe | Recommended |
| ------------------------------------ | ---- | ----------- |
| Same repo, separate licensed modules | ✅    | ⭐⭐⭐⭐        |
| Same repo, mixed licenses per file   | ⚠️   | ❌           |
| Separate repos                       | ✅    | ⭐⭐⭐         |
| Apache root, no sub-licenses         | ❌    | 🚫          |

---

## Bottom line

✔ Yes, you can keep everything in one public repo
✔ Only if **premium code has its own LICENSE and directory**
✔ Apache-2.0 will not “infect” proprietary modules if boundaries are explicit

If you want, I can:

* Review your actual repo tree
* Draft LICENSE/README files
* Check JetBrains Marketplace compatibility
* Help you design a license-gated IntelliJ feature model

Just say.



