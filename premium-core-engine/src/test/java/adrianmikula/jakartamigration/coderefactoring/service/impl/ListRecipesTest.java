package adrianmikula.jakartamigration.coderefactoring.service.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import java.util.stream.Collectors;

@Tag("slow")
public class ListRecipesTest {
    @Test
    public void listAllRecipes() throws java.io.IOException {
        Environment env = Environment.builder()
                .scanRuntimeClasspath()
                .build();

        java.util.List<String> recipes = env.listRecipes().stream()
                .map(r -> r.getName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        java.nio.file.Files.write(java.nio.file.Paths.get("recipes_utf8.txt"), recipes,
                java.nio.charset.StandardCharsets.UTF_8);
    }
}
