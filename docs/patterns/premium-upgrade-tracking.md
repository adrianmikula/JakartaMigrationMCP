# Premium Upgrade Tracking Pattern

This document defines the correct pattern for implementing premium upgrade UI interactions to ensure proper usage event tracking to Supabase.

## Overview

All premium upgrade interactions must track usage events to understand conversion funnels and user behavior. This ensures we have complete analytics on upgrade triggers and conversion rates.

## Correct Implementation Pattern

### 1. Use PremiumUpgradeButton Factory Methods

Always use the factory methods in `PremiumUpgradeButton` class:

```java
// Standard upgrade button with default text
JButton upgradeButton = PremiumUpgradeButton.createUpgradeButton(project, "current_ui_tab");

// Custom upgrade button with specific text and tooltip
JButton upgradeButton = PremiumUpgradeButton.createUpgradeButton(
    project, 
    "Custom Upgrade Text", 
    "Custom Tooltip", 
    "current_ui_tab"
);

// Upgrade button with custom analytics source
JButton upgradeButton = PremiumUpgradeButton.createUpgradeButton(
    project, 
    "analytics_source_identifier",
    "Button Text", 
    "Tooltip Text", 
    "current_ui_tab"
);
```

### 2. Analytics Tracking

The `PremiumUpgradeButton.createUpgradeButton()` methods automatically:
- Call `UsageService.trackUpgradeClick(source, currentUiTab)`
- Send events to Supabase via batch processing
- Include proper context (source identifier + UI tab)
- Handle errors gracefully without blocking upgrade action

### 3. Source Identifier Guidelines

Use descriptive source identifiers:
- `migration_tool_window_placeholder` - Main tool window placeholder
- `migration_tool_window_toolbar` - Main tool window toolbar
- `ai_tab` - AI/MCP server tab
- `reports_tab` - Reports tab
- `truncation_notice` - Truncation notice panel
- `advanced_scans_credit_exhausted` - Advanced scans credit exhaustion
- `history_credit_exhausted` - History tab credit exhaustion
- `refactor_credit_exhausted` - Refactoring credit exhaustion
- `refactor_undo_credit_exhausted` - Refactoring undo credit exhaustion
- `credits_progress_bar` - Credits progress bar upgrade link
- `license_expiration_notifier` - License expiration notification

## Anti-Patterns to Avoid

### ❌ Direct openMarketplace() Calls

Never call `openMarketplace()` directly without analytics tracking:

```java
// WRONG - No analytics tracking
private void openMarketplace() {
    try {
        Desktop.getDesktop().browse(new URI("https://plugins.jetbrains.com/plugin/30093-jakarta-migration"));
    } catch (Exception ex) {
        // error handling
    }
}
```

### ❌ Manual Upgrade Buttons

Don't create manual upgrade buttons without analytics:

```java
// WRONG - No analytics tracking
JButton upgradeButton = new JButton("Upgrade to Premium");
upgradeButton.addActionListener(e -> openMarketplace());
```

### ❌ Dialogs Without Analytics

Don't show upgrade dialogs that bypass analytics:

```java
// WRONG - No analytics tracking
int result = Messages.showYesNoDialog(project,
    "You've used all your action credits. Upgrade to Premium to continue.",
    "Credits Exhausted",
    "Upgrade to Premium",
    "Cancel",
    Messages.getWarningIcon());
if (result == Messages.YES) {
    openMarketplace(); // No tracking!
}
```

## Correct Dialog Implementation

When showing upgrade dialogs, always include analytics tracking:

```java
// CORRECT - With analytics tracking
int result = Messages.showYesNoDialog(project,
    "You've used all your action credits. Upgrade to Premium to continue.",
    "Credits Exhausted",
    "Upgrade to Premium",
    "Cancel",
    Messages.getWarningIcon());
if (result == Messages.YES) {
    // Track the upgrade click before opening marketplace
    try {
        UserIdentificationService userIdentificationService = new UserIdentificationService();
        UsageService usageService = new UsageService(userIdentificationService);
        usageService.trackUpgradeClick("dialog_source_identifier", "current_ui_tab");
    } catch (Exception e) {
        // Log error but don't prevent upgrade
        System.err.println("Failed to track upgrade click analytics: " + e.getMessage());
    }
    openMarketplace();
}
```

## Migration Strategy

For existing violations:
1. **Preserve User Experience**: Keep existing dialogs and messaging
2. **Add Analytics**: Insert tracking before `openMarketplace()` calls
3. **Use Consistent Sources**: Apply standardized source identifiers
4. **Maintain Context**: Include current UI tab for proper analytics

## Verification

All upgrade interactions should:
- ✅ Call `UsageService.trackUpgradeClick()` with source and UI tab
- ✅ Send events to Supabase `/rest/v1/usage_events` endpoint
- ✅ Include proper error handling that doesn't block upgrade
- ✅ Use descriptive source identifiers for analytics analysis

## Testing

Verify tracking works by:
1. Clicking upgrade interactions in different UI contexts
2. Checking Supabase for received events
3. Confirming source identifiers and UI tabs are correctly logged
4. Testing error scenarios don't prevent upgrade functionality
