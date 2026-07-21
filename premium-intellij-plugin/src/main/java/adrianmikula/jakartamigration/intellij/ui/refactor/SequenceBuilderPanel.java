package adrianmikula.jakartamigration.intellij.ui.refactor;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.experiment.domain.MigrationSequence;
import adrianmikula.jakartamigration.experiment.domain.SequenceStep;
import adrianmikula.jakartamigration.experiment.domain.SequenceStepType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Panel for building recipe sequences.
 * Allows users to select and order multiple refactor recipes into a sequence.
 */
public class SequenceBuilderPanel {
    private static final Logger LOG = Logger.getInstance(SequenceBuilderPanel.class);

    private final JPanel panel;
    private final Project project;
    private final RecipeService recipeService;
    
    // UI Components
    private JBTextField sequenceNameField;
    private JTextArea sequenceDescriptionArea;
    private JBList<RecipeDefinition> availableRecipesList;
    private JBList<SequenceStep> selectedSequenceList;
    private DefaultListModel<RecipeDefinition> availableRecipesModel;
    private DefaultListModel<SequenceStep> selectedSequenceModel;
    
    // Callbacks
    private Runnable onSequenceSaved;

    public SequenceBuilderPanel(@NotNull Project project, RecipeService recipeService) {
        this.project = project;
        this.recipeService = recipeService;
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    private void initializeComponent() {
        // Header panel with sequence metadata
        JPanel headerPanel = createHeaderPanel();
        
        // Main content with two lists
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        // Available recipes panel
        JPanel availablePanel = createAvailableRecipesPanel();
        
        // Selected sequence panel
        JPanel selectedPanel = createSelectedSequencePanel();
        
        mainPanel.add(availablePanel);
        mainPanel.add(selectedPanel);
        
        // Button panel at bottom
        JPanel buttonPanel = createButtonPanel();
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(mainPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Load available recipes
        loadAvailableRecipes();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JBPanel<>(new GridBagLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Sequence name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        header.add(new JBLabel("Sequence Name:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.9;
        sequenceNameField = new JBTextField(30);
        header.add(sequenceNameField, gbc);
        
        // Sequence description
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.1;
        header.add(new JBLabel("Description:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.9;
        sequenceDescriptionArea = new JTextArea(3, 30);
        sequenceDescriptionArea.setLineWrap(true);
        sequenceDescriptionArea.setWrapStyleWord(true);
        JBScrollPane descScroll = new JBScrollPane(sequenceDescriptionArea);
        header.add(descScroll, gbc);
        
        return header;
    }

    private JPanel createAvailableRecipesPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            "Available Recipes",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));
        
        availableRecipesModel = new DefaultListModel<>();
        availableRecipesList = new JBList<>(availableRecipesModel);
        availableRecipesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableRecipesList.setCellRenderer(new RecipeListCellRenderer());
        
        JBScrollPane scrollPane = new JBScrollPane(availableRecipesList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Filter by category
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JBLabel filterLabel = new JBLabel("Filter:");
        JComboBox<RecipeCategory> categoryFilter = new JComboBox<>(RecipeCategory.values());
        categoryFilter.addActionListener(e -> filterRecipes((RecipeCategory) categoryFilter.getSelectedItem()));
        filterPanel.add(filterLabel);
        filterPanel.add(categoryFilter);
        panel.add(filterPanel, BorderLayout.NORTH);
        
        return panel;
    }

    private JPanel createSelectedSequencePanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            "Selected Sequence",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));
        
        selectedSequenceModel = new DefaultListModel<>();
        selectedSequenceList = new JBList<>(selectedSequenceModel);
        selectedSequenceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectedSequenceList.setCellRenderer(new SequenceStepListCellRenderer());
        
        JBScrollPane scrollPane = new JBScrollPane(selectedSequenceList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Reorder buttons
        JPanel reorderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton moveUpButton = new JButton("↑");
        JButton moveDownButton = new JButton("↓");
        JButton removeButton = new JButton("Remove");
        
        moveUpButton.addActionListener(e -> moveStepUp());
        moveDownButton.addActionListener(e -> moveStepDown());
        removeButton.addActionListener(e -> removeStep());
        
        reorderPanel.add(moveUpButton);
        reorderPanel.add(moveDownButton);
        reorderPanel.add(removeButton);
        panel.add(reorderPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton addButton = new JButton("Add Selected Recipe →");
        JButton saveButton = new JButton("Save Sequence");
        JButton clearButton = new JButton("Clear");
        
        addButton.addActionListener(e -> addSelectedRecipe());
        saveButton.addActionListener(e -> saveSequence());
        clearButton.addActionListener(e -> clearSequence());
        
        panel.add(addButton);
        panel.add(saveButton);
        panel.add(clearButton);
        
        return panel;
    }

    private void loadAvailableRecipes() {
        availableRecipesModel.clear();
        Path projectPath = java.nio.file.Paths.get(project.getBasePath());
        
        for (RecipeCategory category : RecipeCategory.values()) {
            List<RecipeDefinition> recipes = recipeService.getRecipesByCategory(category, projectPath);
            for (RecipeDefinition recipe : recipes) {
                availableRecipesModel.addElement(recipe);
            }
        }
    }

    private void filterRecipes(RecipeCategory category) {
        availableRecipesModel.clear();
        Path projectPath = java.nio.file.Paths.get(project.getBasePath());
        
        List<RecipeDefinition> recipes = recipeService.getRecipesByCategory(category, projectPath);
        for (RecipeDefinition recipe : recipes) {
            availableRecipesModel.addElement(recipe);
        }
    }

    private void addSelectedRecipe() {
        RecipeDefinition selected = availableRecipesList.getSelectedValue();
        if (selected == null) {
            return;
        }
        
        // Convert to SequenceStep using factory method
        SequenceStep step = SequenceStep.openRewrite(selected.getOpenRewriteRecipeName());
        
        selectedSequenceModel.addElement(step);
    }

    private void moveStepUp() {
        int index = selectedSequenceList.getSelectedIndex();
        if (index <= 0) {
            return;
        }
        
        SequenceStep step = selectedSequenceModel.remove(index);
        selectedSequenceModel.add(index - 1, step);
        selectedSequenceList.setSelectedIndex(index - 1);
    }

    private void moveStepDown() {
        int index = selectedSequenceList.getSelectedIndex();
        if (index < 0 || index >= selectedSequenceModel.size() - 1) {
            return;
        }
        
        SequenceStep step = selectedSequenceModel.remove(index);
        selectedSequenceModel.add(index + 1, step);
        selectedSequenceList.setSelectedIndex(index + 1);
    }

    private void removeStep() {
        int index = selectedSequenceList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        
        selectedSequenceModel.remove(index);
    }

    private void clearSequence() {
        selectedSequenceModel.clear();
        sequenceNameField.setText("");
        sequenceDescriptionArea.setText("");
    }

    private void saveSequence() {
        String name = sequenceNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "Please enter a sequence name.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (selectedSequenceModel.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "Please add at least one recipe to the sequence.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String description = sequenceDescriptionArea.getText().trim();
        List<SequenceStep> steps = new ArrayList<>();
        for (int i = 0; i < selectedSequenceModel.size(); i++) {
            steps.add(selectedSequenceModel.getElementAt(i));
        }
        
        MigrationSequence sequence = new MigrationSequence(name, description, steps, null, List.of());
        
        // TODO: Save sequence using SequenceService
        LOG.info("Saving sequence: " + name + " with " + steps.size() + " steps");
        
        JOptionPane.showMessageDialog(panel, "Sequence saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        
        if (onSequenceSaved != null) {
            onSequenceSaved.run();
        }
    }

    public void loadSequence(MigrationSequence sequence) {
        sequenceNameField.setText(sequence.name());
        sequenceDescriptionArea.setText(sequence.description());
        
        selectedSequenceModel.clear();
        for (SequenceStep step : sequence.steps()) {
            selectedSequenceModel.addElement(step);
        }
    }

    public void setOnSequenceSaved(Runnable callback) {
        this.onSequenceSaved = callback;
    }

    public JPanel getPanel() {
        return panel;
    }

    /**
     * Custom cell renderer for recipe list
     */
    private static class RecipeListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof RecipeDefinition recipe) {
                setText(recipe.getName() + " (" + recipe.getRecipeType() + ")");
                setToolTipText(recipe.getDescription());
            }
            
            return this;
        }
    }

    /**
     * Custom cell renderer for sequence step list
     */
    private static class SequenceStepListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof SequenceStep step) {
                setText((index + 1) + ". " + step.type() + ": " + step.recipe());
            }
            
            return this;
        }
    }
}
