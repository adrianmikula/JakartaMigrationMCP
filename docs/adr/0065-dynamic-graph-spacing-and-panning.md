# Dynamic Node Spacing and Standard Panning for Dependency Graph

## Status

Accepted

## Context

The dependency graph view in the IntelliJ plugin needed two improvements:

1. **Non-standard panning interaction**: Original implementation only allowed panning with middle/right mouse buttons. Left-click did not trigger panning, which is counter-intuitive compared to standard UI patterns (Google Maps, diagram editors, etc.).

2. **Fixed node spacing**: Force-directed layout used hardcoded constants for node size and separation regardless of graph size. This caused poor readability: small graphs looked sparse, large graphs became overcrowded.

Both issues affected usability for graphs ranging from 10 to 1000+ dependencies.

## Decision

### Decision 1: Standard Left-Click Drag Panning

Change panning to use the left mouse button with click-vs-drag discrimination:

- **Single tap** (movement < 5px) → selects node
- **Click and drag** (movement ≥ 5px) → pans view
- **Middle/right drag** → continues to work immediately (backward compatibility)

Implementation in `GraphCanvas.java`:
- Track `pressPoint` on mouse press to measure movement from start
- Calculate squared distance; if > `DRAG_THRESHOLD^2`, start dragging
- `isDragging` flag distinguishes selection click from pan drag
- `leftButtonDown` flag handles `MouseEvent.MOUSE_DRAGGED` where `getButton()` returns `NOBUTTON`

### Decision 2: Auto-Adjusting Node Spacing

Implement dynamic scaling for force-directed layout using inverse square root:

```java
scaleFactor = sqrt(OPTIMAL_NODE_COUNT / actualNodeCount)
```

Base constants optimized for 50 nodes:
- `BASE_MIN_SEPARATION = 80`
- `BASE_NODE_WIDTH = 120`
- `BASE_NODE_HEIGHT = 40`
- `BASE_REPULSION_STRENGTH = 2500`

All scaled by `scaleFactor` with minimum thresholds to ensure readability:
- Minimum width: 60px
- Minimum height: 25px
- Minimum separation: 40px

Examples:
- 10 nodes → scaleFactor ≈ 2.24 (larger, spacious)
- 50 nodes → scaleFactor = 1.0 (optimal)
- 200 nodes → scaleFactor ≈ 0.5 (compact)
- 1000 nodes → scaleFactor ≈ 0.22 (dense)

Implementation in `ForceDirectedLayoutStrategy.java`:
- `calculateScaleFactor(int nodeCount)`
- `calculateMaxIterations(int nodeCount)` — adaptive iterations for large graphs (up to 800)
- Parameters passed to repulsion calculation and node initialization

## Consequences

### Positive

- **Improved usability**: Standard left-click drag is immediately familiar to users
- **Scalability**: Force-directed layout now works gracefully for any graph size
- **Consistent density**: Visual spacing remains proportional regardless of node count
- **Selection clarity**: Small movements click-select, larger movements pan — no accidental selections during drag
- **Backward compatibility**: Middle/right button panning preserved for existing users

### Negative

- **Learning curve for existing users**: Users accustomed to middle-button panning will need to adapt
- **Potential slight overlap at very high node counts (>1000)**: Minimum thresholds prevent complete readability but may still be crowded
- **Performance**: Dynamic iterations help but O(n²) repulsion remains expensive for 1000+ nodes

## Rationale

### Why not left-click + Ctrl to pan?
Adding a modifier would be non-standard for graph panning (Google Maps, diagrams, browsers don't require Ctrl for pan). The threshold approach is cleaner.

### Why inverse square root scaling?
Square root provides diminishing returns that match human perception: spacing reduces quickly at first then levels off, preventing tiny nodes for large graphs while staying readable.

### Why 5px threshold?
Typical mouse jitter; small enough not to interfere with natural drag gestures, large enough to filter accidental movement.

### Why keep non-left button panning?
Some users with mice having tilt wheels or middle-button preferences may already use it; removing would break muscle memory.

## References

- Implementation: `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/`
  - `GraphCanvas.java` (mouse interaction)
  - `ForceDirectedLayoutStrategy.java` (dynamic spacing)
- Tests: `premium-intellij-plugin/src/test/java/adrianmikula/jakartamigration/intellij/ui/`
  - `GraphCanvasInteractionTest.java`
  - `ForceDirectedLayoutStrategyTest.java`
- Spec: `spec/plugin-components.tsp` (GraphLayout.nodeSpacing, GraphInteractionState)
