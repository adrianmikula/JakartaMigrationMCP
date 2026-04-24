# Integration Test for Advanced Scans Tab Fix

## Test Steps

1. **Build the project** - The core fix should compile successfully despite unrelated gauge component issues
2. **Run the plugin** - Start IntelliJ with the plugin
3. **Test the flow**:
   - Open a project with some javax.* imports
   - Go to Dashboard tab
   - Click "Analyze Project" 
   - Wait for scan to complete
   - Switch to "Advanced Scans" tab
   - Verify that scan results are displayed (not "Not scanned yet")

## Expected Behavior

- **Before fix**: Advanced Scans tab shows "Not scanned yet" even after dashboard scan completes
- **After fix**: Advanced Scans tab shows scan results with proper truncation (first 10 rows for non-premium users)

## Key Fix Components

1. **AdvancedScansComponent.refreshFromCachedResults()** - New method that loads and displays results from service cache
2. **MigrationToolWindow scan completion** - Updated to call refresh method after advanced scans complete
3. **Truncation logic** - Already properly implemented via TruncationHelper (shows first 10 rows for non-premium)

## Verification

The fix ensures that when dashboard runs scans, the AdvancedScansComponent automatically refreshes to display the new results, maintaining the existing truncation behavior for free users.
