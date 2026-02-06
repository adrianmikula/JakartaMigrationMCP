package adrianmikula.jakartamigration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for Jakarta Migration MCP Server.
 * This is the entry point for the MCP server that provides Jakarta EE migration tools.
 */
@SpringBootApplication
public class JakartaMigrationMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(JakartaMigrationMcpApplication.class, args);
    }
}
