


# Example Projects

- examples.yaml contains one example project from github for every refactor recipe type, and one for every advanced scan type.  
- Each example Github project should contain one or more javax artifacts or imports which need to be refactored to jakarta.
- The example Github projects should not be too large, or they will take a long time to load during testing.
- It's okay to have the same github repo listed more than once. 
- examples.yaml is used by our tests to verify scans and recipes against real Github projects
- Tests which use examples.yaml should download and unzip the projects every time ensure they are clean checkouts
- Examples.yaml should be updated whenever advanced scans or refactor recipes are added/changed



# Recipes
defined in JSON


# Scans
- Each refactor recipe should have a scan type associated with it in the JSON configuration



# Startup Actions
- Recipes should be loaded from recipes.yaml when the plugin starts up, and inserted into the DB table to ensure it's up to date with the config.
- The DB should use the recipe name as the PK for the recipe and history tables, to avoid breaking history records if the order in the YAML changes between releases.




