


# Startup Logic

## Configuration
- Configuration is loaded from platforms.yaml when the plugin starts
- Platforms.yaml contains details of all supported application server types
- Each application server type specifies which filename and content patterns to search for in the project to identify the appserver name and major version number
- Each application server specifies which major versions work with javax or jakarta
- Each application server also specifies other compatibility/framework requirements like Java/jakarta/spring version



# Platforms tab


## Licensing
- Feature flagged as premium.

## Layout (premium intellij module)

- The Platforms tab has a scan button, which detects your current appserver version, and whether your version  supports jakarta
- The platforms tab shows other requirements for the upgrade (e.g. minimum Java version). 
- Shows a list of refactor recipes which match your appserver
- Each recipe has an apply button to trigger it.


## Actions (premium intellij module)
- Clicking an Analyse Project button scans the project for the presence of appservers and JDK version etc.
- Clicking on the Apply button next to a recipe runs it



## Logic  (premium core module)
- After the scan is complete, the risk category/score on the dashboard tab are recalculated to take into account the detected platforms.
- A change in major appserver version is counted as a significant risk (+100 to the risk score)
- A change in related frameworks like java/spring version is counted as +50 to the risk score.





