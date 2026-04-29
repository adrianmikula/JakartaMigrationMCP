


# Startup Logic

## Database
- The list of refactoring recipes should be stored in a DB table
- Changes to the list of recipes should be managed via Liquidbase scripts





# Refactor tab


## Licensing
- Feature flagged as premium. Should have a lock icon and upgrade/trial buttons for non-premium users.

## Layout (premium intellij module)
- Tabs at the top with different categories of refactor types (Java, XML, Annotations, Build/Dependencies)
- When a category is clicked, a grid of cards are shown, with one card per refactor recipe.
- Each card should show the recipe name, a concise description of what it does, and the last date it was run. 
- Cards are grey if never run before, or run and no files were affected. Cards are green if run at least once in the past, and red if run and also undone in the past.

## Actions (premium intellij module)
- User can click on a recipe to see details about what it does, and see the last date it was applied if there is any history for that recipe
- User can click to apply a recipe to the codebase
- User can click to undo a recipe that was previously applied to the codebase


## Logic  (premium core module)
- All recipes should implement a standard interface
- Recipes can be provided by openrewrite if dealing with java source changes 
- If provided by openrewrite, the recipe script should be downloaded from official sources.
- If an openrewrite recipe isn't available/applicable, then use a custom REGEX string parser implementation.
- When a recipe is run, a new history record is created with details of the recipe, the date, and the list of files that were changed. (premium core module)
- The list of files changed should be stored in a separate DB table with a join key to the main history table.


# History Tab

## Licensing
- Feature flagged as premium. Should have a lock icon and upgrade/trial buttons for non-premium users.

## Layout  (premium intellij module)
- borderless table containing a vertical list of historical refactor actions
- each historical action has columns for recipe, status, date applied, and number of files changed
- each historical action has an Undo button
- If the historical action has already been undone, then the status column should indicate that the action was later undone, with a link to the undo history record.

## Actions (premium intellij module)
- User can click on a historical refactor action to see details of all the files that were changed
- User can click to undo a recipe that was previously applied to the codebase
- User can click on a linked undo status for a historical record which was later undone, to jump to the related undo historical action in the UI list.


## Logic (premium core module)
- A new record is added to the history DB every time a refactor recipe is started
- The status of the historical action is updated when a recipe completes, and a list of changed files is also added and linked to the history record
- When the undo button next to a history record is clicked, the recipe is run in reverse. A new record is added to indicate the undo action.  The original history record is not deleted, but should have a DB flag set to link it to the history record for the corresponding undo action.




# Example Recipes
- https://docs.openrewrite.org/recipes/java/dependencies/upgradedependencyversion
- https://github.com/openrewrite/rewrite-migrate-java/tree/main/src/main/resources/META-INF/rewrite
- 





# Testing

## Unit Tests




# Configuration tests
add a test which fails if the JSON config contains recipes which are missing the openrewrite class (unless they are regex recipes)


## Integration Tests
- loop through all recipes in recipes.json and run them, to ensure they exist and are configured correctly
- loop through all recipes in recipes.json to chack that they exist in openrewrite's list


