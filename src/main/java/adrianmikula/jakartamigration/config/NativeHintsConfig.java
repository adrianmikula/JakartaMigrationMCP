package adrianmikula.jakartamigration.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Central hook for registering GraalVM native hints.
 *
 * For now this is intentionally minimal and does not register any
 * explicit reflection or resource hints. Spring Boot 3's AOT engine
 * is responsible for most configuration. As we discover native
 * issues in the Jakarta Migration MCP server, we can add targeted
 * hints here to keep native configuration in one place.
 */
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeHintsConfig.NativeHints.class)
public class NativeHintsConfig {

    static class NativeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // No-op for now – add reflection/resource/proxy hints here as needed
            // Example:
            // hints.reflection().registerType(SomeClassNeedingReflection.class, builder -> builder.withMembers());
        }
    }
}

