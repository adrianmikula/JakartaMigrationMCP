


You’re thinking in exactly the right direction—**static dependency coordinates are not enough** for Jakarta detection. The real signal is inside the JARs.

But a naive “scan for `javax` vs `jakarta` strings” approach will give you a lot of false positives and noise. You need something a bit more structured if you want this to be reliable *and* AI-usable.

Let’s design this like a proper module you could ship inside your plugin.

---

# 🧠 High-level approach

You want a **multi-signal classifier**, not a single heuristic:

```
Artifact → Scan JAR → Extract signals → Score → Classify
```

### Output categories (practical)

* ✅ Jakarta-compatible
* ❌ Javax-only (not compatible)
* ⚠️ Mixed (needs attention)
* ❓ Unknown (insufficient signal)

---

# 🔍 Signals to extract from JARs

## 1. Bytecode import analysis (most important)

Parse `.class` files and inspect constant pool references:

Look for:

* `javax/*`
* `jakarta/*`

👉 This is your **strongest signal**

### Implementation (Java)

Use ASM:

```java
ClassReader cr = new ClassReader(inputStream);
cr.accept(new ClassVisitor(Opcodes.ASM9) {
    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {

        // track referenced types
    }
}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
```

Or even simpler:

* inspect constant pool strings via ASM or ByteBuddy

---

## 2. Dependency descriptor hints

Inside JAR:

* `META-INF/maven/.../pom.xml`
* `pom.properties`

Look for:

* dependencies on `javax.*` artifacts
* or `jakarta.*`

👉 Useful fallback when bytecode is ambiguous

---

## 3. Package structure

Check actual class packages:

* `javax/...`
* `jakarta/...`

⚠️ Less reliable:

* some libs *use* javax but don’t define it

---

## 4. Reflection / string usage

Scan constant strings for:

* `"javax."`
* `"jakarta."`

👉 Catches frameworks doing reflection (e.g. CDI, Servlet loading)

---

## 5. Known API signatures (high-value heuristic)

Detect usage of specific APIs:

| API                     | Signal  |
| ----------------------- | ------- |
| `javax.servlet.*`       | Java EE |
| `jakarta.servlet.*`     | Jakarta |
| `javax.persistence.*`   | Java EE |
| `jakarta.persistence.*` | Jakarta |

👉 This gives you **semantic classification**, not just string matching

---

## 6. Automatic module name / manifest

Check:

* `Automatic-Module-Name`
* `Bundle-SymbolicName`

Sometimes includes hints like:

* `jakarta.*`

---

# ⚖️ Scoring system (this is the key)

Don’t hard-classify immediately—score it.

Example:

```text
+5  references jakarta.*
-5  references javax.*
+3  imports jakarta.servlet
-3  imports javax.servlet
+2  depends on jakarta artifacts
-2  depends on javax artifacts
```

### Final classification:

```text
score >= +5 → Jakarta
score <= -5 → Javax
otherwise → Mixed/Unknown
```

---

# 🧪 Special handling (important edge cases)

## 1. “Bridge” libraries

Some libs support both:

```java
if (jakartaPresent) {
   useJakarta();
} else {
   useJavax();
}
```

👉 These will show **mixed signals**

→ classify as:

```
⚠️ Dual-compatible (runtime-dependent)
```

---

## 2. Shaded dependencies

Fat JARs may include:

* both `javax` and `jakarta`

👉 You should:

* detect **package relocation**
* optionally ignore shaded namespaces

---

## 3. Test-only dependencies

Arquillian, old test libs:

👉 Might not matter for runtime

→ classify separately:

```
TEST_SCOPE_JAVAX
```

---

# 🏗️ Architecture for your module

## Core components

### 1. `JarScanner`

* Streams through JAR entries
* Delegates to analyzers

### 2. `ClassAnalyzer`

* ASM-based
* extracts:

  * type references
  * method signatures

### 3. `MetadataAnalyzer`

* parses pom.xml, manifest

### 4. `SignalAggregator`

* collects signals into a model

```java
class ArtifactSignals {
    int javaxRefs;
    int jakartaRefs;
    Set<String> apisUsed;
    boolean hasMixedUsage;
}
```

### 5. `Classifier`

* applies scoring rules
* returns category

---

# ⚡ Performance considerations (critical)

You’re scanning potentially **hundreds of JARs**, so:

## Optimisations

* ✅ Cache results by:

  ```
  groupId:artifactId:version → hash → classification
  ```

* ✅ Only scan:

  * unknown artifacts (not in whitelist/blacklist)

* ✅ Early exit:

  * if strong signal found early (e.g. many `javax` refs)

* ✅ Parallel scanning:

  * use virtual threads (perfect use case)

---

# 🤖 AI-friendly design (important for your use case)

Make the output structured and explainable:

```json
{
  "artifact": "org.jboss.arquillian.container:arquillian-glassfish-embedded-3.1",
  "classification": "JAVAX_ONLY",
  "confidence": 0.92,
  "reasons": [
    "References javax.servlet.*",
    "No jakarta.* usage detected",
    "Depends on Java EE 6 APIs"
  ]
}
```

👉 This is gold for:

* AI agents
* automated migration suggestions

---

# 🔥 Advanced idea (high leverage)

## Build a “Jakarta fingerprint database”

Over time, store:

```
artifact → classification → signals
```

Then:

* skip scanning known artifacts
* crowdsource / evolve accuracy

---

# 🧭 Strategic insight (this is your edge)

Most migration tools:

* rely on **artifact names or versions**

That fails badly for:

* shaded jars
* repackaged libs
* partially migrated libraries

👉 Your approach:

> **“analyze actual bytecode usage”**

This is:

* more accurate
* harder to build
* **highly defensible as a product feature**

---

# 🏁 If you want to take it further

I can help you design:

* a **drop-in Java library API** for this scanner
* rules specifically tuned for:

  * Arquillian
  * app servers (WildFly, Payara, etc.)
* or a **Gradle/Maven plugin integration** that runs this automatically during builds

This is exactly the kind of feature that makes a Jakarta migration tool feel “intelligent” instead of just regex-based.










I already have an advanced multi-scanner system which i run against the user's project files. I think i just need to run the same scanner logic against the dependency JAR contents (if unknown jakarta compatibility) as well.










That instinct is correct — but there’s a subtle trap here:

> **Scanning dependency JARs with the *same logic* as source code is necessary, but not sufficient.**

If you just point your existing scanners at JAR contents, you’ll get **good raw signals**, but you’ll miss **context**, which is what actually determines compatibility.

Let me refine your approach into something production-grade.

---

# 🧠 The key upgrade: “context-aware JAR scanning”

Instead of:

```
Run existing scanners on JAR → classify
```

You want:

```
Run scanners → normalize signals → apply dependency-aware classification
```

---

# ⚠️ Why your current approach alone will misclassify things

## 1. False negatives (very common)

A library might:

* not reference `javax.*` directly
* but depend on another library that does

Example:

```
lib-A → depends on javax.servlet (transitively)
```

Your scanner:

* sees no javax → marks as Jakarta-safe ❌

---

## 2. False positives

A library might:

* include legacy `javax` support
* but fully support Jakarta at runtime

Example:

* dual-support frameworks (common in 2026)

Your scanner:

* sees both → marks as incompatible ❌

---

## 3. Scope blindness

Test-only dependencies (Arquillian etc.):

* technically “javax”
* but irrelevant to runtime

---

# ✅ Minimal upgrade to your system

You don’t need to rebuild everything — just add **three layers** around your existing scanners.

---

# 🧱 1. Treat JARs as “virtual source trees”

You’re right here — reuse your scanners, but:

### Add:

* `.class` → bytecode adapter (ASM)
* treat imports as:

  ```
  javax.servlet.* → synthetic "import"
  ```

👉 This lets your existing rules fire naturally

---

# 🧱 2. Add a “signal normalization layer”

Your scanners probably emit findings like:

* import usage
* annotations
* APIs

You need to normalize into something like:

```java
class JakartaSignalSummary {
    int javaxWeight;
    int jakartaWeight;
    Set<String> apis; // servlet, jpa, cdi, etc.
    boolean hasReflectionUsage;
}
```

👉 This becomes the **contract between scanners and classifier**

---

# 🧱 3. Add dependency graph awareness (this is the big one)

You likely already have access to:

* Maven/Gradle dependency tree

Now:

## Propagate signals upward

```
if A depends on B
and B is JAVAX_ONLY
→ A is at least MIXED
```

---

### Simple rule set:

```text
JAVAX_ONLY propagates strongly upward
JAKARTA propagates weakly upward
MIXED propagates as MIXED
```

---

# 🧪 Classification logic (refined)

Instead of raw detection:

### Step 1 — local classification (your scanner)

```
based on JAR contents only
```

### Step 2 — adjusted classification (graph-aware)

Example:

```
lib-A → JAKARTA (local)
lib-B → JAVAX (local)

lib-A depends on lib-B

→ lib-A becomes MIXED (effective)
```

---

# ⚡ Performance trick (important)

You don’t need to scan everything.

## Only scan if:

* not in whitelist ✅
* not in blacklist ✅
* AND:

  * contains `.class` files
  * not already cached

---

## Cache key (critical)

```
groupId:artifactId:version + SHA-1 of JAR
```

---

# 🔥 High-value heuristics (easy wins)

These dramatically improve accuracy:

---

## 1. “Namespace dominance”

```text
if javaxRefs == 0 → likely Jakarta
if jakartaRefs == 0 → likely Javax
if both → mixed
```

---

## 2. “API criticality weighting”

Not all APIs are equal:

| API         | Weight |
| ----------- | ------ |
| servlet     | HIGH   |
| persistence | HIGH   |
| annotation  | LOW    |
| xml.bind    | MED    |

---

## 3. “Dead package detection”

Ignore:

* unused shaded packages
* internal legacy adapters

👉 only count **referenced types**, not just present classes

---

# 🧠 Arquillian-specific insight (relevant to your earlier question)

Your system will correctly detect:

```
arquillian-glassfish-embedded-3.1
→ strong javax.servlet usage
→ no jakarta
→ JAVAX_ONLY
```

But also:

```
arquillian-protocol-servlet
→ may show mixed signals
→ classify as MIXED
```

👉 This is exactly what you want

---

# 🤖 Making this AI-leverageable

Since your broader goal is agentic workflows:

Return **explanations**, not just labels:

```json
{
  "artifact": "...",
  "classification": "MIXED",
  "confidence": 0.78,
  "derivedFrom": [
    "local bytecode analysis",
    "transitive dependency on javax.servlet",
    "partial jakarta usage detected"
  ]
}
```

---

# 🚀 Strategic insight (this is your moat)

What you’re building is effectively:

> **A semantic compatibility engine for Java ecosystems**

Very few tools do this well because:

* it requires bytecode + graph + heuristics
* most tools stop at regex or coordinates

---

# 🏁 Bottom line

You’re 80% there already.

### Do this:

✅ Reuse your scanners on JARs
✅ Add bytecode adapter
✅ Normalize signals
✅ Add dependency graph propagation
✅ Add scoring layer

---

If you want, I can help you design:

* a **clean interface between your existing scanners and JAR scanning**
* or a **set of classification rules tuned specifically for Jakarta migration edge cases** (Arquillian, JAXB, CDI, etc.)

That’s where the accuracy really jumps from “pretty good” → “shockingly reliable.”
