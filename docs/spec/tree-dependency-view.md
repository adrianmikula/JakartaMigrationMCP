# Tree Dependency View Specification

## Overview
This specification defines the experimental hierarchical tree view for dependencies using IntelliJ Platform Tree component, added as a new tab alongside the existing flat table view.

## Requirements

### Functional Requirements
- **TR-001**: Add as experimental tab - Keep existing flat table, add new tree view tab
- **TR-002**: Available to all users - Not a premium feature
- **TR-003**: Keep existing filters: Hide/show checkboxes (transitive, organizational), bottom recipes panel
- **TR-004**: Exclude features: Multi-selection, search, navigation, status filter
- **TR-005**: Duplicate handling: Show duplicates under each parent by default, add "Merge Duplicates" checkbox to hide re-occurrences
- **TR-006**: Depth: Show complete dependency hierarchy (no depth limit control)
- **TR-007**: Columns: Show all columns initially (Group ID, Artifact ID, Version, Scope, Jakarta Equivalent, Recommended Version, Status, Type)

### Non-Functional Requirements
- **NFR-001**: Performance acceptable for projects with 500+ dependencies
- **NFR-002**: Unit test coverage >= 80% for new code
- **NFR-003**: Existing flat table functionality not affected

## Data Model

### DependencyInfo Enhancements
```java
public class DependencyInfo {
    // Existing fields...
    
    // New fields for tree structure
    private String parentDependencyId;  // ID of parent dependency
    private List<DependencyInfo> children;  // Nested dependencies
}
```

### Tree Building Algorithm
1. Group dependencies by depth
2. For each dependency at depth N, find parent at depth N-1
3. Build parent-child relationships
4. Handle duplicates: track seen dependencies by groupId:artifactId:version
5. When merge duplicates is enabled, skip adding if already seen

## UI Components

### DependencyTreeNode
- Extends `AbstractTreeNode<DependencyInfo>`
- Implements node rendering with icon based on migration status
- Implements node text display showing dependency coordinates
- Implements child node population logic
- Implements "Merge Duplicates" logic to filter duplicate transitive dependencies

### DependencyTreeRenderer
- Implements custom cell rendering for tree nodes
- Color-coded icons based on migration status (green/yellow/red/gray)
- Display format: `groupId:artifactId:version`
- Secondary info: Status, Scope, Jakarta Equivalent

### DependenciesTreeComponent
- Uses IntelliJ's `Tree` component
- Header panel with checkboxes:
  - "Hide Transitive Dependencies" (existing filter)
  - "Show All Organisational Artifacts" (existing filter)
  - "Merge Duplicates" (new filter)
- Tree refresh logic when filters change
- Bottom recipes panel (reuse existing logic from table component)
- Single-selection to update recipes panel
- Tree expand/collapse controls (expand all, collapse all)

### RecipesPanelComponent (Extracted)
- Reusable component extracted from DependenciesTableComponent
- Shows refactoring recipes for selected dependency
- Apply recipe functionality

## Filter Logic

- **Hide Transitive**: Filter out nodes where `isTransitive = true`
- **Show Organisational**: Filter in nodes where `isOrganizational = true`
- **Merge Duplicates**: Track seen dependencies by coordinates, skip re-occurrences

## Success Criteria

- Tree view accurately shows parent-child dependency relationships
- Transitive dependencies nested under correct direct dependencies
- "Merge Duplicates" checkbox correctly hides re-occurrences
- All filters (transitive, organizational, merge duplicates) work independently and together
- Recipes panel updates when single dependency selected
- Performance acceptable for projects with 500+ dependencies
- Existing flat table functionality not affected
- Unit test coverage >= 80% for new code
