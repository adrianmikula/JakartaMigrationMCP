package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Support tab component for the IntelliJ plugin.
 * Provides links to GitHub, LinkedIn, and sponsor pages.
 */
public class SupportComponent {
    private static final Logger LOG = Logger.getInstance(SupportComponent.class);
    
    // URLs - these can be configured or loaded from settings
    private static final String GITHUB_URL = "https://github.com/adrianmikula/jakarta-migration-mcp";
    private static final String GITHUB_ISSUES_URL = "https://github.com/adrianmikula/jakarta-migration-mcp/issues";
    private static final String GITHUB_SPONSOR_URL = "https://github.com/sponsors/adrianmikula";
    private static final String LINKEDIN_URL = "https://linkedin.com/in/adrianmikula";
    private static final String PLUGIN_PAGE_URL = "https://plugins.jetbrains.com/plugin/25558-jakarta-migration";
    
    private final JPanel panel;
    private final Project project;
    
    public SupportComponent(Project project) {
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
        JLabel headerLabel = new JLabel("Support & Resources");
        headerLabel.setFont(new Font(headerLabel.getFont().getName(), Font.BOLD, 18));
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(headerLabel);
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // About section
        contentPanel.add(createSectionHeader("About"));
        JTextArea aboutText = new JTextArea(
            "Jakarta Migration Plugin helps you migrate from javax to jakarta namespaces.\n" +
            "This plugin supports various scanning types including JPA, Bean Validation,\n" +
            "Servlet/JSP, CDI, and more."
        );
        aboutText.setEditable(false);
        aboutText.setBackground(null);
        aboutText.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        contentPanel.add(aboutText);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Links section
        contentPanel.add(createSectionHeader("Links"));
        
        // GitHub link
        contentPanel.add(createLinkButton(
            "GitHub Repository",
            "View source code, report issues, and contribute",
            GITHUB_URL
        ));
        
        // GitHub Issues link
        contentPanel.add(createLinkButton(
            "Report Issues",
            "Open a GitHub issue for bugs or feature requests",
            GITHUB_ISSUES_URL
        ));
        
        // Sponsor link
        contentPanel.add(createLinkButton(
            "❤️ Sponsor on GitHub",
            "Support the development of this plugin",
            GITHUB_SPONSOR_URL
        ));
        
        // LinkedIn link
        contentPanel.add(createLinkButton(
            "LinkedIn",
            "Connect with the developer on LinkedIn",
            LINKEDIN_URL
        ));
        
        // Plugin page link
        contentPanel.add(createLinkButton(
            "JetBrains Plugin Page",
            "Rate and review the plugin",
            PLUGIN_PAGE_URL
        ));
        
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Version info
        contentPanel.add(createSectionHeader("Version Info"));
        JLabel versionLabel = new JLabel("Version: 1.0.0 (Premium Edition)");
        versionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        versionLabel.setForeground(Color.GRAY);
        contentPanel.add(versionLabel);
        
        JLabel buildLabel = new JLabel("Build: " + getBuildTimestamp());
        buildLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        buildLabel.setForeground(Color.GRAY);
        contentPanel.add(buildLabel);
        
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
    
    private JPanel createLinkButton(String title, String description, String url) {
        JPanel linkPanel = new JPanel(new BorderLayout(10, 5));
        linkPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        linkPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        linkPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 13));
        titleLabel.setForeground(new Color(0, 100, 180));
        
        // Description label
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font(descLabel.getFont().getName(), Font.PLAIN, 11));
        descLabel.setForeground(Color.GRAY);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(titleLabel);
        textPanel.add(descLabel);
        
        // Arrow icon
        JLabel arrowLabel = new JLabel("→");
        arrowLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        arrowLabel.setForeground(new Color(0, 100, 180));
        
        linkPanel.add(textPanel, BorderLayout.CENTER);
        linkPanel.add(arrowLabel, BorderLayout.EAST);
        
        // Add mouse listener for click handling
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
            // Use Desktop.browse for cross-platform URL opening
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            java.net.URI uri = new java.net.URI(url);
            desktop.browse(uri);
            LOG.info("Opened URL: " + url);
        } catch (Exception e) {
            LOG.error("Failed to open URL: " + url, e);
            Messages.showErrorDialog(
                project,
                "Could not open URL: " + url + "\n\nYou can manually visit: " + url,
                "Open Link Failed"
            );
        }
    }
    
    private String getBuildTimestamp() {
        return "2026-02-20";
    }
}
