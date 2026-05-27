# ADR 0067: Force-Directed Graph Rollback Decision

## Status
Accepted

## Context
The force-directed dependency graph layout in the IntelliJ plugin underwent two failed attempts to improve node spacing calibration:

1. **First Attempt (Commit 5aa8744)**: Implemented industry-standard techniques from D3.js, ForceAtlas2, and Fruchterman-Reingold algorithms including:
   - Area-based scaling
   - Adaptive temperature cooling (simulated annealing)
   - Degree-dependent repulsion
   - Gravity force
   - Energy-based convergence detection
   
   **Result**: Completely broke the graph layout - nodes were not positioned correctly.

2. **Second Attempt (Hybrid Approach)**: Tried to combine the simple structure from the original with area-based scaling:
   - Kept: simple damping (0.85), simple convergence (maxForce < 0.5), area-based scaling
   - Rejected: temperature cooling, degree-dependent repulsion, gravity force, energy-based convergence
   - Tuned constants for tighter spacing (reduced base values)
   
   **Result**: Still did not work correctly - the graph layout remained broken.

The original working version (commit b3186b0c, ADR-0065) used:
- Inverse square root scaling: `scaleFactor = sqrt(OPTIMAL_NODE_COUNT / nodeCount)` where OPTIMAL_NODE_COUNT = 50
- BASE_REPULSION_STRENGTH = 2500
- BASE_NODE_WIDTH = 120
- BASE_NODE_HEIGHT = 40
- BASE_MIN_SEPARATION = 80
- Simple damping (0.85)
- Simple convergence (maxForce < 0.5)

The original had a known limitation: spacing could be too large on some screen sizes, but the layout algorithm worked correctly.

## Decision
After two failed attempts to improve spacing, we decided to completely revert to the original working version (commit b3186b0cc52ca4b51420ef948d5a35614ff066b3) documented in ADR-0065.

**Rationale:**
- The original version was working correctly and produced valid graph layouts
- Spacing issues (too large on some screens) are minor compared to a completely broken layout
- Attempting to fix spacing broke the fundamental layout algorithm in both attempts
- We can address spacing separately in the future with more careful testing and incremental changes
- The complexity introduced by the "improvements" made debugging difficult

## Actions Taken

1. **Reverted ForceDirectedLayoutStrategy.java** to commit b3186b0c
   - File: `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/ForceDirectedLayoutStrategy.java`
   - Restored inverse square root scaling
   - Restored original constants (2500, 120, 40, 80)
   - Removed area-based scaling, tuned constants, and hybrid approach documentation

2. **Updated ADR-0066** to document the rollback
   - Changed status to "Rejected - Rolled Back to Original (ADR-0065)"
   - Documented both failed attempts
   - Explained rationale for rollback

3. **Deleted implementation documentation** for the failed hybrid approach
   - File: `docs/implementation/force-directed-graph-hybrid-spacing.md`
   - Removed to prevent confusion

4. **Verified tests pass**
   - Command: `.\gradlew.bat :premium-intellij-plugin:test --tests "adrianmikula.jakartamigration.intellij.ui.ForceDirectedLayoutStrategyTest"`
   - Result: BUILD SUCCESSFUL

## Consequences

### Positive
- Graph layout returns to working state
- Simple, proven implementation that is easy to understand and maintain
- No risk of breaking the layout further
- Tests pass successfully
- Follows KISS principle

### Negative
- Spacing may still be too large on some screen sizes (known limitation from ADR-0065)
- Lost opportunity to improve spacing with area-based scaling

### Neutral
- Future work can address spacing with more careful testing and incremental changes
- Any future improvements should be tested more thoroughly before committing
- Consider A/B testing or feature flags for layout algorithm changes

## Lessons Learned

1. **Complexity is the enemy of reliability**: Adding industry-standard techniques (temperature cooling, degree-dependent repulsion, gravity) made the system too complex and broke the layout.

2. **Incremental changes are safer**: Instead of changing multiple aspects at once (scaling formula + constants + convergence detection), changes should be made one at a time with testing after each change.

3. **Working is better than perfect**: A working implementation with minor spacing issues is preferable to a "perfect" implementation that doesn't work at all.

4. **Test thoroughly before committing**: The hybrid approach should have been tested more thoroughly before being committed, especially since the first attempt had already failed.

5. **Document failures clearly**: ADR-0066 now documents both failed attempts to prevent future developers from repeating the same mistakes.

## Future Considerations

If spacing issues need to be addressed in the future:
1. Start with the working version (ADR-0065) as the baseline
2. Make one small change at a time (e.g., only adjust constants, only change scaling formula)
3. Test thoroughly after each change
4. Use A/B testing or feature flags to compare layouts
5. Consider user feedback on spacing rather than assuming it needs fixing
6. Keep changes simple and reversible

## References
- ADR-0065: Dynamic Graph Spacing and Standard Panning (original working approach)
- ADR-0066: Force-Directed Graph Calibration - Rollback to Original (documents failed attempts)
- Commit b3186b0cc52ca4b51420ef948d5a35614ff066b3: Working version (reverted to this)
- Commit 5aa8744343f23726e6e58bc4f21ded744f5a9069: First failed attempt (industry-standard techniques)
