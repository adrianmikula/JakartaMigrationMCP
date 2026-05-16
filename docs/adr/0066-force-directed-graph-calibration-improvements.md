# ADR 0066: Force-Directed Graph Calibration - Rollback to Original

## Status
Rejected - Rolled Back to Original (ADR-0065)

## Context
The force-directed dependency tree layout in the IntelliJ plugin was experiencing calibration issues where node distances oscillated between extremes:
- Initially, distances were too small causing nodes to overlap
- After tuning, distances became too large spreading nodes too far apart

The previous implementation (ADR-0065) used inverse square root scaling: `scaleFactor = sqrt(OPTIMAL_NODE_COUNT / nodeCount)` with a fixed baseline of 50 nodes. This approach had limitations:
- Fixed baseline didn't account for actual canvas dimensions
- Spacing was too large on some screen sizes

## Attempted Improvements (All Rejected)

### First Attempt: Industry-Standard Techniques (Commit 5aa8744)
We initially implemented a multi-faceted calibration approach based on industry-standard techniques from D3.js, ForceAtlas2 (Gephi), and Fruchterman-Reingold algorithms. This implementation broke the graph layout completely.

**Features Added:**
- Area-based scaling (Fruchterman-Reingold)
- Adaptive temperature cooling (simulated annealing)
- Degree-dependent repulsion (ForceAtlas2)
- Gravity force (ForceAtlas2)
- Energy-based convergence detection

**Result:** Completely broke the graph layout - nodes were not positioned correctly.

### Second Attempt: Hybrid Approach (Recent)
We attempted a hybrid approach that combined the simple, proven structure from the original with area-based scaling for dynamic spacing.

**Features Kept:**
- Simple damping (0.85)
- Simple convergence detection (maxForce < 0.5)
- Area-based scaling formula

**Features Rejected:**
- Temperature cooling
- Degree-dependent repulsion
- Gravity force
- Energy-based convergence

**Result:** Still did not work correctly - the graph layout remained broken.

## Final Decision: Complete Rollback
After two failed attempts to improve the layout, we decided to completely revert to the original working version (commit b3186b0cc52ca4b51420ef948d5a35614ff066b3).

**Rationale:**
- The original version (ADR-0065) was working correctly
- Spacing issues (too large on some screens) are minor compared to a completely broken layout
- We can address spacing separately in the future if needed
- Attempting to fix spacing broke the fundamental layout algorithm

**Reverted to Original (ADR-0065):**
- Inverse square root scaling: `scaleFactor = sqrt(OPTIMAL_NODE_COUNT / nodeCount)`
- BASE_REPULSION_STRENGTH = 2500
- BASE_NODE_WIDTH = 120
- BASE_NODE_HEIGHT = 40
- BASE_MIN_SEPARATION = 80
- Simple damping (0.85)
- Simple convergence (maxForce < 0.5)
- No temperature cooling, degree-dependent repulsion, or gravity force

## Consequences
- **Positive**: Graph layout returns to working state
- **Positive**: Simple, proven implementation
- **Positive**: No risk of breaking the layout further
- **Negative**: Spacing may still be too large on some screen sizes (known issue)
- **Neutral**: Future work can address spacing with more careful testing

## References
- ADR-0065: Dynamic Graph Spacing and Standard Panning (original working approach)
- Commit b3186b0cc52ca4b51420ef948d5a35614ff066b3: Working version (reverted to this)
- Commit 5aa8744343f23726e6e58bc4f21ded744f5a9069: First failed attempt (industry-standard techniques)
