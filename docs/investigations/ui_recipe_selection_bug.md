# Investigation: UI Recipe Selection Bug

## Summary
In the IntelliJ plugin's "Refactor" tab, users reported that the "MigrateSOAP" recipe was always selected regardless of their actual selection, and recipes were not being applied correctly.

## Root Cause
The root cause was found in `RefactorTabComponent.java` within the `ButtonEditor` inner class.

In the `getTableCellEditorComponent` method:
```java
@Override
public Component getTableCellEditorComponent(JTable table, Object value,
        boolean isSelected, int row, int selectedRow) {
    this.selectedRow = selectedRow;
    
    // For undo column, check if there's undo state for this recipe
    if (isUndoColumn) {
        Recipe recipe = tableModel.getRecipeAt(selectedRow);
        // ...
    }
    // ...
}
```

The issue was two-fold:
1. The fifth parameter of `getTableCellEditorComponent` (which represents the column index) was named `selectedRow`, shadowing any intent to use it as a column index and confusing it with the row index.
2. The code was using `selectedRow` (the fifth parameter, i.e., column index) to retrieve the recipe from the `tableModel` using `tableModel.getRecipeAt(selectedRow)`.

Since the "Apply" button is in column 3 and "Undo" is in column 4, `selectedRow` was always 3 or 4.
- `tableModel.getRecipeAt(3)` corresponded to "MigrateREST" or "MigrateSOAP" depending on the list order.
- `tableModel.getRecipeAt(4)` corresponded to "MigrateSOAP".

This meant that no matter which row the user clicked, the code would always think they selected the recipe at index 3 or 4 of the recipe list.

## Fix
The fix involved:
1. Renaming the fifth parameter of `getTableCellEditorComponent` to `column` to reflect its actual meaning in the Swing API.
2. Using the `row` parameter (the fourth parameter) to correctly identify which recipe was selected by the user.
3. Updating the internal `this.selectedRow` state using the correct `row` index.

```java
@Override
public Component getTableCellEditorComponent(JTable table, Object value,
        boolean isSelected, int row, int column) {
    this.selectedRow = row;
    
    // For undo column, check if there's undo state for this recipe
    if (isUndoColumn) {
        Recipe recipe = tableModel.getRecipeAt(row);
        // ...
    }
}
```

## Impact
This bug prevented any recipe other than the ones at indices 3 and 4 from being executed. It also caused significant confusion as the confirmation dialog would show the wrong recipe name. With this fix, recipe selection now works correctly across the entire table.
