# Force-Directed Graph Hybrid Spacing Implementation

## Overview

Implemented a hybrid approach for the force-directed dependency graph layout that combines the simple, proven structure from the original implementation with area-based scaling for dynamic spacing. This fixes spacing issues while avoiding the complexity that broke the layout in the previous "improvements".

## Problem Statement

The force-directed dependency graph had two issues:

1. **Original version (commit b3186b0cc52ca4b51420ef948d5a35614ff066b3)**: Used inverse square root scaling with fixed baseline (OPTIMAL_NODE_COUNT = 50), which didn't account for canvas dimensions, causing spacing to be too large on some screen sizes.

2. **Broken version (commit 5aa8744343f23726e6e58bc4f21ded744f5a9069)**: Attempted to fix spacing with industry-standard techniques (temperature cooling, degree-dependent repulsion, gravity force, energy-based convergence), but these additions broke the graph layout completely.

## Solution: Hybrid Approach

The hybrid approach adopts the area-based scaling formula from the broken version while keeping the simple, proven structure from the original:

### Adopted from Broken Version

**Area-Based Scaling (Fruchterman-Reingold):**
```java
private double calculateScaleFactor(int nodeCount, int canvasWidth, int canvasHeight) {
    double canvasArea = canvasWidth * canvasHeight;
    double optimalDistance = Math.sqrt(canvasArea / nodeCount);
    // Normalize by a base scale (distance for 50 nodes in 1200x800 canvas)
    double baseScale = Math.sqrt((1200.0 * 800.0) / 50.0);
    return optimalDistance / baseScale;
}
```

**Rationale:**
- Directly relates spacing to available canvas space
- Fixes spacing issues where original was too large
- Industry-standard formula from Fruchterman-Reingold algorithm

### Kept from Original Version

**Simple Damping:**
```java
private static final double DAMPING = 0.85;
// In layout loop:
node.setX(node.getX() + fx * DAMPING);
node.setY(node.getY() + fy * DAMPING);
```

**Simple Convergence Detection:**
```java
// In layout loop:
if (maxForce < 0.5) break;
```

**Rationale:**
- Proven to work reliably
- No over-engineering
- Follows KISS principle

### Rejected from Broken Version

- **Temperature cooling** (COOLING_RATE = 0.95, INITIAL_TEMPERATURE_RATIO = 0.1) - Too complex, broke layout
- **Degree-dependent repulsion** - Added complexity for marginal benefit
- **Gravity force** (GRAVITY_STRENGTH = 0.1) - Caused layout instability
- **Energy-based convergence** - Over-engineered, simple threshold works

### Tuned Constants

Reduced base constants for tighter spacing than original:

| Constant | Original | Tuned | Reason |
|----------|----------|-------|--------|
| BASE_NODE_WIDTH | 120 | 100 | Tighter spacing |
| BASE_NODE_HEIGHT | 40 | 35 | Tighter spacing |
| BASE_MIN_SEPARATION | 80 | 70 | Tighter spacing |
| BASE_REPULSION_STRENGTH | 2500 | 2000 | Gentler forces |

## Implementation Details

### File: ForceDirectedLayoutStrategy.java

**Location:** `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/ForceDirectedLayoutStrategy.java`

**Key Changes:**

1. **Constants** - Tuned base values for better spacing
2. **calculateScaleFactor()** - Replaced inverse square root with area-based formula
3. **layout() method** - Simplified to use simple damping and convergence
4. **calculateRepulsiveForce()** - Removed degree-dependent parameters
5. **Removed methods** - calculateNodeDegrees(), calculateGravityForce()

**Layout Algorithm:**
```java
public void layout(List<GraphNode> nodes, List<GraphEdge> edges, int canvasWidth, int canvasHeight) {
    // Calculate scale factor using area-based formula
    double scaleFactor = calculateScaleFactor(nodeCount, canvasWidth, canvasHeight);
    
    // Apply scale factor to all parameters with minimum thresholds
    double minSeparation = Math.max(BASE_MIN_SEPARATION * scaleFactor, MIN_MIN_SEPARATION);
    double nodeWidth = Math.max(BASE_NODE_WIDTH * scaleFactor, MIN_NODE_WIDTH);
    double nodeHeight = Math.max(BASE_NODE_HEIGHT * scaleFactor, MIN_NODE_HEIGHT);
    double repulsionStrength = BASE_REPULSION_STRENGTH * scaleFactor;
    
    // Run force simulation with simple damping
    for (int iter = 0; iter < maxIterations; iter++) {
        // Calculate repulsive forces (no degree dependence)
        // Calculate attractive forces
        // Apply forces with simple damping
        // Stop if maxForce < 0.5
    }
    
    // Center the layout
}
```

## Documentation Updates

### File: ADR-0066

**Location:** `docs/adr/0066-force-directed-graph-calibration-improvements.md`

**Changes:**
- Updated title to "Force-Directed Graph Calibration - Hybrid Approach"
- Documented what was adopted (area-based scaling) and what was rejected (complex features)
- Explained rationale for each decision
- Listed tuned constants and their values

## Testing

**Test File:** `premium-intellij-plugin/src/test/java/adrianmikula/jakartamigration/intellij/ui/ForceDirectedLayoutStrategyTest.java`

**Test Results:** All 5 tests passed successfully
- testNodeSizeDecreasesWithCount
- testMinimumNodeSizesForLargeGraphs
- testNoOverlappingNodes
- testGraphCentering
- testConvergenceForModerateGraphs

**Command:**
```bash
.\gradlew.bat :premium-intellij-plugin:test --tests ForceDirectedLayoutStrategyTest
```

## Benefits

1. **Dynamic spacing** - Adapts to canvas dimensions, fixing original spacing issues
2. **Simplicity** - Maintains KISS principle, no over-engineering
3. **Reliability** - Uses proven structure from original that worked
4. **Maintainability** - Easier to understand and modify than complex version
5. **Performance** - No overhead from unnecessary calculations (degree tracking, energy tracking)

## Trade-offs

1. **Slightly more complex than original** - Area-based formula adds minor complexity
2. **Requires testing across canvas sizes** - Need to verify spacing works on different screen sizes
3. **No degree-dependent repulsion** - Leaf nodes may cluster slightly, but acceptable for dependency graphs

## References

- ADR-0065: Dynamic Graph Spacing and Standard Panning (original approach)
- ADR-0066: Force-Directed Graph Calibration - Hybrid Approach (this implementation)
- Fruchterman-Reingold Algorithm: Graph Drawing via Force-Directed Layouts
- Commit b3186b0cc52ca4b51420ef948d5a35614ff066b3: Working version before break
- Commit 5aa8744343f23726e6e58bc4f21ded744f5a9069: Broken version with complex additions

## Implementation Date

May 14, 2026
