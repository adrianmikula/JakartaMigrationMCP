# Senior Developer Trust Skill

## Overview

This skill guides evaluating and improving a software product so that experienced, skeptical developers trust its output enough to pay for it. It is not about chasing perfect accuracy; it is about positioning, transparency, and signal-to-noise.

Senior developers do not expect:

- 100% correctness
- Fully automated work
- "AI magic"

They do expect:

- Signal > noise
- Clear limitations
- No hidden risk
- Explanations for why something is flagged

This skill provides a generic, repeatable process to audit any developer-facing product for trust-eroding copy, inconsistent metrics, overconfident claims, and distracting noise, and to produce a concrete plan to fix them.

## When to Use This Skill

Use this skill when:

- A product is being repositioned as a "second opinion" or "risk radar" instead of a replacement for human judgment.
- Users, reviews, or conversion data suggest senior developers do not trust the output.
- The product recently introduced or changed a scoring/risk/confidence UI and the change may have sacrificed trust for monetisation or growth.
- A marketing or copy pass is needed before a release.
- You are asked to review a feature, report, dashboard, or landing page for consistency and credibility.

## Core Principles

### 1. Position as augmentation, not replacement

- Shift from "automates X" to "catches what you might miss" or "helps you prioritise X."
- The tool's value is in flagging, explaining, and helping the human make a safer decision.

### 2. Define limitations explicitly

- State what the tool does **not** do.
- Example framing:
  - "Does not guarantee correctness."
  - "Flags likely risk areas, not all issues."
  - "Designed for large/legacy codebases where naive automation struggles."
  - "Scores are heuristics, not measurements."

### 3. Prefer signal over noise

- Each visible metric, dial, or label should answer one clear question.
- If a number is derived from multiple unrelated factors, either split it or label it carefully.
- Remove or hide placeholder values, dead components, and conflicting duplicates.

### 4. Match semantics, colours, and labels

- A metric that is worse when higher should use a higher-is-worse colour scale.
- A gauge label, its numeric score, and its explanation text must describe the same thing.
- Do not reuse a generic component intended for one meaning with a contradictory title.

### 5. Explain why, not just what

- Every risk, score, or recommendation should be traceable to an input the user can inspect.
- Use footnotes, tooltips, or expandable bullets to show the underlying factors.

### 6. Avoid overconfident and absolute language

Replace absolute or magical claims with precise, bounded claims:

| Overconfident | Trust-first |
|---|---|
| "Automate your migration" | "De-risk and plan your migration" |
| "One-click refactoring" | "Apply recipes from a single action" |
| "Auto-fixes" | "Assisted fixes" or "Suggested remediation" |
| "AI-powered" / "AI magic" | "AI-assisted" or "AI-assisted review" |
| "100% accurate" | "Heuristic / estimate" |
| "Seamless migration" | "Guided migration" |
| "Guarantees" | "Helps reduce the chance of" |
| "Fully automatic" | "Automated where safe, flags the rest" |

## Process

### Step 1: Identify the trust surfaces

Map all user-facing surfaces where trust is built or eroded:

- Primary dashboard / results UI
- Gauges, dials, scores, and risk meters
- Explanation panels, tooltips, and help text
- Notification / toast messages
- Upsell, upgrade, and CTA copy
- Marketplace / landing-page description
- Report templates and executive summaries
- Feature flags and feature descriptions
- Pricing and tier descriptions
- AI / agentic tool descriptions
- Documentation and README

### Step 2: Audit for inconsistencies

For each surface, ask:

1. Does the label match the value it displays?
2. Does the colour scale match the semantic direction of the metric?
3. Is there a clear, honest explanation of how the score was calculated?
4. Are there conflicting terms for the same concept across the UI?
5. Are there dead or unused components that could drift and confuse?
6. Are there placeholders or hard-coded defaults that look authoritative but are not?

### Step 3: Collect overconfident or absolute language

Search for words and phrases such as:

- automate, automatic, auto-fix
- one-click, one-shot
- 100%, guarantee, always
- seamless, effortless, magic
- fully, complete, all
- accurate (without qualification)

For each hit, decide whether it is a real feature name that can be kept, a marketing overclaim that should be softened, or a UI label that should be replaced.

### Step 4: Review the metric model

For each score, gauge, or risk dimension:

- Is it measuring what its title claims?
- Is the formula transparent enough to explain in one sentence?
- Does the scale make sense to a senior engineer?
- Are fallback values hidden or exposed?
- Is a higher-is-worse metric displayed on a higher-is-better component?

### Step 5: Write the trust-improvement plan

Group changes by surface, not by file. For each item, include:

- The trust principle it violates
- The current wording or behaviour
- The proposed trust-first wording or behaviour
- The files/components likely to be affected
- A verification step

Keep the plan scoped: copy and label changes first, then metric semantics, then larger UX changes.

### Step 6: Verify

- Compile the affected modules.
- Run the fast test loop.
- Manually review the changed surfaces for consistency.
- If possible, ask: "Would a senior engineer reading this believe the tool is honest about what it can and cannot do?"

## Checklist for Senior-Dev Trust

### UI / Dashboard

- [ ] No gauge has a title, score, and explanation that describe different things.
- [ ] Higher-is-worse metrics use red/orange/yellow on the right.
- [ ] Higher-is-better metrics use green/yellow/orange on the right.
- [ ] Every score has an adjacent explanation of the inputs.
- [ ] Placeholders and defaults are clearly labelled as "not scanned" or "unknown".
- [ ] No dead or duplicate components are visible.

### Copy and Messaging

- [ ] No absolute or magical automation claims remain in user-facing text.
- [ ] Feature names and descriptions use "assisted", "suggested", "review", "flag" where appropriate.
- [ ] Premium/upgrade prompts do not promise automatic outcomes.
- [ ] Limitations are stated in the product description, not hidden.

### Reports and Summaries

- [ ] Every report has a limitations or "what this does not cover" section.
- [ ] Risk/confidence language is consistent with the UI.
- [ ] Executive summaries avoid "complete", "guaranteed", or "100%".

### AI / Tooling Descriptions

- [ ] AI or MCP tools are described as assisting, not deciding.
- [ ] Tool outputs include a confidence or uncertainty signal where possible.
- [ ] No tool description claims to produce production-ready changes without review.

### Upsell and CTAs

- [ ] CTA copy does not imply the product failed; it frames the human service as a complement.
- [ ] Thresholds for showing CTAs are configurable and not overly aggressive.

## Common Mistakes to Avoid

- **Swapping an honest metric for a monetisation metric.** Replacing a "confidence" or "coverage" dial with a "complexity" dial can help upsell but often erodes trust unless the new metric is equally transparent.
- **Softening language everywhere.** Some terms (e.g. product names, feature keys) can stay; only the *descriptions* should be de-risked.
- **Adding more dials to solve trust.** More numbers usually create noise. Prefer fewer, well-explained signals.
- **Hiding limitations in documentation only.** Limitations should also appear near the metric or recommendation they apply to.

## Example Trust-First Rewrite

**Before:**

```
Auto-Refactor: Automatically migrate your entire codebase to the new platform.
```

**After:**

```
Recipe-Assisted Refactor: Apply transformation recipes to safe patterns.
  - All changes are shown in the diff before you commit.
  - Risky or unsupported patterns are flagged for manual review.
```

## Reference Materials

- The product's own positioning or strategy document
- Any project-specific conventions in `docs/patterns/` or equivalent
- The product's README and marketplace listing (or equivalent public description)
- Other skill files in your `skills/` folder for format and conventions
