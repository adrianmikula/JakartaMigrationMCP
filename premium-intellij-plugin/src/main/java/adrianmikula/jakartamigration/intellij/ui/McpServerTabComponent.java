package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.Properties;

/**
 * MCP Server Tab Component - Displays MCP Server status and available tools.
 */
public class McpServerTabComponent {
    private static final Logger LOG = Logger.getInstance(McpServerTabComponent.class);
    
    private final Project project;
    private final JPanel panel;
    private JLabel statusLabel;
    private JTextArea toolsArea;
    
    private static final String MCP_DOCS_URL = "https://github.com/adrianmikula/JakartaMigrationMCP#mcp-server";
    private static final String GITHUB_URL = "https://github.com/adrianmikula/JakartaMigrationMCP";
    private static final String AI_GUIDELINES_PROPERTIES = "/ai-guidelines.properties";
    private Properties guidelinesProperties;
    
    public McpServerTabComponent(Project project) {
        this.project = project;
        loadGuidelinesProperties();
        this.panel = createPanel();
    }
    
    private void loadGuidelinesProperties() {
        try {
            guidelinesProperties = new Properties();
            InputStream is = getClass().getResourceAsStream(AI_GUIDELINES_PROPERTIES);
            if (is != null) {
                guidelinesProperties.load(is);
                LOG.info("Loaded AI guidelines properties");
            } else {
                LOG.warn("Could not load AI guidelines properties file");
                guidelinesProperties = new Properties();
            }
        } catch (Exception e) {
            LOG.error("Error loading AI guidelines properties", e);
            guidelinesProperties = new Properties();
        }
    }
    
    public JPanel getPanel() {
        return panel;
    }
    
    private JPanel createPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JLabel headerLabel = new JLabel("MCP Server");
        headerLabel.setFont(new Font(headerLabel.getFont().getName(), Font.BOLD, 18));
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(headerLabel);
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // Status section
        contentPanel.add(createSectionHeader("Server Status"));
        contentPanel.add(createStatusPanel());
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Available Tools section
        contentPanel.add(createSectionHeader("Available Tools"));
        contentPanel.add(createToolsPanel());
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Documentation section
        contentPanel.add(createSectionHeader("Documentation"));
        contentPanel.add(createDocumentationPanel());
        
        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private JLabel createSectionHeader(String title) {
        JLabel label = new JLabel(title);
        label.setFont(new Font(label.getFont().getName(), Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        return label;
    }
    
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout(10, 5));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        // Status indicator
        JPanel indicatorPanel = new JPanel();
        indicatorPanel.setLayout(new BoxLayout(indicatorPanel, BoxLayout.Y_AXIS));
        
        JLabel statusDot = new JLabel("●");
        statusDot.setFont(new Font(statusDot.getFont().getName(), Font.BOLD, 24));
        statusDot.setForeground(new Color(0, 180, 0)); // Green for ready
        statusDot.setAlignmentX(Component.CENTER_ALIGNMENT);
        indicatorPanel.add(statusDot);
        
        this.statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font(statusLabel.getFont().getName(), Font.BOLD, 13));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        indicatorPanel.add(statusLabel);
        
        // Description
        JPanel descPanel = new JPanel();
        descPanel.setLayout(new BoxLayout(descPanel, BoxLayout.Y_AXIS));
        descPanel.setBackground(null);
        
        JLabel titleLabel = new JLabel("MCP Server Status");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 13));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descPanel.add(titleLabel);
        
        JLabel descLabel = new JLabel("<html>The MCP server is available for AI Assistants.<br>Use it with Claude, Cursor, or other AI tools.</html>");
        descLabel.setFont(new Font(descLabel.getFont().getName(), Font.PLAIN, 11));
        descLabel.setForeground(Color.GRAY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descPanel.add(descLabel);
        
        statusPanel.add(indicatorPanel, BorderLayout.WEST);
        statusPanel.add(descPanel, BorderLayout.CENTER);
        
        return statusPanel;
    }
    
    private JPanel createToolsPanel() {
        JPanel toolsPanel = new JPanel(new BorderLayout(10, 5));
        toolsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        toolsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        String[] tools = {
            "analyzeMigrationImpact - Analyze the impact of migration on a project",
            "scanProject - Scan project for javax to jakarta migration issues",
            "getDependencyGraph - Get visual dependency graph",
            "applyRefactoring - Apply OpenRewrite refactoring recipes",
            "getMigrationStrategies - Get recommended migration strategies"
        };
        
        StringBuilder sb = new StringBuilder();
        sb.append("The following MCP tools are available:\n\n");
        for (String tool : tools) {
            sb.append("• ").append(tool).append("\n");
        }
        
        this.toolsArea = new JTextArea(sb.toString());
        toolsArea.setEditable(false);
        toolsArea.setBackground(null);
        toolsArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        toolsPanel.add(new JScrollPane(toolsArea), BorderLayout.CENTER);
        
        return toolsPanel;
    }
    
    private JPanel createDocumentationPanel() {
        JPanel docPanel = new JPanel();
        docPanel.setLayout(new BoxLayout(docPanel, BoxLayout.Y_AXIS));
        docPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        
        // AI Assistant Guidelines
        docPanel.add(createSectionHeader(getProperty("ai.section.title.guidelines", "AI Assistant Guidelines")));
        
        JPanel guidelinesPanel = new JPanel(new BorderLayout(10, 5));
        guidelinesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        guidelinesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        
        // Guidelines text area
        JTextArea guidelinesArea = new JTextArea();
        guidelinesArea.setText(getProperty("ai.prompts.analysis", "🤖 AI ASSISTANT PROMPTS FOR JAKARTA MIGRATION\n\n" +
            "Use these prompts with Claude, Cursor, or other AI assistants to perform Jakarta migration tasks:\n\n" +
            "📋 ANALYSIS PROMPTS:\n" +
            "\"Analyze my project for Jakarta migration readiness. Check for javax packages, dependencies, and potential blockers.\"\n\n" +
            "\"Scan my Java project and identify all javax imports that need to be migrated to jakarta.\"\n\n" +
            "\"Review my dependency tree and suggest Jakarta-compatible versions for all libraries.\"\n\n" +
            "\"Check my project's compatibility with Jakarta EE 9/10 and provide migration recommendations.\"\n\n" +
            "🔧 MIGRATION PROMPTS:\n" +
            "\"Generate a migration plan for converting my javax.servlet to jakarta.servlet imports. Include all affected files.\"\n\n" +
            "\"Create a step-by-step guide to migrate my javax.persistence to jakarta.persistence.\"\n\n" +
            "\"Help me refactor my javax.validation imports to jakarta.validation with proper error handling.\"\n\n" +
            "\"Update my Maven dependencies from javax to jakarta equivalents.\"\n\n" +
            "\"Migrate my JAX-RS imports from javax.ws.rs to jakarta.ws.rs.\"\n\n" +
            "📊 DEPENDENCY ANALYSIS:\n" +
            "\"Analyze my project's dependency graph and identify Jakarta migration risks.\"\n\n" +
            "\"Show me transitive dependencies that will be affected by Jakarta migration.\"\n\n" +
            "\"Check which of my dependencies have Jakarta equivalents available.\"\n\n" +
            "\"Generate a compatibility matrix for my current dependencies.\"\n\n" +
            "🏗️ BUILD SYSTEM SUPPORT:\n" +
            "\"Update my Gradle build file for Jakarta EE dependencies.\"\n\n" +
            "\"Configure my Maven project for Jakarta EE 10 migration.\"\n\n" +
            "\"Help me set up Jakarta EE 9/10 with my build system.\"\n\n" +
            "\"Verify my build configuration works with Jakarta dependencies.\"\n\n" +
            "🧪 TESTING & VALIDATION:\n" +
            "\"Create test cases for my migrated Jakarta code.\"\n\n" +
            "\"Help me verify that my Jakarta migration is working correctly.\"\n\n" +
            "\"Set up integration tests for Jakarta EE compatibility.\"\n\n" +
            "\"Check my application runtime after Jakarta migration.\"\n\n" +
            "📝 REPORTING:\n" +
            "\"Generate a migration report with before/after comparison.\"\n\n" +
            "\"Create a summary of all changes made during migration.\"\n\n" +
            "\"Document the migration process and outcomes.\"\n\n" +
            "\"Generate a compatibility report for stakeholders.\"\n\n" +
            "💡 TIPS:\n" +
            "• Always specify your Java version and Jakarta EE target version\n" +
            "• Provide context about your application server and framework\n" +
            "• Include error messages if you encounter issues\n" +
            "• Ask for step-by-step explanations when needed\n" +
            "• Request dry-run analysis before applying changes\n" +
            "• Verify changes in a test environment first"
        ));
        guidelinesArea.setEditable(false);
        guidelinesArea.setBackground(Color.WHITE);
        guidelinesArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        guidelinesArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JScrollPane guidelinesScroll = new JScrollPane(guidelinesArea);
        guidelinesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        guidelinesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        guidelinesPanel.add(guidelinesScroll, BorderLayout.CENTER);
        docPanel.add(guidelinesPanel);
        docPanel.add(Box.createVerticalStrut(10));
        
        // Example prompts section
        docPanel.add(createSectionHeader(getProperty("ai.section.title.examples", "Example Prompts")));
        
        JPanel examplesPanel = new JPanel(new BorderLayout(10, 5));
        examplesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        examplesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        
        JTextArea examplesArea = new JTextArea();
        examplesArea.setText(getProperty("ai.examples.quick.start", "🎯 QUICK START EXAMPLES:\n\n" +
            "\"I have a Spring Boot project using javax.validation. Analyze it for Jakarta migration.\"\n\n" +
            "\"My Maven project has javax.servlet dependencies. Help me migrate to Jakarta EE 10.\"\n\n" +
            "\"Check my Gradle project for Jakarta EE 9 compatibility and suggest changes.\"\n\n" +
            "\"Review my pom.xml and update all javax dependencies to jakarta equivalents.\"\n\n" +
            "🔧 ADVANCED EXAMPLES:\n\n" +
            "\"Create a comprehensive migration plan for my enterprise Java application. Include risk assessment and timeline.\"\n\n" +
            "\"Analyze my dependency graph and identify potential conflicts during Jakarta migration.\"\n\n" +
            "\"Generate refactoring recipes to convert javax.persistence to jakarta.persistence with proper error handling.\"\n\n" +
            "\"Help me implement Jakarta CDI beans to replace javax.enterprise.cdi imports.\"\n\n" +
            "\"Set up Jakarta EE 10 with WildFly and migrate my JAX-RS endpoints.\"\n\n" +
            "📊 WORKFLOW EXAMPLES:\n\n" +
            "\"Start by analyzing my project, then create migration plan, then implement changes step by step.\"\n\n" +
            "\"First check dependency compatibility, then update build files, then migrate source code.\"\n\n" +
            "\"Help me create a migration branch, implement changes, test, then merge to main.\"\n\n" +
            "\"Generate before/after reports and document the entire migration process.\"\n\n" +
            "🏗️ BUILD-SPECIFIC EXAMPLES:\n\n" +
            "\"Update my Maven pom.xml to use Jakarta EE 10 dependencies and update javax imports.\"\n\n" +
            "\"Modify my build.gradle.kts for Jakarta EE 9 and configure the Jakarta plugin.\"\n\n" +
            "\"Help me choose the right Jakarta EE version based on my application server.\"\n\n" +
            "\"Configure my Spring Boot application.properties for Jakarta EE migration.\"\n\n" +
            "\"Set up Jakarta EE 10 with Tomcat and update my web.xml configuration.\"\n\n" +
            "🧪 TESTING EXAMPLES:\n\n" +
            "\"Create unit tests for my migrated Jakarta validation code.\"\n\n" +
            "\"Help me write integration tests for Jakarta EE 10 compatibility.\"\n\n" +
            "\"Set up test containers with Jakarta EE runtimes for validation.\"\n\n" +
            "\"Generate performance tests before and after Jakarta migration.\"\n\n" +
            "\"Verify my application works with Jakarta EE 10 in a test environment.\"\n\n" +
            "📝 REPORTING EXAMPLES:\n\n" +
            "\"Generate a migration report showing all javax to jakarta conversions.\"\n\n" +
            "\"Create a compatibility matrix for my dependencies and Jakarta versions.\"\n\n" +
            "\"Document the migration process with screenshots and step-by-step guide.\"\n\n" +
            "\"Produce a stakeholder report with migration timeline and risks.\"\n\n" +
            "\"Generate a before/after code comparison report for migration.\"\n\n" +
            "💡 BEST PRACTICES:\n\n" +
            "• Always backup your project before starting migration\n" +
            "• Test in a development environment first\n" +
            "• Use version control to track changes\n" +
            "• Verify functionality after each major change\n" +
            "• Update documentation as you migrate\n" +
            "• Consider using automated tools for large-scale migrations\n" +
            "• Test with your target application server version\n" +
            "• Monitor performance after migration\n" +
            "• Plan rollback strategy in case of issues"
        ));
        examplesArea.setEditable(false);
        examplesArea.setBackground(Color.WHITE);
        examplesArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        examplesArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JScrollPane examplesScroll = new JScrollPane(examplesArea);
        examplesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        examplesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        examplesPanel.add(examplesScroll, BorderLayout.CENTER);
        docPanel.add(examplesPanel);
        docPanel.add(Box.createVerticalStrut(10));
        
        // MCP Documentation link
        docPanel.add(createLinkButton(
                "MCP Documentation",
                "Learn how to use MCP server with AI Assistants",
                MCP_DOCS_URL));
        
        // GitHub link
        docPanel.add(createLinkButton(
                "GitHub Repository",
                "View source code and examples",
                GITHUB_URL));
        
        return docPanel;
    }
    
    private String getProperty(String key, String defaultValue) {
        return guidelinesProperties != null ? guidelinesProperties.getProperty(key, defaultValue) : defaultValue;
    }
    
    private JPanel createLinkButton(String title, String description, String url) {
        JPanel linkPanel = new JPanel(new BorderLayout(10, 5));
        linkPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        linkPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        linkPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 13));
        titleLabel.setForeground(new Color(0, 100, 180));
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font(descLabel.getFont().getName(), Font.PLAIN, 11));
        descLabel.setForeground(Color.GRAY);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(titleLabel);
        textPanel.add(descLabel);
        
        JLabel arrowLabel = new JLabel("→");
        arrowLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        arrowLabel.setForeground(new Color(0, 100, 180));
        
        linkPanel.add(textPanel, BorderLayout.CENTER);
        linkPanel.add(arrowLabel, BorderLayout.EAST);
        
        linkPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openUrl(url);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                linkPanel.setBackground(new Color(240, 245, 255));
                linkPanel.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                linkPanel.setBackground(null);
                linkPanel.repaint();
            }
        });
        
        return linkPanel;
    }
    
    private void openUrl(String url) {
        try {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            java.net.URI uri = new java.net.URI(url);
            desktop.browse(uri);
            LOG.info("Opened URL: " + url);
        } catch (Exception e) {
            LOG.error("Failed to open URL: " + url, e);
        }
    }
}
