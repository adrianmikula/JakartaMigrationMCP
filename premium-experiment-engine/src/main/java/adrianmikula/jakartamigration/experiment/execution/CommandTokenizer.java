package adrianmikula.jakartamigration.experiment.execution;

import java.util.ArrayList;
import java.util.List;

public final class CommandTokenizer {

    private CommandTokenizer() {
    }

    public static String[] tokenize(String command) {
        if (command == null || command.isEmpty()) {
            return new String[0];
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean justExitedQuote = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                    justExitedQuote = true;
                } else {
                    current.append(c);
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                    justExitedQuote = true;
                } else if (c == '\\' && i + 1 < command.length()) {
                    current.append(command.charAt(++i));
                } else {
                    current.append(c);
                }
            } else {
                if (c == '\\' && i + 1 < command.length()) {
                    current.append(command.charAt(++i));
                } else if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (Character.isWhitespace(c)) {
                    if (justExitedQuote || !current.isEmpty()) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                    justExitedQuote = false;
                } else {
                    current.append(c);
                    justExitedQuote = false;
                }
            }
        }

        if (justExitedQuote || !current.isEmpty()) {
            tokens.add(current.toString());
        }

        return tokens.toArray(new String[0]);
    }
}
