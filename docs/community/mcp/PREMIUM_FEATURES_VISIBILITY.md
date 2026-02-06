# Premium Features Visibility Improvements

## Problem

The MCP tools were not making users aware of premium/paid tier capabilities, even when those features would be highly beneficial for their migration projects.

## Solution

Added **contextual premium feature recommendations** to all analysis tool responses when relevant.

## Changes Made

### 1. `detectBlockers` Tool
**When**: Blockers are detected  
**Recommendation**: Suggests Auto-Fixes, Advanced Analysis, and Binary Fixes

```json
{
  "premiumFeatures": {
    "recommended": true,
    "message": "Premium Auto-Fixes can automatically resolve many blockers without manual intervention.",
    "features": [
      "Auto-Fixes - Automatically fix detected blockers",
      "Advanced Analysis - Deep transitive conflict detection and resolution",
      "Binary Fixes - Fix issues in compiled binaries and JAR files"
    ],
    "pricingUrl": "https://apify.com/adrian_m/jakartamigrationmcp#pricing"
  }
}
```

### 2. `createMigrationPlan` Tool
**When**: Migration plan requires >30 minutes, >5 phases, or has risk score >0.3  
**Recommendation**: Suggests One-Click Refactor, Auto-Fixes, and Batch Operations

```json
{
  "premiumFeatures": {
    "recommended": true,
    "message": "Premium One-Click Refactor can execute this entire migration plan automatically, saving you X minutes of manual work.",
    "features": [
      "One-Click Refactor - Execute complete migration automatically",
      "Auto-Fixes - Automatically resolve blockers and issues",
      "Batch Operations - Process multiple projects simultaneously"
    ],
    "pricingUrl": "https://apify.com/adrian_m/jakartamigrationmcp#pricing",
    "estimatedSavings": "X minutes of manual work"
  }
}
```

### 3. `analyzeMigrationImpact` Tool
**When**: High complexity, >60 minutes effort, >3 blockers, or >20 files to migrate  
**Recommendation**: Suggests One-Click Refactor, Auto-Fixes, Advanced Analysis, and Batch Operations

```json
{
  "premiumFeatures": {
    "recommended": true,
    "message": "This migration has HIGH complexity and will take approximately X minutes. Premium features can automate most of this work.",
    "features": [
      "One-Click Refactor - Execute complete migration automatically",
      "Auto-Fixes - Automatically resolve X blockers",
      "Advanced Analysis - Deep transitive conflict detection",
      "Batch Operations - Process multiple projects simultaneously"
    ],
    "pricingUrl": "https://apify.com/adrian_m/jakartamigrationmcp#pricing",
    "estimatedSavings": "X minutes of manual work"
  }
}
```

### 4. `recommendVersions` Tool
**When**: >5 recommendations or breaking changes detected  
**Recommendation**: Suggests Auto-Fixes, Advanced Analysis, and Custom Recipes

```json
{
  "premiumFeatures": {
    "recommended": true,
    "message": "Premium Auto-Fixes can automatically apply these X version recommendations and handle breaking changes.",
    "features": [
      "Auto-Fixes - Automatically apply version recommendations",
      "Advanced Analysis - Handle breaking changes automatically",
      "Custom Recipes - Create and use custom migration recipes"
    ],
    "pricingUrl": "https://apify.com/adrian_m/jakartamigrationmcp#pricing"
  }
}
```

## Benefits

1. **Contextual Recommendations**: Premium features are suggested only when they would actually help
2. **Clear Value Proposition**: Shows estimated time savings and specific benefits
3. **Non-Intrusive**: Only appears when relevant, doesn't clutter simple analyses
4. **Actionable**: Includes direct link to pricing page
5. **Transparent**: Clearly explains what premium features can do

## User Experience Flow

### Before
1. User runs `detectBlockers` → Gets list of blockers
2. User runs `createMigrationPlan` → Gets migration plan
3. **No mention of premium features that could help**

### After
1. User runs `detectBlockers` → Gets list of blockers + **premium feature recommendation**
2. User runs `createMigrationPlan` → Gets migration plan + **premium feature recommendation with time savings**
3. User is aware of premium options and can make informed decision

## Example Response

When analyzing a complex project:

```json
{
  "status": "success",
  "totalBlockers": 7,
  "blockers": [...],
  "premiumFeatures": {
    "recommended": true,
    "message": "Premium Auto-Fixes can automatically resolve many blockers without manual intervention.",
    "features": [
      "Auto-Fixes - Automatically fix detected blockers",
      "Advanced Analysis - Deep transitive conflict detection and resolution",
      "Binary Fixes - Fix issues in compiled binaries and JAR files"
    ],
    "pricingUrl": "https://apify.com/adrian_m/jakartamigrationmcp#pricing"
  }
}
```

## Future Enhancements

1. **Feature Flags**: Check user's current tier and only show premium features if they're on free tier
2. **Trial Offers**: Suggest free trial for premium features
3. **Usage-Based Recommendations**: Track user's migration patterns and suggest premium features based on their specific needs
4. **Comparison Table**: Show side-by-side comparison of free vs premium capabilities

## Testing

To verify premium feature recommendations appear:

1. Run `detectBlockers` on a project with blockers → Should see premium features section
2. Run `createMigrationPlan` on a complex project → Should see premium features with time savings
3. Run `analyzeMigrationImpact` on a high-complexity project → Should see premium features recommendation
4. Run `recommendVersions` on a project with many dependencies → Should see premium features if >5 recommendations

