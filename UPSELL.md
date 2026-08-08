# Jakarta Migration Plugin — Migration Risk Audit Upsell

This branch adds in-plugin CTAs for a **Migration Risk Audit** that the author offers for projects the plugin cannot fully migrate automatically.

## Where the CTAs appear

1. **In-scan notification / balloon**
   - Triggered automatically after a **Quick Scan** or **Deep Scan** when the plugin detects patterns it cannot fully migrate.
   - Copy is controlled by `audit.inscan.message` in `audit-upsell-config.properties`.
   - One-click dismiss: **"Don't show again for this project"** is stored in memory for the current project for the IDE session.

2. **Risk tab warning box**
   - A prominent panel at the top of the Risk/Dashboard tab appears when the project exceeds the configured complexity/risk thresholds.
   - It explains which threshold(s) were exceeded (unsupported issue count, total issue count, and/or risk score) and provides a CTA button.

3. **Prominent global footer**
   - A full-width bar is docked at the bottom of the plugin tool window with large text and a CTA button. It is shown no matter which tab is open.
   - Copy is configurable via `audit-upsell-config.properties` (`audit.cta.title`, `audit.cta.subtitle`, `audit.cta.button.text`).

4. **Support / About tab (settings / help menu)**
   - A new clickable link titled **"Migration Risk Audit"** is listed with the other support links.

## Configuration

All landing page, copy, and complexity thresholds are now configured in `premium-intellij-plugin/src/main/resources/audit-upsell-config.properties` (with fallback defaults in `AuditUpsellService.java`):

- `audit.landing.page.url` — landing page opened by all CTAs
- `audit.cta.title` / `audit.cta.subtitle` / `audit.cta.button.text` — bottom bar copy
- `audit.inscan.message` — in-scan notification body
- `audit.complexity.trigger.min.unsupported.issues` — minimum no-recipe issues to trigger CTA
- `audit.complexity.trigger.min.total.issues` — minimum total advanced issues to trigger CTA
- `audit.risk.score.threshold` — computed risk score threshold (formula: `totalIssues + unsupportedIssues * 5`)

## Click handling

All CTAs open the user's default browser to `audit.landing.page.url` from the config file.

- **Default URL:** `https://codemedicconsulting.netlify.app/?panel=services&item=jakarta-migration`
- **To change it:** edit `audit.landing.page.url` in `premium-intellij-plugin/src/main/resources/audit-upsell-config.properties`.

## Analytics

No new external analytics dependencies were added. The plugin emits simple `LOG.info` events:

- `audit_upsell_cta_shown source=<source>`
- `audit_upsell_cta_clicked source=<source>`

## Files changed

- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/service/AuditUpsellService.java` (new)
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/MigrationToolWindow.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/DashboardComponent.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/SupportComponent.java`
- `premium-intellij-plugin/src/main/resources/support-urls.properties`
- `premium-intellij-plugin/src/main/resources/audit-upsell-config.properties` (new)
- `UPSELL.md`

## Assumptions, blockers, and open questions

- **Assumption:** The complex-pattern trigger is based on the difference between total advanced issues and issues in categories that already have OpenRewrite recipes, controlled by the thresholds in `audit-upsell-config.properties`. This is a first-pass heuristic; a more refined signal can be added later.
- **Assumption:** The `https://codemedicconsulting.netlify.app/?panel=services&item=jakarta-migration` landing page exists and is the default in `audit-upsell-config.properties`. Change it there; no code constant needs to be edited.
- **Assumption:** "Don't show again for this project" is currently stored in a per-project in-memory `Set` for the current IDE session. Persistence across restarts was not requested but could be added via `PropertiesComponent` later.
- **Open question:** Should premium users also see the in-scan balloon, or only the subtle persistent "Get Help" links? Current implementation shows the notification whenever complex patterns are detected, with a non-blocking, optional action.
- **Open question:** Should click/shown events also be sent to the existing `UsageService`/Supabase event queue? For now they are logged only, keeping the change minimal.
