# Plugin Icon Specification

## Overview

The Jakarta Migration IntelliJ plugin uses a custom icon that complies with JetBrains official requirements for format, dimensions, and naming.

## Icon Files

All icons are located in `premium-intellij-plugin/src/main/resources/icons/` and are packaged in the plugin JAR.

### Base Icon

| File | Size | Purpose |
|------|------|---------|
| `pluginIcon.svg` | Vector | Primary toolbar and action icon (preferred format) |
| `pluginIcon.png` | 13×13 px | Fallback toolbar icon (standard DPI) |
| `pluginIcon@2x.png` | 26×26 px | HiDPI toolbar fallback (2x resolution) |

### Additional Sizes (Optional)

| File | Size | Purpose |
|------|------|---------|
| `pluginIcon-24.png` | 24×24 px | Marketplace listing (optional) |
| `pluginIcon-24@2x.png` | 48×48 px | HiDPI marketplace (optional) |
| `pluginIcon-128.png` | 128×128 px | Splash screen (optional) |
| `pluginIcon-128@2x.png` | 256×256 px | HiDPI splash (optional) |

### Source Asset

The original source asset is `assets/Tech transformation and dependency flow.png`. This file is transformed using ImageMagick to generate the required PNG sizes. The SVG version (`pluginIcon.svg`) is the primary icon format used in plugin.xml.

### Additional Files

- `pluginIcon_dark.svg` – Dark theme variant (not currently used in plugin.xml)

## Configuration

### plugin.xml

The plugin descriptor references the SVG icon (preferred by JetBrains):

```xml
<idea-plugin ... icon="/icons/pluginIcon.svg">
```

Action icons also reference the same SVG:

```xml
<action ... icon="/icons/pluginIcon.svg"/>
```

### HiDPI Support

The IntelliJ Platform automatically loads `@2x` variants when available on HiDPI displays. No additional configuration is required.

## Naming Convention

- `iconName.svg` – Vector icon (preferred format)
- `iconName.png` – 1x base fallback icon
- `iconName@2x.png` – 2x HiDPI fallback variant
- `iconName-24.png` – 24×24 specific size variant
- `iconName-24@2x.png` – 48×48 HiDPI variant

All icons are placed in the same directory (`icons/`) and referenced via paths starting with `/icons/`.

## Build Inclusion

The `premium-intellij-plugin` module's `processResources` task copies all files from `src/main/resources` into the plugin JAR, ensuring the icons are available at runtime.

## Verification

Run the following to verify plugin packaging:

```bash
./gradlew :premium-intellij-plugin:verifyPlugin
```

This validates `plugin.xml` and checks that icon files exist in the expected locations.

## References

- [IntelliJ Platform SDK: Icons](https://plugins.jetbrains.com/docs/intellij/icons.html)
- [Plugin Icon Requirements](https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html)
