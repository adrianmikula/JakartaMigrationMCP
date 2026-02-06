# Jakarta Migration Tools - Quick Reference for AI Agents

## TL;DR for AI Agents

**Working with source code (.java files)?** → Use `SIMPLE_STRING_REPLACEMENT` or `OPENREWRITE`

**Working with compiled JAR/WAR files?** → Use `APACHE_TOMCAT_MIGRATION`

**Need to test compatibility?** → Use `APACHE_TOMCAT_MIGRATION` on compiled JAR

**Need to validate migration completeness?** → Use `APACHE_TOMCAT_MIGRATION` for bytecode diff

---

## Tool Quick Reference

### Apache Tomcat Migration Tool ⭐ (Most Important for Compatibility Testing)

**Works on**: Compiled JAR/WAR files (bytecode), NOT source code

**Key Use Cases**:
1. **Compatibility Testing**: Migrate JAR → Test if it runs → Identify incompatible libraries
2. **Bytecode Validation**: Compare migrated bytecode to validate source migration completeness
3. **Library Assessment**: Test if third-party JARs are Jakarta-compatible

**Example for AI**:
```
User: "Will my Spring Boot app work after Jakarta migration?"
→ Build JAR → Migrate with Apache tool → Test → Report compatibility issues
```

### Simple String Replacement

**Works on**: Java source code files (.java)

**Use Case**: Quick refactoring of source files from javax.* to jakarta.*

### OpenRewrite

**Works on**: Java source code files (.java, .xml)

**Use Case**: Production-grade source code refactoring with AST-based accuracy

---

## Common AI Agent Workflows

### Workflow 1: Pre-Migration Compatibility Check
```
1. Build application JAR
2. Use APACHE_TOMCAT_MIGRATION to migrate JAR
3. Attempt to run migrated JAR
4. Analyze errors → Identify incompatible libraries
5. Recommend Jakarta-compatible alternatives
```

### Workflow 2: Source Migration + Validation
```
1. Migrate source code (SIMPLE_STRING_REPLACEMENT or OPENREWRITE)
2. Compile migrated source → migrated.jar
3. Use APACHE_TOMCAT_MIGRATION on original → apache-migrated.jar
4. Compare bytecode → Validate nothing was missed
```

### Workflow 3: Library Compatibility Assessment
```
1. Download third-party library JAR
2. Use APACHE_TOMCAT_MIGRATION to migrate it
3. Test migrated JAR with Jakarta dependencies
4. Report compatibility status
```

---

## Key Distinction

| Tool Type | Input | Output | Primary Use |
|-----------|-------|--------|-------------|
| Source Code Tools | .java files | Modified source | Refactoring code |
| Apache Tool | .jar/.war files | New migrated JAR | Testing & validation |

---

*For detailed use cases, see: [docs/architecture/MIGRATION_TOOLS_USE_CASES.md](docs/architecture/MIGRATION_TOOLS_USE_CASES.md)*

