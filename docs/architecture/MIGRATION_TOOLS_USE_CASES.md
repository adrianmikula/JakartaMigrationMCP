# Migration Tools Use Cases - LLM-Friendly Guide

## Overview

This document provides clear, LLM-friendly descriptions of when and why to use each migration tool. Optimized for agentic AI systems working on J2EE/Jakarta migration projects.

## Tool Selection Guide

### When Working with Source Code (.java files)

**Use: SIMPLE_STRING_REPLACEMENT or OPENREWRITE**

These tools work on Java source code files and can modify your original source code.

### When Working with Compiled JAR/WAR Files

**Use: APACHE_TOMCAT_MIGRATION**

This tool works on compiled bytecode, not source code. It's essential for compatibility testing and bytecode analysis.

---

## Tool 1: Simple String Replacement

### What It Does
- Refactors Java source code files (.java) by replacing javax.* with jakarta.*
- Basic string-based replacement
- Works directly on source code

### When to Use
- ✅ Quick refactoring of individual Java files
- ✅ Simple migrations with straightforward patterns
- ✅ Fallback when other tools aren't available
- ✅ Testing migration patterns

### When NOT to Use
- ❌ Complex refactoring scenarios
- ❌ Need for high accuracy
- ❌ Working with compiled JAR files
- ❌ Bytecode analysis

### Example Use Case
```
User: "Refactor this Java file from javax.servlet to jakarta.servlet"
AI: Uses SIMPLE_STRING_REPLACEMENT to modify the source code file
```

---

## Tool 2: Apache Tomcat Migration Tool

### ⚠️ CRITICAL: Works on COMPILED JAR/WAR Files, NOT Source Code

### What It Does
- Migrates compiled Java applications (JAR/WAR/EAR files) at the bytecode level
- Converts javax.* to jakarta.* in compiled class files
- Handles string constants, embedded config files, JSPs, TLDs
- Removes cryptographic signatures (expected behavior)

### Primary Use Cases

#### 1. Compatibility Testing & Library Assessment

**Scenario**: You have a compiled application and want to know if it will work after Jakarta migration.

**Process**:
1. Take your compiled JAR/WAR file
2. Run Apache tool to migrate it to Jakarta namespace
3. Try to run the migrated application
4. Identify which libraries fail or have compatibility issues
5. Determine which dependencies need Jakarta-compatible alternatives

**Why This Matters**:
- Some libraries may compile fine but fail at runtime after migration
- Binary incompatibilities only appear when running migrated bytecode
- Helps identify libraries that need replacement before migrating source code

**Example**:
```
AI Agent: "I need to check if this Spring Boot 2.7 WAR file will work after Jakarta migration"
Process:
1. Use APACHE_TOMCAT_MIGRATION to migrate the WAR file
2. Attempt to run the migrated WAR
3. Analyze runtime errors to identify incompatible libraries
4. Recommend Jakarta-compatible alternatives for failing libraries
```

#### 2. Bytecode Diff & Cross-Validation

**Scenario**: You've migrated source code and want to verify nothing was missed.

**Process**:
1. Compile original source code → original.jar
2. Compile migrated source code → migrated.jar
3. Use Apache tool to migrate original.jar → apache-migrated.jar
4. Compare bytecode between migrated.jar and apache-migrated.jar
5. Identify any differences (missed references, string constants, etc.)

**Why This Matters**:
- Validates that your source code migration was complete
- Catches javax.* references in string constants that source analysis might miss
- Ensures bytecode-level consistency
- Cross-validates against our own bytecode analysis

**Example**:
```
AI Agent: "I've migrated the source code, but want to verify I didn't miss anything"
Process:
1. Compile both original and migrated source
2. Use APACHE_TOMCAT_MIGRATION on original JAR
3. Compare bytecode of our migration vs Apache tool migration
4. Report any differences found
```

#### 3. Third-Party Library Compatibility Check

**Scenario**: You want to test if a third-party library JAR is Jakarta-compatible.

**Process**:
1. Take the third-party library JAR
2. Use Apache tool to migrate it
3. Test if the migrated JAR works with Jakarta dependencies
4. Identify compatibility issues

**Why This Matters**:
- Some libraries claim Jakarta compatibility but have hidden issues
- Binary-level testing reveals runtime problems
- Helps make informed decisions about library replacements

### When to Use
- ✅ Testing compiled application compatibility
- ✅ Bytecode-level validation
- ✅ Cross-checking source code migration completeness
- ✅ Assessing third-party library compatibility
- ✅ Identifying runtime issues before source migration

### When NOT to Use
- ❌ Migrating Java source code files (.java)
- ❌ Modifying original source code
- ❌ Updating Maven/Gradle dependencies
- ❌ Refactoring source code patterns

### Important Notes
- **Removes JAR signatures**: Cryptographic signatures are removed as bytecode changes invalidate them
- **Bytecode only**: Does not modify source code
- **One-way**: Creates a new migrated JAR, doesn't modify the original

### Example Use Cases for AI Agents

**Use Case 1: Pre-Migration Compatibility Check**
```
User: "Will my Spring Boot 2.7 application work after Jakarta migration?"
AI Process:
1. Build the application JAR
2. Use APACHE_TOMCAT_MIGRATION to migrate the JAR
3. Attempt to run the migrated JAR
4. Analyze errors to identify incompatible dependencies
5. Recommend Jakarta-compatible alternatives
```

**Use Case 2: Validation After Source Migration**
```
User: "I've migrated my source code, did I miss anything?"
AI Process:
1. Compile migrated source → migrated.jar
2. Use APACHE_TOMCAT_MIGRATION on original source → apache-migrated.jar
3. Compare bytecode between both
4. Report any javax.* references found in apache-migrated.jar that aren't in migrated.jar
```

**Use Case 3: Library Compatibility Assessment**
```
User: "Is library X compatible with Jakarta?"
AI Process:
1. Download library X JAR
2. Use APACHE_TOMCAT_MIGRATION to migrate it
3. Test migrated JAR with Jakarta dependencies
4. Report compatibility status and any issues found
```

---

## Tool 3: OpenRewrite

### What It Does
- AST-based refactoring of Java source code
- Comprehensive Jakarta migration recipes
- Works on source code files (.java, .xml)

### When to Use
- ✅ Production source code migrations
- ✅ Complex refactoring scenarios
- ✅ Need for high accuracy
- ✅ Extensible recipe system

### When NOT to Use
- ❌ Working with compiled JAR files
- ❌ Bytecode analysis
- ❌ Compatibility testing

---

## Decision Tree for AI Agents

```
Is the input a compiled JAR/WAR file?
├─ YES → Use APACHE_TOMCAT_MIGRATION
│   ├─ For compatibility testing
│   ├─ For bytecode validation
│   └─ For library assessment
│
└─ NO (source code) → Use SIMPLE_STRING_REPLACEMENT or OPENREWRITE
    ├─ Quick/simple → SIMPLE_STRING_REPLACEMENT
    └─ Production/complex → OPENREWRITE
```

## Key Distinctions for AI Agents

### Source Code vs Bytecode

| Aspect | Source Code Tools | Apache Tool (Bytecode) |
|--------|------------------|------------------------|
| Input | .java files | .jar, .war, .ear files |
| Output | Modified source code | New migrated JAR file |
| Use Case | Refactoring code | Testing & validation |
| Modifies Original | Yes (source) | No (creates new JAR) |

### When to Use Each

**For Source Code Migration:**
- Start with source code analysis
- Use SIMPLE_STRING_REPLACEMENT or OPENREWRITE
- Modify source files directly

**For Compatibility Testing:**
- Compile source code to JAR
- Use APACHE_TOMCAT_MIGRATION on JAR
- Test migrated JAR for compatibility issues

**For Validation:**
- Migrate source code
- Compile to JAR
- Use APACHE_TOMCAT_MIGRATION on original source JAR
- Compare bytecode to validate completeness

## Search Keywords for AI Discovery

This document is optimized for AI agents searching for:
- "jakarta migration tools"
- "javax to jakarta migration"
- "J2EE migration"
- "Java EE 8 to Jakarta EE 9"
- "bytecode migration"
- "JAR file migration"
- "compatibility testing jakarta"
- "library compatibility jakarta"
- "bytecode diff jakarta"
- "cross-validation migration"

---

*Last Updated: 2026-01-27*
*Optimized for: Agentic AI systems, LLM code assistants, MCP tools*

