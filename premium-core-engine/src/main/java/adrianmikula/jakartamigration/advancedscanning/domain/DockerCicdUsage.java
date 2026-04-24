package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a Java reference found in Docker or CI/CD configuration files.
 * Captures the type of reference, file context, and migration guidance.
 */
public record DockerCicdUsage(
    DockerCicdReferenceType referenceType,
    DockerCicdFileType fileType,
    String javaVersion,
    String command,
    String jakartaMigrationNote,
    int lineNumber
) {
    public DockerCicdUsage {
        if (referenceType == null) {
            throw new IllegalArgumentException("referenceType cannot be null");
        }
        if (fileType == null) {
            throw new IllegalArgumentException("fileType cannot be null");
        }
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command cannot be null or blank");
        }
        if (lineNumber <= 0) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }
    }

    /**
     * Types of Java references that can be found in Docker/CI-CD files.
     */
    public enum DockerCicdReferenceType {
        COMPILE("Build/Compile operations"),
        TEST("Testing operations"),
        STATIC_ANALYSIS("Static analysis and code quality"),
        PACKAGING("Packaging and bundling operations"),
        RUNTIME("Runtime execution");

        private final String description;

        DockerCicdReferenceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Types of files that can contain Java references.
     */
    public enum DockerCicdFileType {
        DOCKERFILE("Dockerfile"),
        DOCKER_COMPOSE("Docker Compose"),
        GITHUB_ACTIONS("GitHub Actions"),
        GITLAB_CI("GitLab CI"),
        JENKINSFILE("Jenkinsfile"),
        AZURE_PIPELINES("Azure Pipelines"),
        BITBUCKET_PIPELINES("Bitbucket Pipelines"),
        AWS_CODEBUILD("AWS CodeBuild"),
        GOOGLE_CLOUD_BUILD("Google Cloud Build"),
        KUBERNETES("Kubernetes Manifest"),
        SHELL_SCRIPT("Shell Script");

        private final String description;

        DockerCicdFileType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Returns true if this usage has Jakarta EE migration implications.
     */
    public boolean hasJakartaMigrationImplications() {
        return jakartaMigrationNote != null && !jakartaMigrationNote.isBlank();
    }

    /**
     * Returns true if this usage involves a specific Java version.
     */
    public boolean hasJavaVersion() {
        return javaVersion != null && !javaVersion.isBlank();
    }
}
