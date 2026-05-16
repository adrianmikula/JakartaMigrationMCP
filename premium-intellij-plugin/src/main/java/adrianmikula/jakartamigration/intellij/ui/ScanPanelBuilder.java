package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.ui.components.TruncationNoticePanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper class for building scan result panels with consistent structure.
 * Extracted from SourceScansComponent to reduce code duplication.
 */
public class ScanPanelBuilder {
    
    /**
     * Creates a scan panel with table, status label, and truncation notice.
     * 
     * @param columns Table column names
     * @param table The table component to configure
     * @param statusLabel The status label to configure
     * @param truncationNotice The truncation notice panel
     * @param project The project for file opening
     * @return Configured panel
     */
    public static JPanel createScanPanel(String[] columns, JBTable table, JLabel statusLabel, 
                                         TruncationNoticePanel truncationNotice, Project project) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Status label
        statusLabel.setText("Not scanned yet");
        statusLabel.setForeground(Color.GRAY);

        // Table
        table.setModel(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(table, project);

        // Truncation notice
        truncationNotice.setVisible(false);

        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(truncationNotice, BorderLayout.SOUTH);

        return panel;
    }
    
    /**
     * Configures table with standard settings.
     * Hides the last column (Path column) and adds file open listener.
     */
    private static void setupTable(JBTable table, Project project) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        // Hide the last column (Path column)
        TableColumnModel columnModel = table.getColumnModel();
        int pathColumnIndex = columnModel.getColumnCount() - 1;
        columnModel.removeColumn(columnModel.getColumn(pathColumnIndex));
        
        // Add double-click listener to open file
        addFileOpenListener(table, pathColumnIndex, project);
    }
    
    /**
     * Adds file open listener to table.
     */
    private static void addFileOpenListener(JBTable table, int pathColumnIndex, Project project) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        // Get the path from the hidden column
                        int modelRow = table.convertRowIndexToModel(row);
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        String path = (String) model.getValueAt(modelRow, pathColumnIndex);
                        
                        if (path != null && !path.isEmpty()) {
                            openFileInEditor(project, path);
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Opens a file in the IDE editor.
     */
    private static void openFileInEditor(Project project, String filePath) {
        try {
            Path path = Paths.get(filePath);
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(path.toFile());
            
            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true, true);
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }
}
