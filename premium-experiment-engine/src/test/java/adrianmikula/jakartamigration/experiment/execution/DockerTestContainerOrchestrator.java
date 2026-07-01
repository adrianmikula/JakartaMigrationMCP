package adrianmikula.jakartamigration.experiment.execution;

import adrianmikula.jakartamigration.experiment.execution.ExecResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Testcontainers
public class DockerTestContainerOrchestrator implements TestContainerOrchestrator {
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
        container.start();
    }

    @Override
    public void stop() {
        if (container.isRunning()) {
            container.stop();
        }
    }

    @Override
    public ExecResult exec(String command, Path workDir) throws IOException {
        return exec(command, workDir, 120);
    }

    @Override
    public ExecResult exec(String command, Path workDir, int timeoutSeconds) throws IOException {
        String[] parts = command.split(" ");
        org.testcontainers.containers.Container.ExecResult result;
        try {
            result = container.execInContainer(parts);
        } catch (Exception e) {
            throw new IOException("Failed to exec in container: " + e.getMessage(), e);
        }
        return new ExecResult(result.getExitCode(), result.getStdout(), result.getStderr());
    }

    @Override
    public void copyInto(Path localSource, Path containerTarget) throws IOException {
        if (Files.isDirectory(localSource)) {
            container.copyFileToContainer(MountableFile.forHostPath(localSource), containerTarget.toString());
        } else {
            container.copyFileToContainer(MountableFile.forHostPath(localSource), containerTarget.toString());
        }
    }

    @Override
    public void copyOut(Path containerSource, Path localTarget) throws IOException {
        Files.createDirectories(localTarget.getParent());
        container.copyFileFromContainer(containerSource.toString(), localTarget.toString());
    }

    @Override
    public boolean isRunning() {
        return container.isRunning();
    }
}
