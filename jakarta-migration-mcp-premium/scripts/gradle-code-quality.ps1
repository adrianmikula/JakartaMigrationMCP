# Helper script for Gradle code quality checks
if (Test-Path gradlew.bat) {
    .\gradlew.bat codeQualityVerify --no-daemon
} else {
    # Use mise exec to run gradle (mise exec sets up the environment)
    mise exec -- gradle codeQualityVerify --no-daemon
}

