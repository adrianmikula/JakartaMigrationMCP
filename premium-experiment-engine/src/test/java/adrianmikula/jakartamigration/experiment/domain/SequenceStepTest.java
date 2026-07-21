package adrianmikula.jakartamigration.experiment.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SequenceStepTest {

    @Test
    void openRewriteFactory_createsStep() {
        SequenceStep step = SequenceStep.openRewrite("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
        assertEquals(SequenceStepType.OPENREWRITE, step.type());
        assertEquals("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta", step.recipe());
        assertNull(step.recipeVersion());
    }

    @Test
    void openRewriteWithVersion_factory_createsStep() {
        SequenceStep step = SequenceStep.openRewrite("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta", "2.5.0");
        assertEquals(SequenceStepType.OPENREWRITE, step.type());
        assertEquals("2.5.0", step.recipeVersion());
    }

    @Test
    void dependencyUpgradeFactory_createsStep() {
        SequenceStep step = SequenceStep.dependencyUpgrade("jakarta.servlet", "jakarta.servlet-api", "5.0.0");
        assertEquals(SequenceStepType.DEPENDENCY_UPGRADE, step.type());
        assertEquals("jakarta.servlet", step.groupId());
        assertEquals("jakarta.servlet-api", step.artifactId());
        assertEquals("5.0.0", step.version());
    }

    @Test
    void eclipseTransformerFactory_createsStepWithDefaults() {
        SequenceStep step = SequenceStep.eclipseTransformer("8", "11");
        assertEquals(SequenceStepType.ECLIPSE_TRANSFORMER, step.type());
        assertEquals("8", step.sourceLevel());
        assertEquals("11", step.targetLevel());
        assertEquals(List.of("CLASSFILES", "XML", "PROPERTIES"), step.transformations());
    }

    @Test
    void gradleJakartaPluginFactory_createsStep() {
        SequenceStep step = SequenceStep.gradleJakartaPlugin("1.0.0");
        assertEquals(SequenceStepType.GRADLE_JAKARTA_PLUGIN, step.type());
        assertEquals("1.0.0", step.pluginVersion());
    }

    @Test
    void regexReplacementFactory_createsStep() {
        SequenceStep step = SequenceStep.regexReplacement("javax\\.xml\\.bind", "jakarta\\.xml\\.bind", "**/*.java");
        assertEquals(SequenceStepType.REGEX_REPLACEMENT, step.type());
        assertEquals("javax\\.xml\\.bind", step.pattern());
        assertEquals("jakarta\\.xml\\.bind", step.replacement());
        assertEquals("**/*.java", step.filePattern());
        assertEquals("g", step.flags());
    }
}
