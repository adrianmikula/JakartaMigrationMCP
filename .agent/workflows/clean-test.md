---
description: Run Java tests cleanly without leaving orphaned processes
---
To run tests without leaving orphaned Gradle daemons or Java processes:

1. Stop any existing orphans first
// turbo
mise run kill-gradle-java-force

2. Run tests with the --no-daemon flag to ensure the Gradle process exits completely
// turbo
./gradlew.bat test --no-daemon

Alternatively, you can use the combined task if you've updated .mise.toml:
// turbo
mise run clean-test
