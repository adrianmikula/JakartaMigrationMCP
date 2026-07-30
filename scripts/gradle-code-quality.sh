#!/bin/bash
# Helper script for Gradle code quality checks
if [ -f gradlew ]; then
    ./gradlew codeQualityVerify --no-daemon
else
    gradle codeQualityVerify --no-daemon
fi

