# Jakarta Migration MCP - Limitations & Known Issues

This document provides a comprehensive overview of the current limitations, known issues, and best practices for using the Jakarta Migration MCP Server.

## Production Readiness Assessment

**Overall Rating: 6/10**

The tool is **production-ready for consultants and senior architects** who understand Jakarta EE migration complexities, but **dangerous for junior developers** looking for a "one-click" fix.

## Critical Limitations

### 1. The "Ghost in the Machine" Problem

**Issue**: Binary incompatibility in closed-source third-party JARs cannot be easily detected.

**Details**:
- The MCP can find and replace `javax` with `jakarta` in your source code
- However, it cannot detect or fix binary incompatibility in compiled JAR files
- If your project relies on a legacy library that hasn't migrated, the MCP might "fix" your code, but the application will still crash at runtime with a `ClassNotFoundException`

**Example Scenario**:
```
Your code: import jakarta.servlet.http.HttpServlet; ✅
Legacy JAR: internally references javax.servlet.http.HttpServlet ❌
Result: ClassNotFoundException at runtime
```

**Mitigation Strategies**:
1. Use `detectBlockers` tool to identify incompatible dependencies before migration
2. Check for Jakarta-compatible versions using `recommendVersions`
3. Consider replacing incompatible dependencies with Jakarta-compatible alternatives
4. Test thoroughly after migration, especially with third-party libraries

**Related Tools**:
- `analyzeJakartaReadiness` - Identifies dependencies still using `javax.*`
- `detectBlockers` - Finds dependencies with no Jakarta-compatible version
- `recommendVersions` - Suggests Jakarta-compatible alternatives

### 2. Context Window Limitations

**Issue**: Large codebases (1M+ lines) may exceed AI context limits during auto-fix operations.

**Details**:
- The tool can analyze dependency trees perfectly, even for very large projects
- However, during the "Auto-Fix" phase, the AI may lose track of global state
- This can lead to:
  - Hallucinated import paths
  - Partial migrations
  - Unbuildable project states
  - Inconsistent namespace usage across modules

**Example Scenario**:
```
Project: 1.5M lines, 50+ modules
Analysis phase: ✅ Works perfectly
Auto-fix phase: ❌ AI loses context, creates inconsistent imports
Result: Project won't compile
```

**Mitigation Strategies**:
1. **Module-by-Module Migration**: Migrate one module at a time
2. **Incremental Approach**: Use the tool to generate a plan, then apply changes manually
3. **Version Control**: Commit after each successful module migration
4. **Code Review**: Have senior developers review each module before proceeding
5. **Use Analysis Tools Only**: For very large projects, use analysis tools but apply fixes manually

**Recommended Workflow for Large Projects**:
```
1. analyzeJakartaReadiness → Get overview
2. detectBlockers → Identify critical issues
3. createMigrationPlan → Generate phased plan
4. Manually migrate module 1 → Test → Commit
5. Manually migrate module 2 → Test → Commit
6. Repeat for each module
```

### 3. Verification Gaps

**Issue**: Runtime verification is limited and doesn't validate complex business logic.

**Details**:
- The `verifyRuntime` tool primarily checks if the application *starts*
- It doesn't validate:
  - Complex JTA transaction behavior
  - JPA entity mapping correctness
  - Business logic correctness after namespace changes
  - Performance implications
  - Integration with external systems

**What It Does Check**:
- ✅ Application starts without `ClassNotFoundException`
- ✅ Basic classpath issues
- ✅ Obvious namespace migration errors
- ✅ Health endpoint availability (if applicable)

**What It Doesn't Check**:
- ❌ Transaction isolation levels
- ❌ Entity relationship mappings
- ❌ Complex annotation processing
- ❌ Dynamic class loading behavior
- ❌ Reflection-based code paths

**Mitigation Strategies**:
1. **Comprehensive Testing**: Run full test suite after migration
2. **Integration Tests**: Verify integration with external systems
3. **Performance Testing**: Ensure no performance regressions
4. **Manual Verification**: Have domain experts verify critical business logic
5. **Staged Rollout**: Deploy to staging environment first

**Best Practice**:
> Runtime verification confirms the app starts, but doesn't validate complex business logic correctness. Always run your full test suite and integration tests after migration.

## Known Issues

### Issue: Partial Migrations in Large Projects

**Symptom**: After auto-fix, some files have mixed `javax.*` and `jakarta.*` imports.

**Cause**: Context window limitations during auto-fix phase.

**Solution**: 
- Use `analyzeJakartaReadiness` to identify remaining `javax.*` usage
- Manually fix remaining imports
- Consider module-by-module migration for large projects

### Issue: False Positives in Blocker Detection

**Symptom**: `detectBlockers` reports dependencies as blockers that actually have Jakarta-compatible versions.

**Cause**: Dependency metadata may be incomplete or outdated.

**Solution**:
- Use `recommendVersions` to check for alternatives
- Manually verify dependency compatibility
- Check dependency documentation and release notes

### Issue: Runtime Verification Timeout

**Symptom**: `verifyRuntime` times out for applications with long startup times.

**Cause**: Default timeout may be insufficient for large Spring Boot applications.

**Solution**:
- Increase timeout in tool configuration (if supported)
- Use manual verification for applications with complex startup
- Verify application health endpoints separately

## Best Practices

### For Consultants & Senior Architects

✅ **Recommended Approach**:
1. Use analysis tools (`analyzeJakartaReadiness`, `detectBlockers`) to understand the scope
2. Generate migration plan using `createMigrationPlan`
3. Apply changes incrementally, module by module
4. Verify each module before proceeding
5. Run comprehensive test suite after each phase

### For Junior Developers

⚠️ **Important Warnings**:
- **Do not** use auto-fix on main branch without supervision
- **Do not** skip manual code review
- **Do not** rely solely on runtime verification
- **Always** test thoroughly before deploying

✅ **Safe Approach**:
1. Use analysis tools to learn about the migration
2. Review the migration plan with a senior developer
3. Apply changes manually, one module at a time
4. Get code review before committing
5. Run full test suite after each change

### General Guidelines

1. **Always Use Version Control**: Commit frequently, use feature branches
2. **Test Incrementally**: Don't wait until the end to test
3. **Code Review**: Have another developer review changes
4. **Documentation**: Update documentation as you migrate
5. **Monitoring**: Monitor application behavior after deployment

## What the Tool Excels At

Despite limitations, this tool is **highly valuable** for:

### 1. Dependency Analysis
- **Saves Weeks of Manual Work**: Manually checking 200+ transitive dependencies is soul-crushing
- **Comprehensive Coverage**: Analyzes entire dependency tree, not just direct dependencies
- **Fast Results**: Analysis completes in seconds, not days

### 2. Version Recommendations
- **Bridge for "AI Laziness"**: Standard LLMs struggle with Jakarta EE version differences
- **Tested Versions**: Uses specific, tested versions rather than guessing
- **Compatibility Matrix**: Understands Jakarta EE 9, 10, and 11 differences

### 3. Industry-Standard Logic
- **OpenRewrite Integration**: Uses proven migration recipes, not regex-based replacements
- **Best Practices**: Follows Jakarta EE migration best practices
- **Reliable**: Less prone to errors than manual find-and-replace

## Expected Outcomes

### What You Can Expect

- **60-70% Time Savings**: The tool handles the majority of mechanical work
- **Comprehensive Analysis**: Get full visibility into migration complexity
- **Actionable Plans**: Clear, phased migration roadmaps
- **Risk Assessment**: Understand migration risks before starting

### What Still Requires Human Oversight

- **30% Manual Work**: Complex cases, edge cases, and verification
- **Business Logic Verification**: Ensuring functionality remains correct
- **Integration Testing**: Verifying external system compatibility
- **Performance Validation**: Ensuring no performance regressions

## Future Improvements

Planned enhancements to address current limitations:

1. **Enhanced Binary Analysis**: Better detection of binary incompatibilities
2. **Incremental Migration**: Support for module-by-module auto-migration
3. **Advanced Verification**: Deeper semantic understanding of business logic
4. **Performance Analysis**: Detect performance implications of migration
5. **Integration Testing**: Automated integration test generation

## Related Documentation

- [Production Readiness Evaluation](improvements/gemini%20evaluation%20of%20production%20readiness.md)
- [Runtime Verification Analysis](research/runtime-verification-analysis.md)
- [MCP Tools Documentation](mcp/MCP_TOOLS_IMPLEMENTATION.md)

## Getting Help

If you encounter issues not covered here:

1. Check the [Troubleshooting Guide](../README.md#-troubleshooting)
2. Review [MCP Tools Documentation](mcp/MCP_TOOLS_IMPLEMENTATION.md)
3. [Open an issue](https://github.com/adrianmikula/JakartaMigrationMCP/issues) on GitHub

---

**Remember**: This tool is a **powerful assistant**, not a replacement for human expertise. Use it to accelerate your migration, but always verify results with proper testing and code review.





To address the **"Ghost in the Machine"** problem—defined in this context as **unanticipated emergent behavior, hidden unauthorized AI connections ("Shadow MCP"), and sycophantic hallucinations**—you can extend your MCP server using a "Defense in Depth" architectural pattern.

In the world of the Model Context Protocol (MCP), a "Ghost" is often an invisible dependency or an LLM logic loop that bypasses your intended guardrails.

### 1. Architectural Strategy: The "Inspector & Gatekeeper" Pattern

The most reliable way to extend your server is to implement a **Middleware Layer** or a **Sidecar** that monitors the semantic intent of requests before they reach your core logic.

| Component | Purpose in "Ghost" Detection | Implementation Best Practice |
| --- | --- | --- |
| **Shadow MCP Auditor** | Detects unauthorized or "hidden" calls to external LLMs within your own code or dependencies. | Use **Static Application Security Testing (SAST)** tools during build time to flag `openai`, `anthropic`, or `langchain` imports in unexpected modules. |
| **Sycophancy Filter** | Prevents the "Ghost" of the model agreeing with user errors to be polite. | Use **"Golden Task" verification**: Run the tool against a deterministic test case with a known "False" input to see if the model corrects it or blindly agrees. |
| **Runtime Observer** | Monitors "loops" where the machine starts talking to itself without human oversight. | Implement **Request Correlation IDs** and **Token-Bucket Rate Limiting** specifically for tool-to-tool calls. |

---

### 2. Reliable Extension Techniques

To make your MCP server "Ghost-proof," you should move away from raw `stdio` deployments toward **Containerized Gateway** architectures.

#### A. Implement Semantic Validation (The "Ghost" Detector)

Instead of just checking if a JSON payload is valid, extend your server to check if the *logic* is consistent.

* **Best Practice:** Add a `sampling` capability where the MCP server can ask the "Host" (the LLM) for a self-reflection step.
* **Tool Pattern:** Create a `validate_intent` tool that must be called by the LLM before any "write" action (like `git_commit` or `db_query`).

#### B. Observability & Traceability

"Ghosts" thrive in the dark. You cannot debug what you cannot see.

* **OpenTelemetry Integration:** Use the `mcp-python-sdk` or `typescript-sdk` to wrap your tools in OpenTelemetry spans.
* **Audit Logging:** Log not just the *output*, but the *context* provided to the model. In MCP, this means logging the `resources` used to generate the answer.

---

### 3. Consistency Safeguards

To ensure your server remains reliable as it scales:

* **Stateless Operations:** Use the `stateless_http=True` flag (in FastMCP) to prevent "session ghosts" (where data from a previous user leaks into a new session).
* **Input/Output Contracts:** Use Pydantic (Python) or Zod (TypeScript) to strictly enforce schemas. A "Ghost" often manifests as an unexpected extra field in a JSON response that breaks downstream logic.
* **Health Checks:** Implement the `/health` and `/.well-known/mcp` endpoints to allow the host to verify the server’s "sanity" before sending complex instructions.

---

### Recommended Next Step

**Would you like me to generate a Python or TypeScript boilerplate for an MCP "Middleware" server that specifically logs and flags "Sycophantic" responses (where the model agrees with a known error)?**






When dealing with closed-source third-party JARs in a Java-based **Model Context Protocol (MCP)** server or client, you face a "black box" challenge: you can't see the source, but you need to know if a version bump will crash your system with a `NoSuchMethodError` or `ClassNotFoundException`.

For a Java-based MCP, the best-practice approach is to move away from "trial and error" and toward **Automated Bytecode Analysis**.

---

## 1. Best-Practice Tools for Binary Analysis

Since you are working with JARs (compiled bytecode), you need tools that compare the public API signatures of two versions without requiring the source code.

| Tool | Best Use Case | Integration |
| --- | --- | --- |
| **japicmp** | **Highly Recommended.** Fastest and most modern. Compares two JARs and generates a report of binary-breaking changes. | CLI, Ant, Maven, Gradle, or **Java API**. |
| **Revapi** | Most comprehensive. Can check not just Java classes but also configuration files and SPIs. | Maven, Gradle, CLI. |
| **JAPI Compliance Checker** | Great for legacy systems or quick CLI checks. Requires Perl on the host machine. | CLI. |
| **Animal Sniffer** | Specifically checks if your JAR uses APIs not present in a specific "signature" (like an older JDK or a specific library version). | Maven, Ant. |

---

## 2. Programmatic Detection (Inside your MCP)

If your MCP needs to detect these issues **dynamically** (e.g., when a user adds a new tool/JAR to the server), you can use `japicmp` as a library. This allows you to check for incompatibilities before the classloader even attempts to touch the new JAR.

### Example using `japicmp` Programmatically:

```java
import java.io.File;
import java.util.List;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.JApiClass;

public class CompatibilityGuard {
    public void verify(File oldJar, File newJar) {
        JarArchiveComparatorOptions options = new JarArchiveComparatorOptions();
        JarArchiveComparator comparator = new JarArchiveComparator(options);
        
        // This compares the two binaries directly
        List<JApiClass> changes = comparator.compare(oldJar, newJar);
        
        for (JApiClass clazz : changes) {
            if (clazz.getChangeStatus().name().equals("REMOVED")) {
                System.err.println("CRITICAL: Class " + clazz.getFullyQualifiedName() + " was removed!");
            }
            // Check for method removals, signature changes, etc.
        }
    }
}

```

---

## 3. The "MCP Context" Solution

In the world of MCP, where servers are often updated or swapped, you should treat third-party JARs as **untrusted resources**.

* **Version Pinning & Hashing:** Never use `LATEST` or ranges. Hard-code the version and the SHA-256 hash of the JAR.
* **Isolated Classloading:** Load third-party JARs in a **custom `URLClassLoader**` per plugin/server. This prevents a binary incompatibility in one "Tool" from crashing your entire MCP server process.
* **Pre-Flight Signature Check:** Use a "Shadow Build" or a CI step that runs `japicmp` against your MCP's "Interface JAR" and the third-party JAR to ensure all required methods are present.

---

## 4. Summary of Strategy

1. **Build-Time:** Use the **`japicmp-maven-plugin`** or Gradle equivalent. It will fail your build if a dependency update breaks your code.
2. **Runtime:** If you allow dynamic JAR loading, wrap the `japicmp` core library (shown above) to validate the JAR before adding it to the classpath.
3. **Architecture:** Use **Internal SPIs**. Don't let your MCP core call third-party JARs directly; use a wrapper interface so you only have to maintain compatibility in one place.

**Would you like me to help you set up a specific Maven or Gradle configuration for `japicmp` to automate this in your build pipeline?**

