package adrianmikula.jakartamigration.experiment.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

class StreamGobbler extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(StreamGobbler.class);

    private final java.io.InputStream inputStream;
    private final StringBuilder output;

    StreamGobbler(java.io.InputStream inputStream, StringBuilder output) {
        this.inputStream = inputStream;
        this.output = output;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        } catch (Exception e) {
            LOG.debug("Stream gobbler read error (output may be incomplete)", e);
        }
    }
}
