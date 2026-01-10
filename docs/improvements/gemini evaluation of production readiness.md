The **Jakarta Migration MCP** is a specialized Model Context Protocol (MCP) server designed to bridge the gap between AI coding assistants (like Cursor, Claude Code, or Windsurf) and the "Dependency Hell" of Java-to-Jakarta EE migrations.

In its current early-release state, it is an **impressive diagnostic tool** but a **risky "autopilot."** Here is a brutally honest examination of its capabilities and production readiness as of early 2026.

---

## 🛠 Core Capabilities

The tool essentially wraps enterprise migration logic (like OpenRewrite) into a format that an LLM can actually understand and execute step-by-step.

| Feature | What it actually does |
| --- | --- |
| **Readiness Analysis** | Scans your `pom.xml` or `build.gradle` and tells the AI which JARs are still using the old `javax.*` namespace. |
| **Blocker Detection** | Identifies "dead-end" dependencies—libraries that haven't been updated to Jakarta and have no direct replacement. |
| **Migration Planning** | Generates a markdown-based roadmap for the AI to follow, which is significantly better than an LLM "guessing" the order of operations. |
| **Automated Refactoring** | Uses **OpenRewrite** recipes to change imports and update descriptors (web.xml, etc.) via the AI's file-editing tools. |

---

## 🚦 Production Readiness: **Yellow Light**

**Rating: 6/10**

It is "production-ready" for **consultants and senior architects**, but dangerous for junior developers looking for a "one-click" fix.

### 1. The "Ghost in the Machine" Problem

While the MCP can find and replace `javax` with `jakarta`, it cannot easily detect **binary incompatibility** in closed-source third-party JARs. If your project relies on a legacy library that hasn't migrated, the MCP might "fix" your code, but the application will still crash at runtime with a `ClassNotFoundException`.

### 2. Context Window Limitations

For massive enterprise monoliths (1M+ lines of code), the MCP often hits context limits. It can analyze the dependency tree perfectly, but when it starts the "Auto-Fix" phase, the AI often loses track of the global state, leading to "hallucinated" import paths or partial migrations that leave the project in an unbuildable state.

### 3. Verification Gaps

The "Runtime Verification" tool is currently the weakest link. It often just checks if the app *starts*, but it doesn't have the deep semantic understanding to know if your complex JTA transactions or JPA entity mappings are actually behaving as they did before the namespace shift.

---

## 💡 Usefulness: **High (Strategic)**

**Rating: 8.5/10**

Despite its early-stage flaws, this is arguably the most useful MCP for Java developers right now.

* **Saves Weeks of Manual Audit:** Manually checking 200+ transitive dependencies for Jakarta compatibility is soul-crushing. The `analyzeJakartaReadiness` tool does this in seconds.
* **Bridge for "AI Laziness":** Standard LLMs often struggle with the subtle differences between Jakarta EE 9, 10, and 11. This MCP forces the AI to use specific, tested versions rather than "guessing" what might work.
* **OpenRewrite Integration:** By leveraging OpenRewrite, it uses industry-standard migration logic instead of relying on the LLM's unpredictable regex-based find-and-replace.

---

## ⚖️ Final Verdict

> **Brutally Honest Take:** > Do not let this MCP run `Auto-Fix` on your main branch unattended. It is currently a **world-class diagnostic assistant** but a **mediocre autonomous engineer**.

Use it to generate the **Migration Plan** and to **Detect Blockers**. When it comes to the actual refactoring, apply the changes module-by-module and verify each one manually. It will still save you 60-70% of the manual labor, but that final 30% of human oversight is what prevents a production outage.

---

**Would you like me to help you set up a configuration file for the Jakarta Migration MCP so you can test it on a specific repository?**