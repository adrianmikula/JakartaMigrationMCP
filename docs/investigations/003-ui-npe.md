# Investigation: UI NullPointerException in Refactor Tab

## Problem Statement
When selecting a recipe card in the Refactor tab, or when clearing selection, a `NullPointerException` occurred in `RefactorTabComponent.updateCardSelectionState`:
`java.lang.NullPointerException: Cannot invoke "adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.getName()" because "recipe" is null`

## Investigation Steps
1. **Trace Analysis**: The stack trace pointed to line 289 in `RefactorTabComponent.java`, where the code was iterating over all components in the `cardsPanel`.
2. **Component Inspection**:
    - The `cardsPanel` contains `JPanel` cards that have a `recipe` client property.
    - However, it also contains a "spacer" `JPanel` (added for grid layout alignment) which does **not** have a `recipe` property.
3. **Trigger**: When the UI refreshed or selection changed, the code attempted to access `getClientProperty("recipe")` on every component. For the spacer, this returned `null`.

## Solution
Implemented a null check in `updateCardSelectionState`:
```java
RecipeDefinition recipe = (RecipeDefinition) card.getClientProperty("recipe");
if (recipe == null) continue; // Skip spacers/non-recipe components
```

## Outcome
The UI is now stable. Selecting cards or refreshing the tab no longer causing crashes.
