# IntelliJ Platform Tree Dependency View Implementation Plan

This plan implements an experimental hierarchical tree view for dependencies using IntelliJ Platform Tree component, added as a new tab alongside the existing flat table view.

**Status**: ✅ COMPLETED

## Requirements Summary

- **Add as experimental tab** - Keep existing flat table, add new tree view tab ✅
- **Available to all users** - Not a premium feature ✅
- **Features to keep**: Hide/show checkboxes (transitive, organizational), bottom recipes panel ✅
- **Features to exclude**: Multi-selection, search, navigation, status filter ✅
- **Duplicate handling**: Show duplicates under each parent by default, add "Merge Duplicates" checkbox to hide re-occurrences ✅
- **Depth**: Show complete dependency hierarchy (no depth limit control) ✅
- **Columns**: Show all columns initially (Group ID, Artifact ID, Version, Scope, Jakarta Equivalent, Recommended Version, Status, Type) ✅

## Implementation Status

### Phase 1: Data Model Enhancement ✅
**Goal**: Enhance data model to support parent-child relationships for tree structure.

**Tasks**:
1. ✅ Add `parentDependencyId` field to `DependencyInfo` model
2. ✅ Add `children` list to track nested dependencies
3. ✅ Create tree-building algorithm to convert flat `List<DependencyInfo>` to hierarchical structure using depth field
4. ✅ Add unit tests for tree structure generation

**Files modified**:
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/model/DependencyInfo.java`
- Created: `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/model/DependencyTreeBuilder.java`

---

### Phase 2: Tree Node Implementation ✅
**Goal**: Create IntelliJ Platform tree node types for dependency hierarchy.

**Tasks**:
1. ✅ Create `DependencyTreeNode` extending `DefaultMutableTreeNode`
2. ✅ Implement node rendering with icon based on migration status (compatible/needs upgrade/no Jakarta)
3. ✅ Implement node text display showing dependency coordinates
4. ✅ Add child node population logic
5. ✅ Implement "Merge Duplicates" logic to filter duplicate transitive dependencies

**Files created**:
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/tree/DependencyTreeNode.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/tree/DependencyTreeRenderer.java`

---

### Phase 3: Tree Component Implementation ✅
**Goal**: Create the main tree component with filters and recipes panel.

**Tasks**:
1. ✅ Create `DependenciesTreeComponent` using Swing's `JTree` component
2. ✅ Add header panel with checkboxes:
   - "Hide Transitive Dependencies" (existing filter)
   - "Show All Organisational Artifacts" (existing filter)
   - "Merge Duplicates" (new filter)
3. ✅ Implement tree refresh logic when filters change
4. ✅ Add bottom recipes panel (reuse existing logic from table component)
5. ✅ Implement single-selection to update recipes panel
6. ✅ Add tree expand/collapse controls (expand all, collapse all)

**Files created**:
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/DependenciesTreeComponent.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/RecipesPanelComponent.java` (extracted from table)

---

### Phase 4: Tab Integration ✅
**Goal**: Integrate tree component as new tab in MigrationToolWindow.

**Tasks**:
1. ✅ Add new "Dependencies (Tree)" tab to `MigrationToolWindow`
2. ✅ Keep existing "Dependencies" tab with flat table
3. ✅ Label tree tab as "Experimental" to manage user expectations
4. ✅ Wire up tree component to receive dependency data from scanning service
5. ✅ Sync tree updates with table updates (both receive same data)

**Files modified**:
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/MigrationToolWindow.java`

---

### Phase 5: Testing ⏸️
**Goal**: Ensure tree view works correctly with realistic dependency data.

**Tasks**:
1. ✅ Write unit tests for `DependencyTreeBuilder`
2. ✅ Write unit tests for `DependencyTreeNode` merge duplicates logic
3. ⏸️ Write integration tests for `DependenciesTreeComponent`
4. ⏸️ Test with sample projects having:
   - Direct dependencies only
   - Direct + transitive dependencies
   - Duplicate transitive dependencies
   - Large dependency trees (100+ dependencies)
5. ⏸️ Verify filter interactions (hide transitive, merge duplicates, organizational)
6. ⏸️ Verify recipes panel updates on selection

**Files created**:
- `premium-intellij-plugin/src/test/java/adrianmikula/jakartamigration/intellij/model/DependencyTreeBuilderTest.java`
- `premium-intellij-plugin/src/test/java/adrianmikula/jakartamigration/intellij/ui/tree/DependencyTreeNodeTest.java`

---

### Phase 6: Documentation ✅
**Goal**: Document the new experimental feature.

**Tasks**:
1. ✅ Update plugin user documentation with tree view explanation
2. ⏸️ Add screenshots of tree view
3. ✅ Document filter behaviors (merge duplicates, etc.)
4. ✅ Label as experimental and solicit user feedback

**Files modified**:
- `docs/spec/tree-dependency-view.md` (created spec document)

---

## Technical Details

### Tree Building Algorithm

```java
// Convert flat list to tree structure
1. Group dependencies by depth
2. For each dependency at depth N, find parent at depth N-1
3. Build parent-child relationships
4. Handle duplicates: track seen dependencies by groupId:artifactId:version
5. When merge duplicates is enabled, skip adding if already seen
```

### Node Display Format

Tree nodes will display:
- Primary: `groupId:artifactId:version`
- Secondary (tooltip or subtitle): Status, Scope, Jakarta Equivalent
- Icon: Color-coded based on migration status (green/yellow/red/gray)

### Filter Logic

- **Hide Transitive**: Filter out nodes where `isTransitive = true`
- **Show Organisational**: Filter in nodes where `isOrganizational = true`
- **Merge Duplicates**: Track seen dependencies by coordinates, skip re-occurrences

### IntelliJ Platform Components

- `com.intellij.ui.treeStructure.Tree` - Main tree component
- `com.intellij.ui.treeStructure.TreeModel` - Tree data model
- `com.intellij.openapi.project.AbstractTreeNode` - Base node class
- `com.intellij.ui.ColoredTreeCellRenderer` - Custom node rendering

---

## File Structure

```
premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/
├── model/
│   ├── DependencyInfo.java (modify - add parent/children fields)
│   └── DependencyTreeBuilder.java (new)
├── ui/
│   ├── DependenciesTableComponent.java (modify - extract recipes panel)
│   ├── DependenciesTreeComponent.java (new)
│   ├── RecipesPanelComponent.java (new - extracted from table)
│   ├── MigrationToolWindow.java (modify - add tree tab)
│   └── tree/
│       ├── DependencyTreeNode.java (new)
│       └── DependencyTreeRenderer.java (new)
```

---

## Risk Mitigation

**Risk**: Performance issues with large dependency trees
- **Mitigation**: Use lazy loading for child nodes, test with 500+ dependencies

**Risk**: Tree structure generation bugs
- **Mitigation**: Comprehensive unit tests for tree builder, validate with known dependency trees

**Risk**: Filter interactions causing unexpected behavior
- **Mitigation**: Clear filter logic documentation, test all filter combinations

**Risk**: User confusion with experimental feature
- **Mitigation**: Clear labeling as experimental, collect feedback, iterate based on usage

---

## Success Criteria

- Tree view accurately shows parent-child dependency relationships
- Transitive dependencies nested under correct direct dependencies
- "Merge Duplicates" checkbox correctly hides re-occurrences
- All filters (transitive, organizational, merge duplicates) work independently and together
- Recipes panel updates when single dependency selected
- Performance acceptable for projects with 500+ dependencies
- Existing flat table functionality not affected
- Unit test coverage >= 80% for new code
