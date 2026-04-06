



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


## Configuration
- Advanced scan types are configured via scans.json comfiguration file
- Each scan type is linked to one or more refactor recipes







## Tests

