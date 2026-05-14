# ADR 0066: Force-Directed Graph Calibration Improvements

## Status
Accepted

## Context
The force-directed dependency tree layout in the IntelliJ plugin was experiencing calibration issues where node distances oscillated between extremes:
- Initially, distances were too small causing nodes to overlap
- After tuning, distances became too large spreading nodes too far apart

The previous implementation (ADR-0065) used inverse square root scaling: `scaleFactor = sqrt(OPTIMAL_NODE_COUNT / nodeCount)` with a fixed baseline of 50 nodes. This approach had limitations:
- Fixed baseline didn't account for actual canvas dimensions
- No adaptive mechanisms for different graph characteristics
- Simple damping (0.85) lacked temperature-based convergence control
- No consideration of node degree distribution (important for power-law networks)
- Disconnected components could drift to canvas edges

## Decision
We implemented a multi-faceted calibration approach based on industry-standard techniques from D3.js, ForceAtlas2 (Gephi), and Fruchterman-Reingold algorithms:

### 1. Area-Based Scaling (Fruchterman-Reingold)
Replaced inverse square root with: `optimalDistance = sqrt(canvasArea / nodeCount)`

**Rationale:**
- Directly relates spacing to available canvas space
- More intuitive and predictable than fixed baseline
- Industry-standard formula used in FR algorithm
- Eliminates arbitrary "optimal node count" constant

### 2. Adaptive Temperature Cooling (Simulated Annealing/D3.js)
Implemented temperature-based displacement limiting:
- Initial temperature = 10% of canvas width
- Decays by 5% per iteration (COOLING_RATE = 0.95)
- Limits maximum displacement per iteration based on current temperature

**Rationale:**
- Allows large movements early for exploration
- Smaller movements later for refinement
- Prevents premature convergence to local minima
- Matches D3.js alpha/temperature cooling approach

### 3. Degree-Dependent Repulsion (ForceAtlas2)
Modified repulsion to scale with node degree: `repulsion * sqrt((degree1 + 1) * (degree2 + 1))`

**Rationale:**
- Highly connected nodes repel more strongly
- Reduces visual clutter from leaf nodes in power-law networks
- Particularly effective for dependency graphs (often scale-free)
- Proven technique in Gephi's ForceAtlas2 algorithm

### 4. Gravity Force (ForceAtlas2)
Added center-attracting force with strength 0.1

**Rationale:**
- Prevents disconnected components from drifting to edges
- Keeps sparse graphs cohesive
- Particularly important for graphs with multiple disconnected clusters
- Standard feature in production force-directed layouts

### 5. Improved Convergence Detection
Replaced simple max-force threshold with energy-based tracking:
- Tracks total system energy over iterations
- Minimum 50 iterations before checking convergence
- Stops when relative energy change < 1% or temperature < 0.5

**Rationale:**
- Prevents premature stopping during exploration phase
- Avoids unnecessary iterations when converged
- More robust than fixed force threshold
- Matches simulated annealing best practices

### 6. Simplified Parameter Structure
Consolidated from multiple hardcoded constants to single optimal distance parameter

**Rationale:**
- Easier to tune (one parameter vs many)
- More predictable behavior
- Aligns with ForceAtlas2's single scaling parameter approach
- Reduces maintenance burden

## Consequences
- **Positive**: Node spacing now dynamically adapts to both canvas size and node count
- **Positive**: Eliminates oscillation between too-large and too-small distances
- **Positive**: Better handling of power-law networks (common in dependency graphs)
- **Positive**: Disconnected components stay cohesive and centered
- **Positive**: Faster convergence with fewer unnecessary iterations
- **Positive**: Industry-standard techniques make behavior more predictable
- **Neutral**: Slightly more complex implementation due to multiple calibration techniques
- **Neutral**: New constants (COOLING_RATE, GRAVITY_STRENGTH) require tuning for edge cases

## References
- ADR-0065: Dynamic Graph Spacing and Standard Panning (previous approach)
- ForceAtlas2 Paper: https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0098679
- D3.js Force Simulation: https://d3js.org/d3-force
- Fruchterman-Reingold Algorithm: Graph Drawing via Force-Directed Layouts
