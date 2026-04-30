#!/usr/bin/env bash
# Fast Test Loop Script - Linux version
# Optimized for snappy agentic AI feedback during development
# Usage: ./scripts/fast-test.sh [compile|fast|core|pdf|all]

set -e

MODE="${1:-}"

if [ -z "$MODE" ]; then
    echo "Usage: $0 {compile|fast|core|pdf|all}"
    echo "  compile - Fast compilation check"
    echo "  fast    - Fast unit tests only (excludes @Tag(\"slow\"))"
    echo "  core    - Core functionality tests"
    echo "  pdf     - PDF generation tests"
    echo "  all     - All tests"
    exit 1
fi

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "Error: gradlew not found in current directory"
    exit 1
fi

# Common Gradle flags for fast execution
GRADLE_FLAGS="--configuration-cache --parallel"

case "$MODE" in
    compile)
        echo "Running compilation check..."
        bash gradlew :premium-core-engine:compileJava $GRADLE_FLAGS
        echo "Compilation successful!"
        ;;
    fast)
        echo "Running fast unit tests only..."
        bash gradlew :premium-core-engine:fastTest $GRADLE_FLAGS
        ;;
    core)
        echo "Running core functionality tests..."
        bash gradlew :premium-core-engine:coreTest $GRADLE_FLAGS
        ;;
    pdf)
        echo "Running PDF generation tests..."
        bash gradlew :premium-core-engine:test --tests "*PdfReportServiceImplTest*" $GRADLE_FLAGS
        ;;
    all)
        echo "Running all tests..."
        bash gradlew :premium-core-engine:test $GRADLE_FLAGS
        ;;
    *)
        echo "Error: Unknown mode '$MODE'"
        echo "Usage: $0 {compile|fast|core|pdf|all}"
        exit 1
        ;;
esac

exit $?
