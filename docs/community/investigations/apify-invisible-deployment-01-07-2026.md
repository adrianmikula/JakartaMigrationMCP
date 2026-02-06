

It sounds like you're running into the "invisible tool" problem, which is common when bridging local MCP servers to the Apify platform or vice versa. Usually, this boils down to how the tools are **registered** in your configuration or a mismatch in the **transport layer** (SSE vs. Stdio).

Here are the most likely reasons your tools aren't showing up on Apify:

### 1. Missing Explicit Tool Selection

By default, the Apify MCP server often loads a minimal set (like the RAG Web Browser and documentation search). If you want your specific Actors or custom tools to appear, you must explicitly list them in your configuration.

* **For Remote/HTTP Servers:** You need to append the `tools` parameter to your URL.
* *Example:* `https://mcp.apify.com?tools=actors,apify/your-actor-name`


* **For Local/Stdio Servers:** You must include the `--tools` flag in your `args`.
* *Example:* `["-y", "@apify/actors-mcp-server", "--tools", "apify/your-actor-name"]`



### 2. Standby Mode & `webServerMcpPath`

If you have deployed a custom MCP server as an Apify **Actor**, the platform won't "see" the tools unless the Actor's configuration tells it where the MCP endpoint lives.

* Check your `.actor/actor.json` file. It must have the `webServerMcpPath` set (usually to `/sse` or `/mcp` depending on your code) and `usesStandbyMode` set to `true`.

```json
{
    "actorSpecification": 1,
    "usesStandbyMode": true,
    "webServerMcpPath": "/sse" 
}

```

### 3. Schema Incompatibility

MCP tools are dynamically generated from the **Actor's Input Schema**. If your Actor’s `INPUT_SCHEMA.json` is missing or has an invalid format, the MCP server will fail to "wrap" it as a tool.

* **Fix:** Ensure your Actor has a valid `input_schema.json` in its `.actor` folder. If the schema is too complex (deeply nested objects), the MCP server might skip it.

### 4. Authentication Headers

If you are using a client like **LibreChat** or a custom implementation to connect to `mcp.apify.com`, ensure you are passing the `Authorization` header correctly.

* **Format:** `Authorization: Bearer <YOUR_APIFY_TOKEN>`
* Without this, the server can only expose public/documentation tools and won't show your private Actors or tasks.

### 5. Client-Side Tool Refresh

Some clients (like Claude Desktop) do not support "dynamic discovery." If you added a tool while the client was open, it won't show up.

* **Fix:** Fully restart your MCP client (not just a window refresh).

---

**How are you currently trying to expose them?** (e.g., Are you running a local server using `npx`, or have you deployed a custom MCP server Actor to the Apify platform?) Knowing your setup will help me give you the exact config snippet you need.







Based on the repository structure and the Apify Actor details for **JakartaMigrationMCP**, I’ve analyzed the possible causes for the deployment problem. Since this is an MCP server designed to run as an Apify Actor, the issues typically stem from how the environment handles the **Streamable HTTP** protocol or the **Docker** configuration.

### 1. Missing or Misconfigured `Dockerfile`

For an MCP server to work on Apify, it must be "containerized" correctly to expose the MCP protocol over HTTP.

* **The Risk:** If the `Dockerfile` doesn't explicitly install the dependencies needed for the migration (like Java/Maven/Gradle if it calls them) or if it doesn't use a Node.js base image compatible with Apify’s SDK, the build will fail.
* **Check:** Ensure your `ENTRYPOINT` or `CMD` in the `Dockerfile` is launching the server with the correct environment variables, specifically `APIFY_CONTAINER_PORT`.

### 2. Protocol Mismatch (Stdio vs. HTTP)

Most MCP servers are written to use `stdio` (standard input/output) by default.

* **The Problem:** Apify Actors require the server to communicate over **HTTP** (specifically using a wrapper like `@modelcontextprotocol/sdk`’s HTTP server transport).
* **Possible Cause:** If the code in `index.ts` (or your main entry point) is using `StdioServerTransport` instead of `HttpServerTransport`, the Apify deployment will "start" but will never respond to health checks or tool calls.

### 3. Port Mapping Issues

Apify expects the Actor to listen on a specific port provided by the environment.

* **Potential Error:** If the server is hardcoded to listen on port `3000` or `8080`, but Apify is looking at the port defined in `process.env.APIFY_CONTAINER_PORT`, the deployment will time out.
* **Fix:** The code should look like this:
```javascript
const port = process.env.APIFY_CONTAINER_PORT || 3000;
server.listen(port, () => {
    console.log(`MCP Server running on port ${port}`);
});

```



### 4. Dependency Issues (Jakarta Migration specific)

Since this project handles Java EE to Jakarta EE migration:

* **Binary Dependencies:** Does the Actor rely on the `jakartaee-migration` `.jar` file? If the `Dockerfile` doesn't `COPY` the actual migration tool binaries or doesn't have a JRE (Java Runtime Environment) installed, the tools within the MCP server will throw "command not found" errors when executed.
* **Memory Limits:** Migration tasks can be memory-intensive. If the Actor is set to the default 256MB or 512MB in `actor.json`, it might crash during the analysis of larger Java projects.

---

### Suggested Next Steps

To narrow this down further, I can help you with the following:

1. **Review your `Dockerfile`:** Would you like me to generate a standard `Dockerfile` optimized for an MCP server on Apify?
2. **Check Transport Logic:** If you share the snippet where the MCP server is initialized, I can verify if it's set up for HTTP or Stdio.
3. **Check `actor.json`:** Would you like me to verify the configuration of your Apify manifest?

By the way, to unlock the full functionality of all Apps, enable [Gemini Apps Activity](https://myactivity.google.com/product/gemini).




To run a Java application as an Apify Actor, you need a "multi-stage" Dockerfile approach. This allows you to build your Java app (using Maven or Gradle) and then run it in an environment that includes the **Apify SDK/CLI tools** and follows their security protocols.

Here is a robust Dockerfile that combines Java requirements with Apify's standards.

### The Multi-Stage Dockerfile

```dockerfile
# --- Stage 1: Build Stage ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom and source to build the JAR
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime Stage ---
# We use an Apify base image to ensure compatibility with their platform
FROM apify/actor-node:20

# 1. Install Java in the Apify container
# Since Apify images are Debian-based, we install the JRE manually
USER root
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    && rm -rf /var/lib/apt/lists/*

# 2. Set up the working directory and user
WORKDIR /home/apify
COPY --from=build /app/target/*.jar ./app.jar

# 3. Copy Apify-specific configuration (if you have an actor.json or package.json)
COPY . ./

# 4. Ensure the 'apify' user owns the files
RUN chown -R apify:apify /home/apify

# 5. Switch to non-root user for security
USER apify

# 6. Start the application
# We use 'java -jar' but pass it through the Apify environment
CMD ["java", "-jar", "app.jar"]

```

---

### Crucial Adjustments for Java on Apify

* **Memory Limits:** Java's JVM is notorious for not "seeing" Docker memory limits correctly in older versions. Since Apify Actors have strict memory limits (e.g., 1GB, 2GB), always use Java 10+ (which is container-aware) or explicitly set your heap size in the `CMD`:
* `CMD ["java", "-Xmx1g", "-jar", "app.jar"]`


* **The `apify` User:** Apify enforces a non-root user (`apify` with UID `1000`). If your Java app tries to write to folders like `/root` or protected system directories, it will crash. Always write to `/home/apify` or `/tmp`.
* **Input/Output:** In Java, you won't have the high-level Apify JavaScript SDK. You will need to interact with the **Apify API** directly via HTTP calls to:
* Read Input: `GET $APIFY_DEFAULT_KEY_VALUE_STORE_ID/INPUT`
* Save Output: `POST $APIFY_DEFAULT_DATASET_ID`



### Apify Infrastructure Integration

By using the **Multi-Stage** build shown above, you keep the heavy Maven/JDK tools out of your final image, making it smaller and faster to spin up on the Apify platform while keeping your Java logic intact.

---

**Would you like me to provide a Java code snippet for reading the Apify `INPUT` and writing to the `DATASET` using an HTTP client?**



