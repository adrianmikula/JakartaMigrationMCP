# Premium Upgrade Notice Consistency

## Status
Open — identified during UI prominence work for truncation/upsell notices.

## Problem
Premium upgrade CTAs are implemented inconsistently across the IntelliJ plugin. Several places hand-roll dialogs, links, and marketplace URLs instead of reusing the shared `PremiumUpgradeButton` / `TruncationNoticePanel` components. This creates visual and messaging drift.

## Inconsistencies Found

1. **Custom link vs. shared button**
   - `CreditsProgressBar` uses a small blue HTML `JBLabel` "Upgrade to Premium" link.
   - Most other components should use `PremiumUpgradeButton` for a consistent gold button style.

2. **Dialog/modal upsells are one-off strings**
   - `HistoryTabComponent` — `Messages.showYesNoDialog` with custom text when undo credits are exhausted.
   - `BasicRefactorComponent` — hand-rolled dialog for refactor credit exhaustion.
   - `RecipesPanelComponent` — `Messages.showWarningDialog` for credit exhaustion.
   - These should be routed through a shared credit-exhaustion dialog or `PremiumUpgradeButton` panel.

3. **Marketplace URL duplicated in many files**
   - `TruncationNoticePanel`, `PremiumUpgradeButton`, `LicenseExpirationNotifier`, `HistoryTabComponent`, `BasicRefactorComponent`, `CreditsProgressBar`, and `MigrationToolWindow` each hard-code `https://plugins.jetbrains.com/plugin/30093-jakarta-migration`.
   - `gradle.properties` and `support-urls.properties` use a different slug (`30093-jakarta-migration-javax--jakarta-`) for support links.
   - All upgrade CTAs should reference one canonical source, e.g. `PremiumUpgradeButton.getMarketplaceUrl()`.

4. **Copy is scattered across resource bundles**
   - `ui-text.properties` and `notification-messages.properties` contain hard-coded upgrade copy.
   - `LicenseService` / `SimplifiedLicenseService` / `FeatureFlagsService` already produce core upgrade prompts but are not used consistently by the UI.

5. **Different external landing page**
   - `AuditUpsellService` opens a Netlify landing page (`codemedicconsulting.netlify.app/...`) from `audit-upsell-config.properties`.
   - This is a different destination from the JetBrains Marketplace used by all other premium CTAs. Decide whether this is intentional and document it.

## Proposed Fixes

1. **Unify the marketplace URL**
   - Export `MARKETPLACE_URL` from a single class (e.g. `PremiumUpgradeButton.getMarketplaceUrl()` or `FeatureFlagsProperties.getMarketplaceUrl()`) and replace all hard-coded usages.
   - Reconcile `gradle.properties` / `support-urls.properties` slugs with the one used for upgrades if they should be the same.

2. **Replace custom links with `PremiumUpgradeButton`**
   - Update `CreditsProgressBar` to use `PremiumUpgradeButton`.
   - Consider if `TruncationNoticePanel` should create its button through `PremiumUpgradeButton` to avoid duplicate button styling code.

3. **Create a shared credit-exhaustion upsell component**
   - Introduce `CreditExhaustedDialog` or `UpsellDialog` that takes a context string and always uses the same title, icon, marketplace action, and analytics event.
   - Replace the one-off dialogs in `HistoryTabComponent`, `BasicRefactorComponent`, and `RecipesPanelComponent`.

4. **Centralize upgrade copy**
   - Move all user-visible upgrade text to a single resource bundle or `LicenseService.getUpgradePrompt()` so pricing and wording stay in sync.

5. **Document `AuditUpsellService` landing page decision**
   - Either make the audit upsell use the marketplace URL as well, or keep the audit landing page and document why it is intentionally different.

## Affected Files
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/components/PremiumUpgradeButton.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/components/TruncationNoticePanel.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/CreditsProgressBar.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/HistoryTabComponent.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/refactor/BasicRefactorComponent.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/RecipesPanelComponent.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/service/AuditUpsellService.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/license/LicenseExpirationNotifier.java`
- `premium-intellij-plugin/src/main/resources/ui-text.properties`
- `premium-intellij-plugin/src/main/resources/notification-messages.properties`
- `config/freemium.properties`
- `gradle.properties`
