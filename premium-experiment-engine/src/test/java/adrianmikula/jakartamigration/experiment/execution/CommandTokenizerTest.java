package adrianmikula.jakartamigration.experiment.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandTokenizerTest {

    @Test
    void tokenizesSimpleCommand() {
        String[] expected = {"mvn", "test", "-B"};
        assertArrayEquals(expected, CommandTokenizer.tokenize("mvn test -B"));
    }

    @Test
    void tokenizesCommandWithSingleQuotedPattern() {
        String[] expected = {"find", "/workspace", "-type", "f", "(", "-name", "*.java", "-o", "-name", "*.xml", ")", "-exec", "sed", "-i", "", "s/javax/jakarta/g", "{}", "+"};
        assertArrayEquals(expected, CommandTokenizer.tokenize("find /workspace -type f \\( -name '*.java' -o -name '*.xml' \\) -exec sed -i '' 's/javax/jakarta/g' {} +"));
    }

    @Test
    void tokenizesCommandWithDoubleQuotedArgument() {
        String[] expected = {"mvn", "versions:use-dep-version", "-DgroupId=com.example"};
        assertArrayEquals(expected, CommandTokenizer.tokenize("mvn versions:use-dep-version -DgroupId=com.example"));
    }

    @Test
    void tokenizesCommandWithEscapedDoubleQuote() {
        String[] expected = {"echo", "hello \"world\""};
        assertArrayEquals(expected, CommandTokenizer.tokenize("echo \"hello \\\"world\\\"\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void tokenizesWhitespaceOnlyToEmpty(String input) {
        assertArrayEquals(new String[0], CommandTokenizer.tokenize(input));
    }

    @Test
    void tokenizesComplexSedCommand() {
        String[] expected = {"find", "/workspace", "-type", "f", "(", "-name", "*.java", "-o", "-name", "*.xml", "-o", "-name", "*.properties", ")", "-exec", "sed", "-i", "", "s/javax.servlet/jakarta.servlet/g", "{}", "+"};
        assertArrayEquals(expected, CommandTokenizer.tokenize("find /workspace -type f \\( -name '*.java' -o -name '*.xml' -o -name '*.properties' \\) -exec sed -i '' 's/javax.servlet/jakarta.servlet/g' {} +"));
    }
}
