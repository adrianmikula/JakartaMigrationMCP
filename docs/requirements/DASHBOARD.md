



# Dashboard tab


## Licensing
- Feature flagged as community.

## Layout 
- Top: show 2 large coloured indicators  showing the risk scores and categories. The indicators are in a speedometer style with a needle which points left/green if the risk is low, up/yellow if medium, and right/red if high. 
- Indicator 2 shows the migration risk
- Indicator 1 shows the migration effort 

- Middle: shows the percentage of all available scans that have been run (only premium/trial users can run all scans), the number and type of dependencies found, and the number of applicable refactor recipes found.

- Bottom: show a table of the scan results, with the scan name, the number of items found, and the risk level.


## Actions 


## Logic  
-  The migration risk score ranges from 0 to 100.
- the migration risk score is calculated based on the total number of items found by the basic scans, advanced scans, and platform scans.  The weighting for each scan result is determined by the YAML configuration.
- The migration effort score ranges from 1 to 50 weeks.
- The migration effort score is calculated based on the migration risk score. Each risk category has a predefined effort time range, defined in the YAML configuration.

## Tests

