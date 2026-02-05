# Functionality Review and Implementation Roadmap

**Date:** 2026-02-04  
**Based On:** [`docs/research/market-analysis.md`](docs/research/market-analysis.md)  
**Purpose:** Comprehensive review of current functionality, gaps, and implementation plan

---

## Executive Summary

| Metric | Value |
|--------|-------|
| **Market Requirements Met** | 8/10 (80%) |
| **Core MCP Tools Implemented** | 8/10 |
| **Premium MCP Tools Implemented** | 5/5 (100%) |
| **Syntax Errors** | 1 (Critical) |
| **UI Components** | 4/7 tabs (57%) |

### Current Implementation Status

| Category | Status | Count |
|----------|--------|-------|
| ✅ Fully Implemented | Community + Premium Tools | 10 MCP tools |
| ⚠️ Partial/Broken | 1 syntax error in `buildArtifactLookupResponse` | 1 |
| ❌ Missing (UI) | 3 UI enhancements planned | 3 |

---

## 1. Current MCP Tools Implementation

### 1.1 Community (Free) Tools

| Tool | Status | File | Notes |
|------|--------|------|-------|
| `analyzeJakartaReadiness` | ✅ | [`CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java:66) | Full implementation |
| `detectBlockers` | ✅ | [`CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java:104) | Full implementation |
| `recommendVersions` | ✅ | [`CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java:139) | Full implementation |
| `lookupArtifact` | ✅ | [`CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java:179) | Maven Central lookup |
| `detectReflectionUsage` | ✅ | [`CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java:210) | String-based javax detection |
| `scanXmlNamespaces` | ✅ | [`CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java:241) | XML namespace scanning |
| `explainRuntimeFailure` | ✅ | [`CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java:506) | Stack trace analysis |

### 1.2 Premium Tools

| Tool | Status | File | Notes |
|------|--------|------|-------|
| `createMigrationPlan` | ✅ | [`JakartaMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java:114) | Premium - requires license |
| `analyzeMigrationImpact` | ✅ | [`JakartaMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java:161) | Premium - requires license |
| `verifyRuntime` | ✅ | [`JakartaMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java:203) | Premium - requires license |
| `applyAutoFixes` | ✅ | [`JakartaMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java:248) | Premium - requires license |
| `executeMigrationPlan` | ✅ | [`JakartaMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java:304) | Premium - requires license |

---

## 2. Critical Issue: Syntax Error

### Issue: Missing method call prefix in `buildArtifactLookupResponse`

**File:** [`CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java:425)

**Lines 425-429 (Current - Broken):**
```java
if (recommendation jakartaGroupId() != null) {
    json.append("    \"jakartaGroupId\": \"").append(escapeJson(recommendation jakartaGroupId())).append("\",\n");
    json.append("    \"jakartaArtifactId\": \"").append(escapeJson(recommendation jakartaArtifactId())).append("\",\n");
    json.append("    \"recommendedVersion\": \"").append(escapeJson(recommendation recommendedVersion())).append("\",\n");
}
```

**Expected (Fixed):**
```java
if (recommendation.jakartaGroupId() != null) {
    json.append("    \"jakartaGroupId\": \"").append(escapeJson(recommendation.jakartaGroupId())).append("\",\n");
    json.append("    \"jakartaArtifactId\": \"").append(escapeJson(recommendation.jakartaArtifactId())).append("\",\n");
    json.append("    \"recommendedVersion\": \"").append(escapeJson(recommendation.recommendedVersion())).append("\",\n");
}
```

**Impact:** The `lookupArtifact` MCP tool will fail to compile/execute

---

## 3. Remaining Gaps and Implementation

### 3.1 MCP Tools Gaps (Low Priority)

| Gap | Why It Matters | Implementation |
|-----|---------------|----------------|
| Gradle Support | Market research shows 40%+ projects use Gradle | 3-4 days effort |
| Dry Run Improvements | Better UX for premium auto-fixes | 1 day effort |
| Progress Reporting | Long migrations need feedback | 2 days effort |

### 3.2 UI Gaps (Current Sprint)

| UI Component | Status | Implementation File | Notes |
|--------------|--------|---------------------|-------|
| **Dashboard Tab** | ✅ Existing | [`MigrationToolWindow.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/MigrationToolWindow.java:75) | Already implemented |
| **Dependencies Tab** | ⚠️ Partial | [`DependenciesTableComponent.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/DependenciesTableComponent.java) | Missing XML/Reflection sections |
| **Dependency Graph Tab** | ✅ Existing | [`DependencyGraphComponent.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/DependencyGraphComponent.java) | Already implemented |
| **Migration Strategy Tab** | ❌ Hide | [`MigrationPhasesComponent.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/MigrationPhasesComponent.java) | Should hide/deprecate |
| **Refactor Tab (NEW)** | ❌ Missing | Create new | For refactoring tasks & auto-fix UI |
| **Runtime Tab (NEW)** | ❌ Missing | Create new | For stack trace analysis UI |
| **XML/Reflection Sections** | ❌ Missing | Add to Dependencies Tab | Show XML/Reflection issues |

### 3.3 Database Schema Gaps

| Table | Status | File | Notes |
|-------|--------|------|-------|
| `analysis_reports` | ✅ Existing | [`SqliteMigrationAnalysisStore.java`](migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/SqliteMigrationAnalysisStore.java) | Already implemented |
| `dependency_nodes` | ✅ Existing | [`SqliteMigrationAnalysisStore.java`](migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/SqliteMigrationAnalysisStore.java) | Already implemented |
| `migration_plans` | ✅ Existing | [`SqliteMigrationAnalysisStore.java`](migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/SqliteMigrationAnalysisStore.java) | Already implemented |
| `refactoring_tasks` | ❌ Missing | Add to schema | For tracking refactoring tasks |

---

## 4. Implementation Plan

### Phase 1: Critical Fix (Day 1)

#### Task 1.1: Fix Syntax Error in CommunityMigrationTools.java

```java
// File: mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java
// Lines 425-429: Fix missing method call prefix

// Change FROM:
if (recommendation jakartaGroupId() != null) {
    json.append("    \"jakartaGroupId\": \"").append(escapeJson(recommendation jakartaGroupId())).append("\",\n");
    json.append("    \"jakartaArtifactId\": \"").append(escapeJson(recommendation jakartaArtifactId())).append("\",\n");
    json.append("    \"recommendedVersion\": \"").append(escapeJson(recommendation recommendedVersion())).append("\",\n");
}

// Change TO:
if (recommendation.jakartaGroupId() != null) {
    json.append("    \"jakartaGroupId\": \"").append(escapeJson(recommendation.jakartaGroupId())).append("\",\n");
    json.append("    \"jakartaArtifactId\": \"").append(escapeJson(recommendation.jakartaArtifactId())).append("\",\n");
    json.append("    \"recommendedVersion\": \"").append(escapeJson(recommendation.recommendedVersion())).append("\",\n");
}
```

**Files to modify:**
- [`mcp-server/.../CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java)

---

### Phase 2: Database Schema Extension (Day 2)

#### Task 2.1: Add `refactoring_tasks` Table

**File:** [`migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/SqliteMigrationAnalysisStore.java`](migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/SqliteMigrationAnalysisStore.java)

```java
// Add to SqliteMigrationAnalysisStore.java

/**
 * Saves a refactoring task to the database.
 */
public void saveRefactoringTask(Path projectPath, RefactoringTask task) {
    try (var conn = getConnection(projectPath)) {
        // Ensure table exists
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS refactoring_tasks (
                id TEXT PRIMARY KEY,
                project_path TEXT NOT NULL,
                file_path TEXT NOT NULL,
                task_type TEXT NOT NULL,
                description TEXT,
                status TEXT DEFAULT 'pending',
                priority INTEGER DEFAULT 0,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                completed_at TEXT,
                error_message TEXT
            )
        """);
        
        String sql = """
            INSERT OR REPLACE INTO refactoring_tasks 
            (id, project_path, file_path, task_type, description, status, priority)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, task.id());
            stmt.setString(2, projectPath.toString());
            stmt.setString(3, task.filePath());
            stmt.setString(4, task.taskType().name());
            stmt.setString(5, task.description());
            stmt.setString(6, task.status().name());
            stmt.setInt(7, task.priority());
            stmt.executeUpdate();
        }
    } catch (SQLException e) {
        throw new RuntimeException("Failed to save refactoring task", e);
    }
}

/**
 * Gets all refactoring tasks for a project.
 */
public List<RefactoringTask> getRefactoringTasks(Path projectPath) {
    try (var conn = getConnection(projectPath)) {
        var tasks = new ArrayList<RefactoringTask>();
        
        String sql = "SELECT * FROM refactoring_tasks WHERE project_path = ? ORDER BY priority DESC";
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, projectPath.toString());
            
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(new RefactoringTask(
                        rs.getString("id"),
                        TaskType.valueOf(rs.getString("task_type")),
                        rs.getString("file_path"),
                        rs.getString("description"),
                        TaskStatus.valueOf(rs.getString("status")),
                        rs.getInt("priority")
                    ));
                }
            }
        }
        
        return tasks;
    } catch (SQLException e) {
        throw new RuntimeException("Failed to get refactoring tasks", e);
    }
}
```

**Domain Classes to Create:**
```java
// migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/domain/RefactoringTask.java
package adrianmikula.jakartamigration.analysis.persistence.domain;

public record RefactoringTask(
    String id,
    TaskType taskType,
    String filePath,
    String description,
    TaskStatus status,
    int priority
) {}

public enum TaskType {
    IMPORT_FIX,
    XML_NAMESPACE_FIX,
    REFLECTION_FIX,
    DEPENDENCY_UPDATE
}

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
```

---

### Phase 3: UI Enhancements (Days 3-5)

#### Task 3.1: Add Refactor Tab

**File:** Create [`intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/RefactorTabComponent.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/RefactorTabComponent.java)

```java
package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.analysis.persistence.domain.RefactoringTask;
import adrianmikula.jakartamigration.analysis.persistence.domain.TaskStatus;
import adrianmikula.jakartamigration.analysis.persistence.domain.TaskType;
import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RefactorTabComponent {
    private static final Logger LOG = Logger.getInstance(RefactorTabComponent.class);
    
    private final Project project;
    private final JPanel contentPanel;
    private final JTable tasksTable;
    private final DefaultTableModel tableModel;
    private final JButton applyFixButton;
    private final JButton applyAllButton;
    private final JLabel statusLabel;
    
    private static final String[] COLUMN_NAMES = {
        "Type", "File", "Description", "Priority", "Status"
    };
    
    public RefactorTabComponent(Project project) {
        this.project = project;
        this.contentPanel = new JPanel(new BorderLayout());
        this.tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tasksTable = new JTable(tableModel);
        this.applyFixButton = new JButton("Apply Fix");
        this.applyAllButton = new JButton("Apply All");
        this.statusLabel = new JLabel("No refactoring tasks");
        
        initializeUI();
    }
    
    private void initializeUI() {
        // Toolbar
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        applyFixButton.addActionListener(e -> applySelectedFix());
        applyAllButton.addActionListener(e -> applyAllFixes());
        toolbarPanel.add(applyFixButton);
        toolbarPanel.add(applyAllButton);
        toolbarPanel.add(Box.createHorizontalStrut(10));
        toolbarPanel.add(statusLabel);
        
        // Table with scroll
        JScrollPane scrollPane = new JScrollPane(tasksTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Premium indicator
        JPanel premiumPanel = createPremiumIndicator();
        
        contentPanel.add(toolbarPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(premiumPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createPremiumIndicator() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setBackground(new Color(245, 245, 245));
        
        JLabel iconLabel = new JLabel("🔒");
        JLabel textLabel = new JLabel("Premium Feature");
        textLabel.setFont(textLabel.getFont().deriveFont(Font.BOLD));
        
        panel.add(iconLabel);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(textLabel);
        
        return panel;
    }
    
    /**
     * Load refactoring tasks from the database.
     */
    public void loadRefactoringTasks(Path projectPath) {
        tableModel.setRowCount(0);
        
        try (var store = new SqliteMigrationAnalysisStore(projectPath)) {
            List<RefactoringTask> tasks = store.getRefactoringTasks(projectPath);
            
            for (RefactoringTask task : tasks) {
                Object[] row = {
                    getTypeIcon(task.taskType()),
                    task.filePath(),
                    task.description(),
                    task.priority(),
                    getStatusLabel(task.status())
                };
                tableModel.addRow(row);
            }
            
            statusLabel.setText(tasks.size() + " refactoring tasks");
        } catch (Exception e) {
            LOG.error("Failed to load refactoring tasks", e);
            statusLabel.setText("Error loading tasks");
        }
    }
    
    private String getTypeIcon(TaskType type) {
        return switch (type) {
            case IMPORT_FIX -> "📥";
            case XML_NAMESPACE_FIX -> "📄";
            case REFLECTION_FIX -> "🔍";
            case DEPENDENCY_UPDATE -> "📦";
        };
    }
    
    private String getStatusLabel(TaskStatus status) {
        return switch (status) {
            case PENDING -> "⏳ Pending";
            case IN_PROGRESS -> "🔄 In Progress";
            case COMPLETED -> "✅ Completed";
            case FAILED -> "❌ Failed";
        };
    }
    
    private void applySelectedFix() {
        int selectedRow = tasksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(contentPanel,
                "Please select a refactoring task to apply",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get task from selected row
        RefactoringTask task = getTaskFromRow(selectedRow);
        if (task != null) {
            // Call MCP tool or refactoring service
            LOG.info("Applying fix for task: " + task.id());
            // TODO: Implement fix application
        }
    }
    
    private void applyAllFixes() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(contentPanel,
                "No refactoring tasks to apply",
                "Empty List",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(contentPanel,
            "Apply " + rowCount + " refactoring fixes? This will modify your source files.",
            "Confirm Apply All",
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            for (int i = 0; i < rowCount; i++) {
                RefactoringTask task = getTaskFromRow(i);
                if (task != null) {
                    // Apply fix for each task
                    LOG.info("Applying fix for task: " + task.id());
                }
            }
            loadRefactoringTasks(Path.of(project.getBasePath()));
        }
    }
    
    private RefactoringTask getTaskFromRow(int row) {
        try {
            String filePath = (String) tableModel.getValueAt(row, 1);
            String description = (String) tableModel.getValueAt(row, 2);
            int priority = (Integer) tableModel.getValueAt(row, 3);
            
            TaskType type = TaskType.IMPORT_FIX; // Would need more info from row
            TaskStatus status = TaskStatus.PENDING;
            
            return new RefactoringTask(
                "task-" + System.currentTimeMillis(),
                type,
                filePath,
                description,
                status,
                priority
            );
        } catch (Exception e) {
            LOG.error("Failed to get task from row", e);
            return null;
        }
    }
    
    public JPanel getPanel() {
        return contentPanel;
    }
}
```

---

#### Task 3.2: Add Runtime Tab

**File:** Create [`intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/RuntimeTabComponent.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/RuntimeTabComponent.java)

```java
package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.runtimeverification.domain.ErrorAnalysis;
import adrianmikula.jakartamigration.runtimeverification.domain.RuntimeError;
import adrianmikula.jakartamigration.runtimeverification.service.ErrorAnalyzer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RuntimeTabComponent {
    private static final Logger LOG = Logger.getInstance(RuntimeTabComponent.class);
    
    private final Project project;
    private final JPanel contentPanel;
    private final JTextArea stackTraceInput;
    private final JButton analyzeButton;
    private final JPanel resultsPanel;
    private final JLabel categoryLabel;
    private final JLabel rootCauseLabel;
    private final DefaultListModel<String> factorsListModel;
    private final JList<String> factorsList;
    private final DefaultListModel<String> fixesListModel;
    private final JList<String> fixesList;
    
    public RuntimeTabComponent(Project project) {
        this.project = project;
        this.contentPanel = new JPanel(new BorderLayout());
        this.stackTraceInput = new JTextArea(10, 50);
        this.analyzeButton = new JButton("Analyze Stack Trace");
        this.resultsPanel = new JPanel(new BorderLayout());
        this.categoryLabel = new JLabel("Category: -");
        this.rootCauseLabel = new JLabel("Root Cause: -");
        this.factorsListModel = new DefaultListModel<>();
        this.factorsList = new JList<>(factorsListModel);
        this.fixesListModel = new DefaultListModel<>();
        this.fixesList = new JList<>(fixesListModel);
        
        initializeUI();
    }
    
    private void initializeUI() {
        // Input section
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JLabel inputLabel = new JLabel("Paste Stack Trace:");
        inputLabel.setFont(inputLabel.getFont().deriveFont(Font.BOLD));
        inputPanel.add(inputLabel, BorderLayout.NORTH);
        
        stackTraceInput.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollPane = new JScrollPane(stackTraceInput);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        inputPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        analyzeButton.addActionListener(e -> analyzeStackTrace());
        buttonPanel.add(analyzeButton);
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Results section
        resultsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        resultsPanel.setVisible(false);
        
        JPanel summaryPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Analysis Summary"));
        
        categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.BOLD));
        rootCauseLabel.setFont(rootCauseLabel.getFont().deriveFont(Font.PLAIN));
        
        summaryPanel.add(categoryLabel);
        summaryPanel.add(rootCauseLabel);
        
        // Factors panel
        JPanel factorsPanel = new JPanel(new BorderLayout());
        factorsPanel.setBorder(BorderFactory.createTitledBorder("Contributing Factors"));
        factorsPanel.add(new JScrollPane(factorsList), BorderLayout.CENTER);
        
        // Fixes panel
        JPanel fixesPanel = new JPanel(new BorderLayout());
        fixesPanel.setBorder(BorderFactory.createTitledBorder("Suggested Fixes"));
        fixesPanel.add(new JScrollPane(fixesList), BorderLayout.CENTER);
        
        // Bottom panel with factors and fixes
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        bottomPanel.add(factorsPanel);
        bottomPanel.add(fixesPanel);
        
        resultsPanel.add(summaryPanel, BorderLayout.NORTH);
        resultsPanel.add(bottomPanel, BorderLayout.CENTER);
        
        // Premium indicator
        JPanel premiumPanel = createPremiumIndicator();
        
        // Main layout
        contentPanel.add(inputPanel, BorderLayout.NORTH);
        contentPanel.add(resultsPanel, BorderLayout.CENTER);
        contentPanel.add(premiumPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createPremiumIndicator() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setBackground(new Color(245, 245, 245));
        
        JLabel iconLabel = new JLabel("🔒");
        JLabel textLabel = new JLabel("Premium Feature");
        textLabel.setFont(textLabel.getFont().deriveFont(Font.BOLD));
        
        panel.add(iconLabel);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(textLabel);
        
        return panel;
    }
    
    /**
     * Analyze the stack trace and display results.
     */
    private void analyzeStackTrace() {
        String stackTrace = stackTraceInput.getText().trim();
        
        if (stackTrace.isEmpty()) {
            JOptionPane.showMessageDialog(contentPanel,
                "Please paste a stack trace to analyze",
                "Empty Input",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Parse stack trace
            List<String> lines = List.of(stackTrace.split("\\r?\\n"));
            ErrorAnalyzer errorAnalyzer = new ErrorAnalyzer();
            List<RuntimeError> errors = errorAnalyzer.parseErrorsFromOutput(lines, List.of());
            
            if (errors.isEmpty()) {
                JOptionPane.showMessageDialog(contentPanel,
                    "No errors found in the provided stack trace",
                    "No Errors",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // Analyze errors
            Path projectPath = project.getBasePath() != null 
                ? Paths.get(project.getBasePath()) 
                : Paths.get(".");
            
            adrianmikula.jakartamigration.runtimeverification.domain.MigrationContext context = 
                new adrianmikula.jakartamigration.runtimeverification.domain.MigrationContext(
                    projectPath,
                    false,
                    ""
                );
            
            ErrorAnalysis analysis = errorAnalyzer.analyzeErrors(errors, context);
            
            // Display results
            displayAnalysisResults(analysis, errors);
            
        } catch (Exception e) {
            LOG.error("Failed to analyze stack trace", e);
            JOptionPane.showMessageDialog(contentPanel,
                "Analysis failed: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void displayAnalysisResults(ErrorAnalysis analysis, List<RuntimeError> errors) {
        resultsPanel.setVisible(true);
        
        categoryLabel.setText("Category: " + analysis.category());
        rootCauseLabel.setText("Root Cause: " + analysis.rootCause());
        
        // Clear and populate factors
        factorsListModel.clear();
        for (String factor : analysis.contributingFactors()) {
            factorsListModel.addElement("• " + factor);
        }
        
        // Clear and populate fixes
        fixesListModel.clear();
        for (var fix : analysis.suggestedFixes()) {
            fixesListModel.addElement("[" + fix.priority() + "] " + fix.description());
        }
        
        // Refresh UI
        contentPanel.revalidate();
        contentPanel.repaint();
    }
    
    public JPanel getPanel() {
        return contentPanel;
    }
}
```

---

#### Task 3.3: Update MigrationToolWindow.java

**File:** [`intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/MigrationToolWindow.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/MigrationToolWindow.java)

**Changes:**

1. Add imports for new components:
```java
import adrianmikula.jakartamigration.intellij.ui.RefactorTabComponent;
import adrianmikula.jakartamigration.intellij.ui.RuntimeTabComponent;
```

2. Add component fields:
```java
private RefactorTabComponent refactorTabComponent;
private RuntimeTabComponent runtimeTabComponent;
```

3. Modify `initializeContent()` to add new tabs:
```java
// Existing tabs...
dashboardComponent = new DashboardComponent(project, this::handleAnalyzeProject);
tabbedPane.addTab("Dashboard", dashboardComponent.getPanel());

dependenciesComponent = new DependenciesTableComponent(project);
tabbedPane.addTab("Dependencies", dependenciesComponent.getPanel());

dependencyGraphComponent = new DependencyGraphComponent(project);
tabbedPane.addTab("Dependency Graph", dependencyGraphComponent.getPanel());

// NEW: Refactor tab
refactorTabComponent = new RefactorTabComponent(project);
tabbedPane.addTab("Refactor", refactorTabComponent.getPanel());

// NEW: Runtime tab
runtimeTabComponent = new RuntimeTabComponent(project);
tabbedPane.addTab("Runtime", runtimeTabComponent.getPanel());

// REMOVED: Migration Strategy tab (now deprecated)
// migrationPhasesComponent = new MigrationPhasesComponent(project);
// tabbedPane.addTab("Migration Strategy", migrationPhasesComponent.getPanel());
```

4. Add refresh logic for new tabs in `updateDashboardFromReport()`:
```java
// After loading dependencies...
// Load refactoring tasks if available
try {
    refactorTabComponent.loadRefactoringTasks(projectPath);
} catch (Exception e) {
    LOG.warn("Failed to load refactoring tasks", e);
}
```

---

#### Task 3.4: Add XML/Reflection Sections to Dependencies Tab

**File:** Update [`intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/DependenciesTableComponent.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/DependenciesTableComponent.java)

```java
// Add after the main dependencies table in DependenciesTableComponent.java

/**
 * Create a section showing XML namespace issues found during scanning.
 */
public JPanel createXmlNamespaceSection(List<XmlFileUsage> xmlUsages) {
    JPanel sectionPanel = new JPanel(new BorderLayout());
    sectionPanel.setBorder(BorderFactory.createTitledBorder("XML Namespace Issues"));
    
    if (xmlUsages.isEmpty()) {
        JLabel emptyLabel = new JLabel("No XML namespace issues found ✓");
        emptyLabel.setForeground(new Color(0, 128, 0));
        sectionPanel.add(emptyLabel, BorderLayout.CENTER);
        return sectionPanel;
    }
    
    DefaultTableModel model = new DefaultTableModel(
        new Object[]{"File", "Namespace", "Class References"}, 0
    );
    
    for (XmlFileUsage usage : xmlUsages) {
        model.addRow(new Object[]{
            usage.filePath(),
            usage.namespaceUsages().size() + " namespaces",
            usage.classReferences().size() + " references"
        });
    }
    
    JTable table = new JTable(model);
    JScrollPane scrollPane = new JScrollPane(table);
    sectionPanel.add(scrollPane, BorderLayout.CENTER);
    
    return sectionPanel;
}

/**
 * Create a section showing reflection-based javax usage.
 */
public JPanel createReflectionSection(ReflectionSummary summary) {
    JPanel sectionPanel = new JPanel(new BorderLayout());
    sectionPanel.setBorder(BorderFactory.createTitledBorder("Reflection-Based javax Usage"));
    
    if (!summary.hasCriticalUsages()) {
        JLabel emptyLabel = new JLabel("No critical reflection usages found ✓");
        emptyLabel.setForeground(new Color(0, 128, 0));
        sectionPanel.add(emptyLabel, BorderLayout.CENTER);
        return sectionPanel;
    }
    
    // Show warning
    JPanel warningPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    warningPanel.setBackground(new Color(255, 248, 220));
    warningLabel.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
    warningPanel.add(warningLabel);
    sectionPanel.add(warningPanel, BorderLayout.NORTH);
    
    // Show critical types
    JPanel typesPanel = new JPanel(new GridLayout(0, 1));
    typesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    
    for (String type : summary.criticalTypes()) {
        JLabel typeLabel = new JLabel("⚠️ " + type);
        typesPanel.add(typeLabel);
    }
    
    JScrollPane scrollPane = new JScrollPane(typesPanel);
    sectionPanel.add(scrollPane, BorderLayout.CENTER);
    
    return sectionPanel;
}
```

---

## 5. Implementation Summary

### Files to Create

| File | Purpose |
|------|---------|
| [`migration-core/.../domain/RefactoringTask.java`](migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/domain/RefactoringTask.java) | Domain class for refactoring tasks |
| [`migration-core/.../domain/TaskType.java`](migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/domain/TaskType.java) | Enum for task types |
| [`migration-core/.../domain/TaskStatus.java`](migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/domain/TaskStatus.java) | Enum for task status |
| [`intellij-plugin/.../ui/RefactorTabComponent.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/RefactorTabComponent.java) | Refactor tab UI |
| [`intellij-plugin/.../ui/RuntimeTabComponent.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/RuntimeTabComponent.java) | Runtime analysis tab UI |

### Files to Modify

| File | Change |
|------|--------|
| [`mcp-server/.../CommunityMigrationTools.java`](mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java) | Fix syntax error at lines 425-429 |
| [`migration-core/.../SqliteMigrationAnalysisStore.java`](migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/SqliteMigrationAnalysisStore.java) | Add refactoring_tasks table and methods |
| [`intellij-plugin/.../ui/MigrationToolWindow.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/MigrationToolWindow.java) | Add Refactor and Runtime tabs |
| [`intellij-plugin/.../ui/DependenciesTableComponent.java`](intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/DependenciesTableComponent.java) | Add XML/Reflection sections |

### Estimated Effort

| Phase | Effort | Description |
|-------|--------|-------------|
| Phase 1 | 1 hour | Fix syntax error |
| Phase 2 | 1 day | Database schema extension |
| Phase 3 | 3 days | UI components |
| **Total** | **~4 days** | Complete implementation |

---

## 6. Verification Steps

After implementing the changes, verify:

1. **Syntax Error Fix:**
   ```bash
   cd mcp-server && ../gradlew compileJava
   ```

2. **Database Schema:**
   ```bash
   cd migration-core && ../gradlew test --tests "*SqliteMigrationAnalysisStoreTest*"
   ```

3. **UI Components:**
   - Build IntelliJ plugin: `cd intellij-plugin && ../gradlew buildPlugin`
   - Install plugin in IntelliJ
   - Verify new tabs appear

4. **MCP Tools:**
   ```bash
   cd mcp-server && ../gradlew bootRun
   # Test with MCP client
   ```

---

## Related Documentation

- [`docs/research/market-analysis.md`](docs/research/market-analysis.md) - Original market research
- [`docs/improvements/FUNCTIONALITY_GAP_ANALYSIS_2026-02-04.md`](docs/improvements/FUNCTIONALITY_GAP_ANALYSIS_2026-02-04.md) - Previous gap analysis
- [`docs/improvements/INTELLIJ_PLUGIN_UI_ENHANCEMENT_2026-02-04.md`](docs/improvements/INTELLIJ_PLUGIN_UI_ENHANCEMENT_2026-02-04.md) - UI enhancement plan
- [`docs/architecture/README.md`](docs/architecture/README.md) - Architecture overview
