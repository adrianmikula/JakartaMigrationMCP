$content = Get-Content 'migration-core/src/main/java/adrianmikula/jakartamigration/sourcecodescanning/service/impl/SourceCodeScannerImpl.java' -Raw

# Fix the regex patterns - replace \" with \\\" in the patterns
$patterns = @(
    @{
        Old = 'Class\.forName\s*\(\s*\"
'
        New = 'Class\.forName\s*\(\s*\'
    },
    @{
        Old = '"Class\\.forName\\s*\\(\\s*\\"'
        New = '"Class\\.forName\\s*\\(\\s*\\\\"'
    }
)

# This is complex - let me just rewrite the entire file
$newContent = @'
    private static final Pattern CLASS_FOR_NAME_PATTERN = Pattern.compile(
        "Class\\.forName\\s*\\(\\s*\\\\\"([^\"]*javax\\.[^\"]*)\\\\\""
    );
'@
