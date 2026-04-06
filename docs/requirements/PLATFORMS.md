


# Startup Logic

## Configuration
- Configuration is loaded from platforms.yaml when the plugin starts
- Platforms.yaml contains details of all supported application server types
- Each application server type specifies which filename and content patterns to search for in the project to identify the appserver name and major version number
- Each application server specifies which major versions work with javax or jakarta
- Each application server also specifies other compatibility/framework requirements like Java/jakarta/spring version



# Platforms tab


## Licensing
- Feature flagged as premium. Should have a lock icon and upgrade/trial buttons for non-premium users.



## Layout (premium intellij module)

- The Platforms tab has a scan button, which detects your current appserver version, and whether your version  supports jakarta
- Shows which appserver you are using and whether it is compatible with Jakarta
- Shows the minimum jakarta-compatible version you need to upgrade to
- If your appserver version is higher than the minimum then it displays as compatible.
- The platforms tab shows other requirements for the upgrade (e.g. minimum Java version). 



## Actions (premium intellij module)
- Clicking an Analyse Project button scans the project for the presence of appservers and JDK version etc.



## Logic  (premium core module)
- A scan loops through all of the platform configurations, and scans the project for files matching the patterns for each platform
- After the scan is complete, the risk category/score on the dashboard tab are recalculated to take into account the detected platforms.
- A change in major appserver version is counted as a significant risk (+100 to the risk score)
- A change in related runtimes or frameworks like java/spring version is counted as +50 to the risk score.
- 




