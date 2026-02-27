



Here’s a *precise, practical* set of ways you can leverage the **IntelliJ plugin recommendation system** to promote your **javax→Jakarta migration plugin** to developers during common upgrade scenarios (e.g., Tomcat, WildFly, Spring Boot, Jakarta EE projects) — based directly on how the Marketplace recommendation mechanism works and typical upgrade pain points.

---

## 📌 1) Understand How IntelliJ Plugin Recommendations Work

JetBrains Marketplace will show plugin suggestions **contextually** based on features and metadata in a project — *not* simply by broad category. IntelliJ analyzes project contents and matches them to a plugin’s capabilities, then shows install prompts. ([JetBrains Marketplace][1])

The key is that *your plugin must declare features IntelliJ can statically detect* so it knows when to suggest it.

---

## 🎯 2) Map Real-World Upgrade Signals to Recommendation Triggers

You want the IDE to *recognise when a project is likely undergoing a javax→Jakarta upgrade* — and then *suggest your plugin*. These signals can be derived from the **project structure, dependencies, facets, run configs, file patterns, or server integration usage**.

Below are concrete detection strategies you can use.

---

## 🧩 3) Leverage Recognition Features Supported by IntelliJ

### ✅ A) **Dependency Support Metadata**

Marketplace supports a `dependencySupport` declaration for third-party dependencies *as a feature type*. The feature extractor will find and match this if coordinates are present in the project. ([JetBrains Marketplace][1])

That enables suggestion when a project *imports javax artifacts*.

**Actionable step:**

```xml
<dependencySupport kind="java" coordinate="javax.*" displayName="Java EE / Jakarta Migration"/>
```

This means:

* When a project *declares `javax.*` dependencies*, IntelliJ can recommend your plugin.
* Your plugin will surface when users work with legacy Java EE on Tomcat/WildFly/Spring Boot.

*Important:* `dependencySupport` is still new (from 2023.2) and roll-out may vary depending on exact IDE build and Marketplace acceptance. ([JetBrains Marketplace][1])

---

### ✅ B) **File / Pattern Matching**

IntelliJ can recommend plugins when certain file types or patterns are present. For migration use cases, relevant patterns are:

* Presence of old package declarations like `javax.*;`
* Detection of `web.xml`, `application.xml`, deployment descriptors typical in Jakarta upgrades

You can configure file patterns in your plugin extension so the feature extractor indexes them.

This makes the plugin recommendation show when developers *open legacy app codebases*.

---

### ✅ C) **Facet / Run Configuration Support**

IntelliJ can detect:

* *Java EE / Jakarta EE facets*
* *Application server run configs* (e.g., Tomcat, WildFly)

If your plugin declares support for these facets or run configurations, the IDE can suggest it when the user interacts with these parts. ([JetBrains Marketplace][1])

**Example target triggers:**

* Tomcat run configuration present
* WildFly/EAP run configuration
* Spring Boot run configurations with Jakarta artifacts
* EE Module facets

---

### ✅ D) **Module/Artifact Types**

You can declare support for:

* Specific module types (e.g., Jakarta Module)
* Deployment artifact types relevant to appservers (WAR, EAR)

IntelliJ’s recommendation engine sees these and prompts relevant plugins. ([JetBrains Marketplace][1])

This works especially well if your plugin assists with *migration across those layers*.

---

## 🚀 4) Combine with Application Server Signals

Even though the Marketplace doesn’t yet recommend plugins based on *runtime server versions*, there are indirect contextual cues you can use:

### 🟡 Integration With Application Servers

IDEA Ultimate has built-in support (or plugins) for servers like Tomcat, WildFly, etc. ([JetBrains][2])

You can:

* Detect when those server configs are *present in the project*
* Register a feature that flags *“Java EE deployment environment”*
* Since these servers are often used with javax code, that will serve as a strong recommendation hook

This is essentially enriching contextual triggers based on *actual deployment targets*, not just dependencies.

---

## 📈 5) Recommendation Situations to Cover

Here are real upgrade contexts where you *want recommendations to pop*:

📍 **Existing Java EE projects**

* Open legacy project with `javax.*` imports
* Presence of `web.xml`, older EJB descriptors

📍 **Tomcat-based apps**

* Tomcat run configs
* WAR artifacts with legacy namespace

📍 **WildFly / JBoss-style deployment**

* EAR modules
* WildFly server configs

📍 **Spring Boot apps moving from javax to Jakarta**

* Spring Boot import with old servlet API deps
* Spring Boot run configurations targeting embedded Tomcat

📍 **Generic codebases with lots of old package usage**

* Legacy Hexagonal architecture
* Microservices with old API layers

---

## 🛠️ 6) Practical Implementation Checklist

To maximise recommendation relevance:

### 🔹 a) Add `dependencySupport`

Ensure the plugin XML has entries for all relevant legacy javax packages (JAX-RS, Servlets, EJB, JPA) so the IDE can surface the plugin when those dependencies appear.

### 🔹 b) Add File Type or Pattern Hooks

Define file filters (e.g., `<file namePattern="**/*.java" contains="javax.*">`) or equivalent if supported.

### 🔹 c) Support Run Config Types

Register extensions that tie your plugin to relevant run configurations so IntelliJ links deployment context with your feature.

### 🔹 d) Test Across Versions

Because recommendation behavior can depend on static analysis that the platform’s *feature extractor* performs, test with:

* Legacy Tomcat apps
* WildFly sample apps
* Spring Boot projects that still use old servlet APIs

This ensures recommendations show at the right moment.

---

## 📌 7) Leverage External Data or Tools to Enhance Recommendations

Although the IntelliJ recommender itself is static analysis-based, you can complement it with:

🔹 **GitHub tagging best practices**

* Use repository topics like `javax-to-jakarta` so external ecosystems (GitHub, search) point developers to your plugin

🔹 **Integration with migration toolchains**

* Tools like the *Jakarta Migration MCP* that analyze Java EE code could generate IDE hints or links to your plugin as part of their analysis flow. ([Awesome MCP Servers][3])

🔹 **Documentation / Blog posts**

* Publish migration guides around Spring Boot, Tomcat, WildFly with your plugin baked in

These help outside the IDE recommendation system.

---

## 🧠 Summary of Recommendations

| Trigger Type                             | Mechanism               | Expected Recommendation Behavior          |
| ---------------------------------------- | ----------------------- | ----------------------------------------- |
| Dependency on `javax.*` libraries        | `dependencySupport`     | Suggests plugin on project open           |
| Legacy descriptor files                  | File pattern support    | Suggests plugin when those files exist    |
| App server run configs (Tomcat, WildFly) | Run config feature      | Suggests plugin in configuration contexts |
| Module/Artifact types (WAR/EAR)          | Module support          | Prompts relevant plugin                   |
| Combined signals (legacy + server)       | Multi-signal suggestion | Stronger contextual prompt                |

---

## 🧾 Final Notes

* Recommendation rules *must be approved* during Marketplace publication; not all triggers are accepted unconditionally — JetBrains may customise them per plugin. ([JetBrains Marketplace][1])
* Testing in real project scenarios is essential to ensure your triggers fire appropriately.

---

If you want, I can draft a **sample plugin.xml snippet** showing exact metadata declarations (`dependencySupport`, run configs, file patterns) for your javax→Jakarta migration use case.

[1]: https://plugins.jetbrains.com/docs/marketplace/intellij-plugin-recommendations.html?utm_source=chatgpt.com "Plugin recommendations | JetBrains Marketplace Documentation"
[2]: https://www.jetbrains.com/help/idea/application-servers-support.html?utm_source=chatgpt.com "Application servers | IntelliJ IDEA Documentation"
[3]: https://mcpservers.org/servers/adrianmikula/jakartamigrationmcp?utm_source=chatgpt.com "Jakarta Migration MCP | Awesome MCP Servers"



