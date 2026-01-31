import mill._
import mill.scalalib._
import mill.javalib._
import coursier.MavenRepository

def scalaVersion = "2.13.12"

object jakartaMigrationMcp extends JavaModule {

  val version = "1.0.0"

  // Maven-style layout (src/main/java, src/test/java)
  override def sources = T.sources(millSourcePath / "src" / "main" / "java")
  override def resources = T.sources(millSourcePath / "src" / "main" / "resources")

  def mainClass = Some("adrianmikula.projectname.ProjectNameApplication")

  def javaVersion = "21"

  def javacOptions = Seq("-encoding", "UTF-8", "-parameters")

  def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(
      MavenRepository("https://repo.spring.io/milestone"),
      MavenRepository("https://repo.spring.io/snapshot"),
      MavenRepository("https://jitpack.io"),
    )
  }

  def ivyDeps = Agg(
    // Spring Boot 3.2
    ivy"org.springframework.boot:spring-boot-starter-web:3.2.0",
    ivy"org.springframework.boot:spring-boot-starter-validation:3.2.0",
    ivy"org.springframework.boot:spring-boot-starter-actuator:3.2.0",
    ivy"org.springframework.boot:spring-boot-starter-webflux:3.2.0",
    // Spring AI MCP
    ivy"org.springframework.ai:spring-ai-starter-mcp-server-webmvc:1.1.2",
    ivy"org.springframework.ai:spring-ai-mcp-annotations:1.1.2",
    ivy"org.springaicommunity:mcp-annotations:0.8.0",
    // Resilience4j
    ivy"io.github.resilience4j:resilience4j-spring-boot3:2.1.0",
    ivy"io.github.resilience4j:resilience4j-circuitbreaker:2.1.0",
    ivy"io.github.resilience4j:resilience4j-ratelimiter:2.1.0",
    // JGit
    ivy"org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r",
    // OpenRewrite
    ivy"org.openrewrite:rewrite-java:8.10.0",
    ivy"org.openrewrite:rewrite-maven:8.10.0",
    ivy"org.openrewrite.recipe:rewrite-migrate-java:2.5.0",
    ivy"org.openrewrite.recipe:rewrite-spring:5.10.0",
    // ASM
    ivy"org.ow2.asm:asm:9.6",
    ivy"org.ow2.asm:asm-commons:9.6",
    // YAML, japicmp
    ivy"org.yaml:snakeyaml:2.2",
    ivy"com.github.siom79.japicmp:japicmp:0.18.0",
  )

  def compileIvyDeps = Agg(ivy"org.projectlombok:lombok:1.18.30")

  object test extends JavaTests with TestModule.Junit5 {
    override def testSources = T.sources(millSourcePath / "src" / "test" / "java")

    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.springframework.boot:spring-boot-starter-test:3.2.0",
      ivy"org.testcontainers:junit-jupiter:1.19.3",
      ivy"org.testcontainers:testcontainers:1.19.3",
      ivy"io.projectreactor:reactor-test:3.6.6",
      ivy"com.squareup.okhttp3:mockwebserver:4.12.0",
      ivy"org.awaitility:awaitility:4.2.0",
      ivy"org.junit.platform:junit-platform-launcher:1.10.2",
    )
  }

  /** Runnable fat JAR (replaces Gradle bootJar). Named jakarta-migration-mcp-{version}.jar */
  override def assembly = T {
    val base = super.assembly()
    val name = s"jakarta-migration-mcp-${version}.jar"
    val dest = T.dest / name
    os.copy(base.path, dest, createFolders = true)
    PathRef(dest)
  }
}
