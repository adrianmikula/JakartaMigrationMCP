Lets complete the following tasks in order. While implementing the tasks, always ensure we follow the coding guidelines defined in AGENTS.md





# bug fixes

the reports tab has disappeared. restore it.

the experimental feature tabs are not dynamically displayed after enabling experimental features from the support UI tab. Fix this.

after clicking 'analyse project' in the  platforms tab, it always reports "No application servers detected" even when I've opened an example repo which I know contains an appserver. Soemthing is wrong with how we are scanning for appservers. Start by checking that our platform integration tests testing with real github projects from examples.yaml, and that the tests are passing  

failed: Recipe not found: org.openrewrite.java.migrate.jakarta.JavaxAnnotationToJakartaAnnotation. Discovered 730 recipes. Top ones: [org.openrewrite.DeleteSourceFiles,



# performance

lets load the example repo for the performance tests from the 'project_complexity' section in examples.yaml. project_complexity is already present in examples.yaml, and it's spelt correctly. for some reason the AI agent keeps thinking it's not there or misspelt. Look into other reasons why its not loading

it's bad practice to manually force GC calls inside our code. Instead of this, optimise any loops to use try/catch with resources, and also optimise any large datasets being loaded to use streaming rather than loading everything into memory at once.




# testing

lets review our existing integration tests, and ensure that wherever possible, they are loading real repos as examples from examples.yaml (via the ExampleProjectManager) instead of hardcoding fake mocked input data.

lets write some realistic integration tests for the maven artifact lookup service, which pass in common javax artifact coordinates, and verify that the service found matching jakarta maven artifact coordinates



# configuration

lets remove feature-flags.yaml and keep all of the feature flags defined in code.




# user help

lets update the MCP tool list on the AI tab to reflect the current set of available tools
lets optimise the AI prompt suggestions on the AI tab, and keep them simple and concise 




# source control

lets use .gitattributes to normalise line endings so we don't see lots of differences when we switch our dev OS 




# dependencies

why are our local gradle tasks not finding and reusing cached maven artifacts?  Gradle keeps downloading the same artifacts every time






# platform enhancements

lets add support for detecting common appserver gradle and maven artifacts during the platform scanning. instead of writing complex regex patterns, lets build in artifact matching support into the scan, and just include common artifact names in the YAML

now lets massively simplify the platform file-based searches. Instead of specifying specific folder structures to search (e.g. src/main/webapp/WEB-INF/tomcat-web.xml), lets just search for the file name in any location within the project.  Then lets have a regex to help us find the current version of the appserver from within the found file.

lets also add support for detecting gradle variables used inside maven artifact coordinates (specifically the artifact version number), and locating the variable definition in gradle.properties or libs.versions.toml (anywhere in the project) to extract the actual value of the version.

now lets review platforms.yaml and ensure all config is following the new design. I can still see some full paths and regex maven artifact searches in there

lets also add support for more appservers:
- Netbeans
- Glassfish
- Spring Boot

lets review  the integration tets for platform scans to ensure they are testing using real github repos obtained from the examples.yaml config file

we can do most of the testing via unit tests, and just run a single integration test for each appserver type using the real github projects from examples.yaml (via the existing examples manager class)


# dependency graph improvements

improve the force-directed dependency graph so the nodes aren't bunched together so close that they overlap each other

Lets choose the default view based on the number of dependencies:
- 5 or less: tree mode
- 5 to 25: circular mode
- 25 or more: force-directed mode





# advanced scans

we will also need to add detection of dockerfile changes using examples like https://github.com/lurodrig/log4j2-in-tomcat

lets improve our examples.yaml to specify what kind of items are present in each appserver example using broad categories: e.g. dependencies, metadata, docker, clients, spring

lets extend our platform scan functionality to also count eh number of wars, ears, etc which are built/deployed to the appserver. we will use this to increase or decrease the platforms risk score calculation

are the risk scoring weights still all in the YAML? I don't want any hardocded weights in the java code



# support

Link to repos which outline appserver-specific jakarta migration steps. 
e.g. https://github.com/WASdev/sample.DefaultApplication
https://github.com/IBM/application-modernization-javaee-quarkus




# refactoring

lets review our recent code changes and eliminate any code duplication of unnecessary complexity. try to reduce the length of the code by 50%



# final checks

let's fix any compilation issues, ensure all the tests still pass, and fix any test failures




After completing all the tasks, always check the rules in AGENTS.md to ensure that our implementation follows all the rules.  Then finish by checking that the code compiles, and complete the post-task steps in AGENTS.md