package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.*;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.credits.CreditType;
import adrianmikula.jakartamigration.credits.CreditsService;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RefactorTabComponent {
    private static final Logger LOG = Logger.getInstance(RefactorTabComponent.class);

    private final JPanel panel;
    private final Project project;
    private final RecipeService recipeService;
    private final JTabbedPane categoryTabs;
    private final Map<RecipeCategory, JPanel> gridMap = new HashMap<>();

    private final JPanel detailsPanel;
    private final JBLabel detailsTitleLabel;
    private final JEditorPane detailsDescPane;
    private final JBLabel detailsStatusLabel;
    private final JButton detailsApplyButton;
    private final JButton detailsUndoButton;
    private final JProgressBar progressBar;
    private final JLabel progressLabel;
    private RecipeDefinition selectedRecipe;

    // File modification results components
    private JPanel fileResultsPanel;
    private JBLabel fileResultsLabel;
    private JBScrollPane fileListScrollPane;
    private JPanel fileListPanel;
    private JButton toggleFileListButton;

    // Callback to notify other tabs (e.g. history) after a recipe runs
    private Runnable onRecipeExecuted;

    // Callback to notify UI to refresh credits display
    private Runnable onCreditUsed;

    // Credits service for freemium model
    private final CreditsService creditsService;

    public RefactorTabComponent(@NotNull Project project, RecipeService recipeService) {
        this.creditsService = new CreditsService();
        this.project = project;
        this.recipeService = recipeService;
        this.panel = new JBPanel<>(new BorderLayout());
        this.categoryTabs = new JTabbedPane();

        this.detailsPanel = new JPanel(new BorderLayout());
        this.detailsTitleLabel = new JBLabel("Select a recipe to see details");
        this.detailsDescPane = new JEditorPane("text/html", "");
        this.detailsStatusLabel = new JBLabel("");
        this.detailsApplyButton = new JButton("Apply Recipe");
        this.detailsUndoButton = new JButton("Undo Changes");
        this.progressBar = new JProgressBar();
        this.progressLabel = new JLabel("");

        // Initialize file results components
        this.fileResultsPanel = new JPanel(new BorderLayout());
        this.fileResultsLabel = new JBLabel("");
        this.fileListScrollPane = new JBScrollPane();
        this.fileListPanel = new JPanel();
        this.toggleFileListButton = new JButton("Show Modified Files");

        initializeComponent();
    }

    public void setOnRecipeExecuted(Runnable callback) {
        this.onRecipeExecuted = callback;
    }

    public void setOnCreditUsed(Runnable callback) {
        this.onCreditUsed = callback;
    }

    private void notifyCreditUsed() {
        if (onCreditUsed != null) {
            onCreditUsed.run();
        }
    }

    private void initializeComponent() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.7);
        splitPane.setBorder(null);

        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel headerPanel = new JBPanel<>(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));

        JBLabel titleLabel = new JBLabel("Migration Recipes");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.NORTH);

        JBLabel subtitleLabel = new JBLabel("Select and apply specialized templates to modernize your codebase.");
        subtitleLabel.setForeground(Color.GRAY);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);

        topPanel.add(headerPanel, BorderLayout.NORTH);

        for (RecipeCategory category : RecipeCategory.values()) {
            JPanel grid = new JPanel(new GridBagLayout());
            grid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            gridMap.put(category, grid);

            JBScrollPane scrollPane = new JBScrollPane(grid);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            categoryTabs.addTab(category.getDisplayName(), scrollPane);
        }
        topPanel.add(categoryTabs, BorderLayout.CENTER);

        setupDetailsPanel();

        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(detailsPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        refreshAllRecipes();
    }

    private void setupDetailsPanel() {
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        detailsPanel.setBackground(new Color(250, 250, 252));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        detailsTitleLabel.setFont(detailsTitleLabel.getFont().deriveFont(Font.BOLD, 14f));
        content.add(detailsTitleLabel);
        content.add(Box.createVerticalStrut(8));

        detailsDescPane.setEditable(false);
        detailsDescPane.setOpaque(false);
        detailsDescPane.setFont(detailsDescPane.getFont().deriveFont(12f));
        content.add(detailsDescPane);
        content.add(Box.createVerticalStrut(10));

        detailsStatusLabel.setFont(detailsStatusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        content.add(detailsStatusLabel);
        content.add(Box.createVerticalStrut(8));

        // Progress bar (hidden until recipe runs)
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(300, 18));
        progressLabel.setFont(progressLabel.getFont().deriveFont(Font.ITALIC, 11f));
        progressLabel.setForeground(Color.GRAY);
        progressLabel.setVisible(false);
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        progressPanel.setOpaque(false);
        progressPanel.add(progressBar);
        progressPanel.add(Box.createHorizontalStrut(8));
        progressPanel.add(progressLabel);
        content.add(progressPanel);

        // File results panel (hidden until recipe completes)
        setupFileResultsPanel();
        content.add(fileResultsPanel);
        content.add(Box.createVerticalStrut(8));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        buttonBar.setOpaque(false);
        detailsApplyButton.setEnabled(false);
        detailsApplyButton.addActionListener(e -> {
            if (selectedRecipe != null)
                handleApplyRecipe(selectedRecipe);
        });
        buttonBar.add(detailsApplyButton);

        detailsUndoButton.setEnabled(false);
        detailsUndoButton.addActionListener(e -> {
            if (selectedRecipe != null)
                handleUndo(selectedRecipe);
        });
        buttonBar.add(Box.createHorizontalStrut(10));
        buttonBar.add(detailsUndoButton);

        detailsPanel.add(content, BorderLayout.CENTER);
        detailsPanel.add(buttonBar, BorderLayout.SOUTH);
    }

    private void setupFileResultsPanel() {
        fileResultsPanel.setVisible(false);
        fileResultsPanel.setOpaque(false);
        fileResultsPanel.setLayout(new BoxLayout(fileResultsPanel, BoxLayout.Y_AXIS));

        // File results summary label
        fileResultsLabel.setFont(fileResultsLabel.getFont().deriveFont(Font.BOLD, 12f));
        fileResultsLabel.setForeground(new Color(0, 100, 0));
        fileResultsPanel.add(fileResultsLabel);
        fileResultsPanel.add(Box.createVerticalStrut(5));

        // Toggle button for file list
        toggleFileListButton.setFont(toggleFileListButton.getFont().deriveFont(Font.PLAIN, 11f));
        toggleFileListButton.setVisible(false);
        toggleFileListButton.addActionListener(e -> toggleFileListVisibility());
        fileResultsPanel.add(toggleFileListButton);
        fileResultsPanel.add(Box.createVerticalStrut(5));

        // File list scroll pane
        fileListScrollPane.setVisible(false);
        fileListScrollPane.setPreferredSize(new Dimension(400, 150));
        fileListScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        // File list panel
        fileListPanel.setLayout(new BoxLayout(fileListPanel, BoxLayout.Y_AXIS));
        fileListPanel.setBackground(Color.WHITE);

        fileListScrollPane.setViewportView(fileListPanel);
        fileResultsPanel.add(fileListScrollPane);
    }

    private void toggleFileListVisibility() {
        boolean isVisible = fileListScrollPane.isVisible();
        fileListScrollPane.setVisible(!isVisible);
        toggleFileListButton.setText(isVisible ? "Show Modified Files" : "Hide Modified Files");
        fileResultsPanel.revalidate();
        fileResultsPanel.repaint();
    }

    private void updateFileResultsDisplay(RecipeExecutionResult result) {
        if (result.success() && result.filesChanged() > 0) {
            // Show file results panel
            fileResultsPanel.setVisible(true);
            
            // Update summary label
            String summary = String.format("✓ Modified %d of %d files", 
                result.filesChanged(), result.filesProcessed());
            fileResultsLabel.setText(summary);
            
            // Show toggle button if there are files to display
            if (!result.changedFilePaths().isEmpty()) {
                toggleFileListButton.setVisible(true);
                toggleFileListButton.setText("Show Modified Files");
                
                // Update file list
                updateFileList(result.changedFilePaths());
            } else {
                toggleFileListButton.setVisible(false);
                fileListScrollPane.setVisible(false);
            }
        } else {
            // Hide file results panel if no files were changed or execution failed
            fileResultsPanel.setVisible(false);
        }
        
        fileResultsPanel.revalidate();
        fileResultsPanel.repaint();
    }

    private void updateFileList(List<String> filePaths) {
        fileListPanel.removeAll();
        
        for (String filePath : filePaths) {
            JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            filePanel.setOpaque(false);
            
            // File icon (using a simple text representation)
            JLabel fileIcon = new JLabel("📄");
            fileIcon.setFont(fileIcon.getFont().deriveFont(10f));
            filePanel.add(fileIcon);
            
            // File path label
            JLabel fileLabel = new JLabel(filePath);
            fileLabel.setFont(fileLabel.getFont().deriveFont(Font.PLAIN, 11f));
            fileLabel.setForeground(new Color(60, 60, 60));
            filePanel.add(fileLabel);
            
            fileListPanel.add(filePanel);
        }
        
        // Add some space at the bottom
        fileListPanel.add(Box.createVerticalStrut(10));
        
        fileListPanel.revalidate();
        fileListPanel.repaint();
    }

    private void updateDetails(RecipeDefinition recipe) {
        this.selectedRecipe = recipe;
        detailsTitleLabel.setText(recipe.getName());
        detailsDescPane.setText("<html><body style='font-family:sans-serif; font-size:11pt; color:#444;'>" +
                recipe.getDescription() + "<br><br><b>Type:</b> " + recipe.getRecipeType() +
                (recipe.getOpenRewriteRecipeName() != null ? "<br><b>Recipe:</b> " + recipe.getOpenRewriteRecipeName()
                        : "")
                +
                "</body></html>");

        refreshDetailsStatus(recipe);
        detailsApplyButton.setEnabled(true);
        detailsApplyButton.setText(recipe.getStatus() == RecipeStatus.NEVER_RUN ? "Apply Recipe" : "Run Again");

        detailsUndoButton.setEnabled(recipe.getStatus() == RecipeStatus.RUN_SUCCESS);
        detailsUndoButton.setVisible(recipe.getStatus() == RecipeStatus.RUN_SUCCESS);

        for (RecipeCategory cat : RecipeCategory.values()) {
            JPanel grid = gridMap.get(cat);
            if (grid != null) {
                for (Component comp : grid.getComponents()) {
                    if (comp instanceof JPanel card) {
                        updateCardSelectionState(card);
                    }
                }
            }
        }
    }

    private void refreshDetailsStatus(RecipeDefinition recipe) {
        String statusText = "Status: " + formatStatus(recipe.getStatus());
        if (recipe.getLastRunDate() != null) {
            statusText += " (Last run: " + recipe.getLastRunDate().toString().substring(0, 16).replace('T', ' ') + ")";
        }
        detailsStatusLabel.setText(statusText);
    }

    private String formatStatus(RecipeStatus status) {
        return switch (status) {
            case RUN_SUCCESS -> "Success";
            case RUN_UNDONE -> "Undone";
            case RUN_FAILED -> "Failed";
            default -> "Never run";
        };
    }

    private void refreshAllRecipes() {
        if (recipeService == null)
            return;
        java.nio.file.Path projectPath = java.nio.file.Paths.get(project.getBasePath());
        for (RecipeCategory category : RecipeCategory.values()) {
            List<RecipeDefinition> recipes = recipeService.getRecipesByCategory(category, projectPath);
            updateCategoryGrid(category, recipes);
        }
    }

    private void updateCategoryGrid(RecipeCategory category, List<RecipeDefinition> recipes) {
        JPanel grid = gridMap.get(category);
        if (grid == null)
            return;

        grid.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        if (recipes.isEmpty()) {
            gbc.gridwidth = 2;
            JLabel emptyLabel = new JLabel("No recipes available in this category.");
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            grid.add(emptyLabel, gbc);
        } else {
            int count = 0;
            for (RecipeDefinition recipe : recipes) {
                grid.add(createRecipeCard(recipe), gbc);
                count++;
                if (count % 2 == 0) {
                    gbc.gridx = 0;
                    gbc.gridy++;
                } else {
                    gbc.gridx = 1;
                }
            }
        }

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        grid.add(new JPanel(), gbc);

        grid.revalidate();
        grid.repaint();
    }

    private JPanel createRecipeCard(RecipeDefinition recipe) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.putClientProperty("recipe", recipe);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getBorderColor(recipe), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        card.setBackground(getCardBackground(recipe));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                updateDetails(recipe);
            }
        });

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(recipe.getName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        infoPanel.add(nameLabel);

        if (recipe.getLastRunDate() != null) {
            JLabel dateLabel = new JLabel(recipe.getLastRunDate().toString().substring(0, 10));
            dateLabel.setFont(dateLabel.getFont().deriveFont(Font.PLAIN, 10f));
            dateLabel.setForeground(new Color(100, 100, 100));
            infoPanel.add(Box.createVerticalStrut(2));
            infoPanel.add(dateLabel);
        }

        card.add(infoPanel, BorderLayout.CENTER);
        updateCardSelectionState(card);

        return card;
    }

    private void updateCardSelectionState(JPanel card) {
        RecipeDefinition recipe = (RecipeDefinition) card.getClientProperty("recipe");
        if (recipe == null)
            return;

        if (selectedRecipe != null && selectedRecipe.getName().equals(recipe.getName())) {
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 120, 215), 2),
                    BorderFactory.createEmptyBorder(9, 11, 9, 11)));
        } else {
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(getBorderColor(recipe), 1),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        }
    }

    private Color getBorderColor(RecipeDefinition recipe) {
        return switch (recipe.getStatus()) {
            case RUN_SUCCESS -> new Color(34, 139, 34);
            case RUN_UNDONE -> new Color(178, 34, 34);
            case RUN_FAILED -> Color.ORANGE;
            default -> new Color(210, 210, 210);
        };
    }

    private Color getCardBackground(RecipeDefinition recipe) {
        return switch (recipe.getStatus()) {
            case RUN_SUCCESS -> new Color(245, 255, 245);
            case RUN_UNDONE -> new Color(255, 245, 245);
            case RUN_FAILED -> new Color(255, 252, 245);
            default -> Color.WHITE;
        };
    }

    private void setRunning(boolean running) {
        progressBar.setVisible(running);
        progressLabel.setVisible(running);
        if (running) {
            progressLabel.setText("Running recipe...");
        }
        detailsApplyButton.setEnabled(!running);
        detailsUndoButton.setEnabled(!running);
    }

    private void handleApplyRecipe(RecipeDefinition recipe) {
        // Check if user is free tier and has refactor credits
        boolean isPremium = CheckLicense.isLicensed() != null && CheckLicense.isLicensed();
        if (!isPremium) {
            if (!creditsService.hasCredits(CreditType.ACTIONS)) {
                int result = Messages.showYesNoDialog(project,
                        "You've used all your action credits.\n\n" +
                        "Upgrade to Premium for unlimited refactors and undo operations.",
                        "Credits Exhausted",
                        "Upgrade to Premium",
                        "Cancel",
                        Messages.getWarningIcon());
                if (result == Messages.YES) {
                    openMarketplace();
                }
                return;
            }
        }

        int confirm = Messages.showYesNoDialog(project,
                "Apply recipe '" + recipe.getName() + "'?\n\nThis will modify source files.",
                "Apply Recipe", Messages.getQuestionIcon());

        if (confirm == Messages.YES) {
            // Use credit if free tier
            if (!isPremium) {
                boolean creditUsed = creditsService.useCredit(CreditType.ACTIONS, "Refactor", "apply_recipe");
                if (creditUsed) {
                    LOG.info("Credit deducted for applying recipe: " + recipe.getName() +
                        ". Remaining: " + creditsService.getRemainingCredits(CreditType.ACTIONS));
                    notifyCreditUsed(); // Refresh UI
                } else {
                    LOG.warn("Failed to deduct credit for recipe: " + recipe.getName());
                }
            }
            java.nio.file.Path projectPath = java.nio.file.Paths.get(project.getBasePath());
            setRunning(true);
            CompletableFuture.supplyAsync(() -> recipeService.applyRecipe(recipe.getName(), projectPath))
                    .thenAccept(result -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            setRunning(false);
                            if (result.success()) {
                                // Update file results display
                                updateFileResultsDisplay(result);
                                
                                // Create enhanced success message
                                String successMessage;
                                if (result.filesChanged() > 0) {
                                    successMessage = String.format("Successfully applied '%s'.\n\nModified %d of %d files.", 
                                        recipe.getName(), result.filesChanged(), result.filesProcessed());
                                } else {
                                    successMessage = String.format("Successfully applied '%s'.\n\nNo files required modification.", 
                                        recipe.getName());
                                }
                                Messages.showInfoMessage(project, successMessage, "Success");
                            } else {
                                Messages.showErrorDialog(project, "Failed: " + result.errorMessage(), "Error");
                            }
                            refreshAllRecipes();
                            // Re-select to update status/date in details panel
                            recipeService.getRecipes(projectPath).stream()
                                    .filter(r -> r.getName().equals(recipe.getName()))
                                    .findFirst().ifPresent(this::updateDetails);
                            // Notify history tab to refresh
                            if (onRecipeExecuted != null) {
                                onRecipeExecuted.run();
                            }
                        });
                    });
        }
    }

    private void handleUndo(RecipeDefinition recipe) {
        // Check if user is free tier and has refactor credits
        boolean isPremium = CheckLicense.isLicensed() != null && CheckLicense.isLicensed();
        if (!isPremium) {
            if (!creditsService.hasCredits(CreditType.ACTIONS)) {
                int result = Messages.showYesNoDialog(project,
                        "You've used all your action credits.\n\n" +
                        "Undo operations require credits. Upgrade to Premium for unlimited undo.",
                        "Credits Exhausted",
                        "Upgrade to Premium",
                        "Cancel",
                        Messages.getWarningIcon());
                if (result == Messages.YES) {
                    openMarketplace();
                }
                return;
            }
        }

        java.nio.file.Path projectPath = java.nio.file.Paths.get(project.getBasePath());

        Optional<RecipeExecutionHistory> lastExec = recipeService.getHistory(projectPath).stream()
                .filter(h -> h.getRecipeName().equals(recipe.getName()))
                .filter(h -> h.isSuccess() && !h.isUndo())
                .max(Comparator.comparing(RecipeExecutionHistory::getExecutedAt));

        if (lastExec.isEmpty()) {
            Messages.showWarningDialog(project, "No reversible history found for this recipe.", "Undo Failed");
            return;
        }

        Long executionId = lastExec.get().getId();

        int confirm = Messages.showYesNoDialog(project,
                "Undo the last application of '" + recipe.getName() + "'?\n\n" +
                        "This will attempt to revert changes made to files.",
                "Undo Recipe",
                Messages.getQuestionIcon());

        if (confirm == Messages.YES) {
            // Use credit if free tier
            if (!isPremium) {
                boolean creditUsed = creditsService.useCredit(CreditType.ACTIONS, "Refactor", "undo_recipe");
                if (creditUsed) {
                    LOG.info("Credit deducted for undoing recipe: " + recipe.getName() +
                        ". Remaining: " + creditsService.getRemainingCredits(CreditType.ACTIONS));
                    notifyCreditUsed(); // Refresh UI
                } else {
                    LOG.warn("Failed to deduct credit for undo: " + recipe.getName());
                }
            }
            setRunning(true);
            CompletableFuture.supplyAsync(() -> recipeService.undoRecipe(executionId, projectPath))
                    .thenAccept(result -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            setRunning(false);
                            if (result.success()) {
                                // Update file results display for undo operation
                                updateFileResultsDisplay(result);
                                
                                // Create enhanced undo message
                                String undoMessage;
                                if (result.filesChanged() > 0) {
                                    undoMessage = String.format("Successfully reverted changes.\n\nModified %d of %d files.", 
                                        result.filesChanged(), result.filesProcessed());
                                } else {
                                    undoMessage = "Successfully reverted changes. No files required modification.";
                                }
                                Messages.showInfoMessage(project, undoMessage, "Undo Complete");
                            } else {
                                Messages.showErrorDialog(project, "Undo failed: " + result.errorMessage(),
                                        "Undo Failed");
                            }
                            refreshAllRecipes();
                            if (onRecipeExecuted != null) {
                                onRecipeExecuted.run();
                            }
                        });
                    });
        }
    }

    /**
     * Opens the JetBrains Marketplace to purchase/upgrade.
     */
    private void openMarketplace() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://plugins.jetbrains.com/plugin/30093-jakarta-migration"));
        } catch (Exception ex) {
            Messages.showInfoMessage(project,
                    "Please visit: https://plugins.jetbrains.com/plugin/30093-jakarta-migration",
                    "Upgrade to Premium");
        }
    }

    public JPanel getPanel() {
        return panel;
    }
}
