# ADR 0066: Force-Directed Graph Calibration - Hybrid Approach

## Status
Accepted (Superseded - Hybrid Approach)

## Context
The force-directed dependency tree layout in the IntelliJ plugin was experiencing calibration issues where node distances oscillated between extremes:
- Initially, distances were too small causing nodes to overlap
- After tuning, distances became too large spreading nodes too far apart

The previous implementation (ADR-0065) used inverse square root scaling: `scaleFactor = sqrt(OPTIMAL_NODE_COUNT / nodeCount)` with a fixed baseline of 50 nodes. This approach had limitations:
- Fixed baseline didn't account for actual canvas dimensions
- Spacing was too large on some screen sizes

## Initial Decision (Rolled Back)
We initially implemented a multi-faceted calibration approach based on industry-standard techniques from D3.js, ForceAtlas2 (Gephi), and Fruchterman-Reingold algorithms. This implementation broke the graph layout completely.

### What We Adopted (Kept)
**Area-Based Scaling (Fruchterman-Reingold):**
- Replaced inverse square root with: `scaleFactor = sqrt(canvasArea / nodeCount) / BASE_SCALE`
- Directly relates spacing to available canvas space
- More intuitive and predictable than fixed baseline

### What We Rejected (Removed)
**Adaptive Temperature Cooling (Simulated Annealing/D3.js):**
- Initial implementation broke the layout
- Too complex for the use case
- Simple damping (0.85) is sufficient

**Degree-Dependent Repulsion (ForceAtlas2):**
- Added complexity for marginal benefit
- Not needed for dependency graphs
- Removed to keep implementation simple

**Gravity Force (ForceAtlas2):**
- Caused layout instability
- Disconnected components don't drift in practice
- Removed to prevent errors

**Improved Convergence Detection:**
- Energy-based tracking was over-engineered
- Simple max-force threshold (0.5) works reliably
- Removed to reduce complexity

## Revised Decision (Hybrid Approach)
We implemented a hybrid approach that combines the simple, proven structure from the original with area-based scaling for dynamic spacing:

### 1. Area-Based Scaling (Adopted)
- Formula: `scaleFactor = sqrt(canvasArea / nodeCount) / BASE_SCALE`
- Normalizes by base scale (distance for 50 nodes in 1200x800 canvas)
- Accounts for canvas dimensions to fix spacing issues

### 2. Simple Damping (Kept from Original)
- Fixed damping factor (0.85) for stable convergence
- No temperature cooling complexity
- Proven to work reliably

### 3. Simple Convergence (Kept from Original)
- Stops when maxForce < 0.5
- No energy-based tracking
- No minimum iteration thresholds

### 4. Tuned Constants (Improved)
- BASE_NODE_WIDTH: 100 (reduced from 120 for tighter spacing)
- BASE_NODE_HEIGHT: 35 (reduced from 40 for tighter spacing)
- BASE_MIN_SEPARATION: 70 (reduced from 80 for tighter spacing)
- BASE_REPULSION_STRENGTH: 2000 (reduced from 2500 for gentler forces)

## Consequences
- **Positive**: Node spacing now dynamically adapts to canvas dimensions
- **Positive**: Fixes spacing issues where original was too large
- **Positive**: Simple, proven structure from original prevents errors
- **Positive**: KISS principle maintained - no over-engineering
- **Positive**: Constants tuned for better visual density
- **Neutral**: Slightly more complex than original due to area-based formula
- **Neutral**: Requires testing across different canvas sizes

## References
- ADR-0065: Dynamic Graph Spacing and Standard Panning (original approach)
- Fruchterman-Reingold Algorithm: Graph Drawing via Force-Directed Layouts
- ForceAtlas2 Paper: https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0098679 (rejected for complexity)
- D3.js Force Simulation: https://d3js.org/d3-force (rejected for complexity)
