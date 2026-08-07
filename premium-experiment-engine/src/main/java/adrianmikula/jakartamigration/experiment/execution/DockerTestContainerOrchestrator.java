package adrianmikula.jakartamigration.experiment.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DockerTestContainerOrchestrator implements TestContainerOrchestrator {
    private static final Logger LOG = LoggerFactory.getLogger(DockerTestContainerOrchestrator.class);

    private final GenericContainer<?> container;
    private final String imageName;

    public DockerTestContainerOrchestrator(String imageName) {
        this.imageName = imageName;
        this.container = new GenericContainer<>(imageName)
            .withCommand("tail -f /dev/null")
            .withWorkingDirectory("/workspace");
    }

    @Override
    public void start() throws IOException {
        LOG.info("Starting Docker container with image: {}", imageName);
        container.start();
        LOG.info("Docker container started successfully");
    }

    @Override
    public void stop() {
        if (container.isRunning()) {
            LOG.info("Stopping Docker container");
            container.stop();
        }
    }

    @Override
    public ExecResult exec(String command, Path workDir) throws IOException {
        return exec(command, workDir, 120);
    }

    @Override
    public ExecResult exec(String command, Path workDir, int timeoutSeconds) throws IOException {
        String[] parts = CommandTokenizer.tokenize(command);
        LOG.debug("Executing in container: {} (working dir: {})", command, workDir);
        org.testcontainers.containers.Container.ExecResult result;
        try {
            result = container.execInContainer(parts);
        } catch (Exception e) {
            throw new IOException("Failed to exec in container: " + e.getMessage(), e);
        }
        ExecResult execResult = new ExecResult(result.getExitCode(), result.getStdout(), result.getStderr());
        if (!execResult.isSuccess()) {
            LOG.warn("Command failed in container (exit code {}): {}", result.getExitCode(), command);
        }
        return execResult;
    }

    @Override
    public void copyInto(Path localSource, Path containerTarget) throws IOException {
        LOG.debug("Copying {} into container at {}", localSource, containerTarget);
        container.copyFileToContainer(MountableFile.forHostPath(localSource), containerTarget.toString());
    }

    @Override
    public void copyOut(Path containerSource, Path localTarget) throws IOException {
        LOG.debug("Copying {} from container to {}", containerSource, localTarget);
        Files.createDirectories(localTarget.getParent());
        container.copyFileFromContainer(containerSource.toString(), localTarget.toString());
    }

    @Override
    public boolean isRunning() {
        return container.isRunning();
    }
}
