



# Dashboard tab


## Licensing
- Feature flagged as premium. Should have a lock icon and upgrade/trial buttons for non-premium users.

## Layout 
- 

## Actions 


## Logic  

appserver artifacts don't have javax in the coordinates, but depending on the version, they will depend on javax libraries. let's use jakartaCompatibility and minVersion from platforms.yaml to determine if appserver artifacts support javax or jakarta based on the min

if a basic scan finds appserver artifacts, then we don't need to query maven central. we can just use platforms.yaml to recommend the min jakarta-compatible  version

now lets improve our dependency scan results classification, where we determine which maven dependencies are jakarta-compatible or require an upgrade. we shouldn't just be assuming all references to javax need upgrading.  some javax packages are still owned by the JDK and don't need to be upgraded to work with Jakarta EE.  instead of just relying on the presence of javax.* artifact and package names, lets use the config file compatibility.yaml to blacklist and whitelist certain artifacts and packages.  If dependencies found in the scan aren't blacklisted or whitelisted, then we should try the maven lookup service as a fallbakc approach. if that yeilds no matches, then the artifact's compatibility should be marked as 'unknown' 



# Deep Scanning

lets check that we have the correct logical flow for our dependency scanning. we should begin by using the configured YAML whitelist/blacklist to identify quick jakarta compatibility matches for top-level dependencies. after that, for dependencies with 'unknown' compatibility status, we should iterate starting with the top-level dependency, followed by it's transitive dependencies, and finally the transitive dependencies of those transitive dependencies..  For each dependency that we iterate through, we should first check against the whitelist/blacklist, then we should use JAR/bytecode scanning to check the actual contents of the class files for jakarta compatibility. If a transitive dependency is identified to have a jakarta incompatibility, that incompatibility travels back up the dependency tree to all of it's ancestors as well.  Each scan result for a dependency should contain metadata which identifies not only the compatibility status, but the precise reason for the compatibility status (whitelist/blacklist, imports, config, reflection, or incompatible transitive dependency, etc) 



## Configuration
- Advanced scan types are configured via scans.json comfiguration file
- Each scan type is linked to one or more refactor recipes







## Tests

