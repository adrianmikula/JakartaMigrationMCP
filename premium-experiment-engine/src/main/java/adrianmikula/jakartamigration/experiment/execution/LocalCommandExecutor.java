package adrianmikula.jakartamigration.experiment.execution;

import adrianmikula.jakartamigration.experiment.execution.ExecResult;

import java.io.IOException;
import java.nio.file.Path;

public class LocalCommandExecutor implements CommandExecutor {
    private final ProcessBuilderFactory processBuilderFactory;

    public LocalCommandExecutor() {
        this((command, workingDir) -> new ProcessBuilder(command).directory(workingDir.toFile()));
    }

    public LocalCommandExecutor(ProcessBuilderFactory processBuilderFactory) {
        this.processBuilderFactory = processBuilderFactory;
    }

    @Override
    public ExecResult exec(String command, Path workingDir) throws IOException {
        return exec(command, workingDir, 120);
    }

    @Override
    public ExecResult exec(String command, Path workingDir, int timeoutSeconds) throws IOException {
        String[] parts = CommandTokenizer.tokenize(command);
        ProcessBuilder pb = processBuilderFactory.create(parts, workingDir);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        StreamGobbler outGobbler = new StreamGobbler(process.getInputStream(), stdout);
        StreamGobbler errGobbler = new StreamGobbler(process.getErrorStream(), stderr);
        outGobbler.start();
        errGobbler.start();

        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroy();
            throw new IOException("Command interrupted", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out after " + timeoutSeconds + "s: " + command);
        }

        try {
            outGobbler.join();
            errGobbler.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return new ExecResult(process.exitValue(), stdout.toString(), stderr.toString());
    }

    @FunctionalInterface
    interface ProcessBuilderFactory {
        ProcessBuilder create(String[] command, Path workingDir);
    }
}
