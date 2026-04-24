# Dev Mode Integration with Gradle Tasks

This document explains how to use the development mode features that have been integrated with the Gradle build system.

## Overview

The Jakarta Migration plugin now includes development-specific features that are only available when running in dev mode. These include:

- **Dev Tab**: A development-only settings tab with premium license simulation
- **Premium Simulation**: Ability to simulate premium features without requiring a license
- **Enhanced Debugging**: Additional logging and development tools

## Gradle Tasks

### `runIdeDev`
Runs the IDE in development mode with the Dev tab available.

```bash
./gradlew :premium-intellij-plugin:runIdeDev
```

**Features:**
- Sets `jakarta.migration.mode=dev` system property
- Dev tab appears as the first tab in the tool window
- Premium simulation can be enabled via the Dev tab checkbox
- All licensing checks are bypassed

### `runIdeDevPremium`
Runs the IDE in development mode with premium simulation enabled by default.

```bash
./gradlew :premium-intellij-plugin:runIdeDevPremium
```

**Features:**
- Sets `jakarta.migration.mode=dev` system property
- Sets `jakarta.migration.dev.simulate_premium=true` system property
- Dev tab appears with premium simulation already enabled
- All premium features are immediately available

### `runIdeDemo`
Runs the IDE in demo marketplace mode.

```bash
./gradlew :premium-intellij-plugin:runIdeDemo
```

**Features:**
- Sets `jakarta.migration.mode=demo` system property
- Uses JetBrains Demo Marketplace
- Dev tab is NOT available
- Standard licensing behavior applies

### `runIdeProd`
Runs the IDE in production marketplace mode.

```bash
./gradlew :premium-intellij-plugin:runIdeProd
```

**Features:**
- Sets `jakarta.migration.mode=production` system property
- Uses JetBrains Production Marketplace
- Dev tab is NOT available
- Standard licensing behavior applies

## Dev Tab Features

When running in dev mode (`runIdeDev` or `runIdeDevPremium`), a "Dev" tab appears in the Jakarta Migration tool window with the following features:

### Premium Simulation Checkbox
- **Location**: Dev tab (first tab)
- **Function**: Simulates having a premium license
- **Effect**: 
  - All premium features become available
  - UI shows "Premium (Simulated)" status
  - Credit limits are bypassed
  - All tabs show premium versions

### Settings Persistence
- Premium simulation state persists during the IDE session
- Settings are reset when the IDE restarts (for safety)
- Settings are stored in system properties

## Development Workflow

### 1. Standard Development
```bash
./gradlew :premium-intellij-plugin:runIdeDev
```
- Use when you want to test both free and premium modes
- Toggle premium simulation via the Dev tab as needed
- Best for testing UI transitions between modes

### 2. Premium Feature Development
```bash
./gradlew :premium-intellij-plugin:runIdeDevPremium
```
- Use when developing premium features exclusively
- Premium simulation enabled from the start
- Best for premium feature testing and debugging

### 3. Production Testing
```bash
./gradlew :premium-intellij-plugin:runIdeProd
```
- Use for final testing before release
- Authentic production environment
- No development shortcuts available

## System Properties

The following system properties control the development behavior:

| Property | Values | Effect |
|----------|--------|---------|
| `jakarta.migration.mode` | `dev`, `demo`, `production` | Controls the operating mode |
| `jakarta.migration.dev.simulate_premium` | `true`, `false` | Enables premium simulation in dev mode |

## Safety Features

- **Dev Mode Only**: Premium simulation only works in dev mode
- **Session Reset**: Settings reset on IDE restart for safety
- **Visual Indicators**: Clear "Premium (Simulated)" status when active
- **Production Protection**: No dev features available in production builds

## Troubleshooting

### Dev Tab Not Visible
- Ensure you're using `runIdeDev` or `runIdeDevPremium`
- Check that `jakarta.migration.mode=dev` is set
- Restart the IDE if mode was changed during runtime

### Premium Simulation Not Working
- Verify you're in dev mode
- Check the Dev tab checkbox is enabled
- Look for "Premium (Simulated)" status in the toolbar

### Build Issues
- Clean and rebuild: `./gradlew clean build`
- Ensure all modules are built: `./gradlew buildDevPlugin`
- Check for system property conflicts

## Examples

### Testing Premium Features
```bash
# Start with premium simulation enabled
./gradlew :premium-intellij-plugin:runIdeDevPremium

# In the IDE:
# 1. Open Jakarta Migration tool window
# 2. Dev tab will be visible with premium simulation enabled
# 3. Test premium features (refactor, reports, etc.)
# 4. Toggle simulation off to test free mode behavior
```

### Testing Mode Transitions
```bash
# Start in standard dev mode
./gradlew :premium-intellij-plugin:runIdeDev

# In the IDE:
# 1. Open Jakarta Migration tool window
# 2. Go to Dev tab
# 3. Enable premium simulation
# 4. Observe UI changes across all tabs
# 5. Disable simulation to see free mode
```

## Integration with Existing Workflows

The dev mode integration is designed to work seamlessly with existing development workflows:

- **No changes needed** to existing development processes
- **Backward compatible** with all existing Gradle tasks
- **Optional features** that can be ignored if not needed
- **Safe defaults** that don't affect production builds
