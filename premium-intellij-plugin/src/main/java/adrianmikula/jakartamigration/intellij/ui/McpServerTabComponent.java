package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
    
    public McpServerTabComponent(Project project) {
        this.project = project;
        this.panel = createPanel();
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
        docPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // MCP Documentation link
        docPanel.add(createLinkButton(
                "MCP Documentation",
                "Learn how to use the MCP server with AI Assistants",
                MCP_DOCS_URL));
        
        // GitHub link
        docPanel.add(createLinkButton(
                "GitHub Repository",
                "View source code and examples",
                GITHUB_URL));
        
        return docPanel;
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
