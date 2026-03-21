





To access an exhaustive and programmatically accessible list of OpenRewrite recipes and their associated Jakarta artifacts via a REST API, you have two primary paths: the **Moderne API** (the commercial creators of OpenRewrite who provide a public GraphQL/REST interface) or the **GitHub Content API**.

Given your requirement for "exhaustive" and "daily updates," here is how to integrate these into your app.

---

### 1. The OpenRewrite/Moderne API (Recommended)
The most structured way to get recipe metadata, including every Jakarta migration path, is through the **Moderne Public API**. They provide a schema that describes every recipe, its version, and its dependencies.

* **Endpoint:** `https://api.moderne.io/query`
* **Format:** GraphQL (which acts as a flexible REST endpoint).
* **What it gives you:** Real-time access to the entire recipe catalog, including those for Jakarta EE 8, 9, 10, and 11.

**Example Query for Jakarta Recipes:**
```graphql
query GetJakartaRecipes {
  recipes(filter: "jakarta") {
    name
    displayName
    description
    recipeList {
      name
    }
  }
}
```

### 2. GitHub REST API (For Raw Recipe Definitions)
If you want to parse the actual migration logic (e.g., "If I see `javax.servlet`, change to `jakarta.servlet`"), you should pull the raw YAML files directly from the OpenRewrite source repositories.

* **Endpoint:** `https://api.github.com/repos/openrewrite/rewrite-migrate-java/contents/src/main/resources/META-INF/rewrite`
* **Method:** `GET`
* **Why use this:** This is the "source of truth." When a developer adds a new Jakarta artifact mapping, it appears here first. You can fetch `jakarta-ee-9.yml` and parse the `ChangePackage` and `ChangeDependency` blocks to populate your app's database.

### 3. Monitoring New Artifacts (The Maven Central API)
To fulfill your requirement of locating new Jakarta artifacts daily, you should query the **Sonatype Search API**. This allows your app to "discover" when a new `jakarta.*` group ID is published.

* **Base URL:** `https://search.maven.org/solrsearch/select`
* **Query Parameters:** * `q`: `g:jakarta.*`
    * `rows`: `50`
    * `sort`: `timestamp desc`
* **App Logic:** Run a cron job every 24 hours to hit this endpoint, compare the results against your local database, and flag any new artifacts.

---

### Integration Architecture



| Data Point | Source API | Update Frequency |
| :--- | :--- | :--- |
| **Recipe Metadata** | Moderne GraphQL API | Weekly (Stable) |
| **Mapping Logic** | GitHub REST API | Daily (As PRs merge) |
| **New Jakarta JARs** | Maven Central API | Real-time / Daily |

---

### Implementation Tip: Parsing the YAML
Since OpenRewrite recipes are primarily YAML-based, your app will need a robust YAML parser (like **SnakeYAML** for Java or **js-yaml** for Node.js). 

The recipes follow a predictable structure:
```yaml
type: specs.openrewrite.org/v1beta/recipe
name: org.openrewrite.java.migrate.jakarta.ServletToJakarta
recipeList:
  - org.openrewrite.java.ChangePackage:
      oldPackageName: javax.servlet
      newPackageName: jakarta.servlet
```
Your API-driven app can scrape these `oldPackageName` and `newPackageName` values to create a comprehensive "Migration Map" for your users.

**Would you like me to write a sample script in Python or Node.js that fetches and parses the latest Jakarta migration YAML from the GitHub API?**