# PDF Report Generation Memory Issues

## Problem Description

PDF report generation causes Java heap space errors even for small projects due to excessive memory consumption during HTML-to-PDF conversion.

## Root Cause Analysis

### Memory Usage Pattern
- **Small project (2 dependencies)**: 232 MB memory before PDF conversion
- Memory usage scales linearly with data volume
- ITextRenderer loads entire HTML document into memory for PDF conversion
- HTML templates with embedded CSS create massive string literals

### Contributing Factors

1. **Large HTML Templates**
   - Original implementation had 1817 lines of Java code with embedded CSS
   - CSS was duplicated as string literals for each report generation
   - Each report regenerated the same CSS from scratch

2. **ITextRenderer Memory Model**
   - Flying Saucer's ITextRenderer loads entire DOM into memory
   - No streaming or chunking support for large documents
   - PDF generation requires full document in memory

3. **Data Volume**
   - Tables with 1000+ rows create massive HTML strings
   - Each table row adds HTML markup overhead
   - Large projects with 100+ dependencies and 1000's of scan findings impossible

## Optimization Work Completed

### 1. CSS Caching (Implemented)
- Extracted CSS to external resource files:
  - `src/main/resources/pdf-reporting/css/common-styles.css`
  - `src/main/resources/pdf-reporting/css/header-styles.css`
  - `src/main/resources/pdf-reporting/css/footer-styles.css`
  - `src/main/resources/pdf-reporting/css/refactoring-styles.css`
- CSS loaded once during service initialization and cached
- Reduced template overhead significantly
- Fallback to inline CSS if external files fail to load

### 2. Memory Monitoring (Implemented)
- Added logging of HTML content size before conversion
- Warning when HTML exceeds 10 MB threshold
- Better visibility into memory usage during report generation

### 3. HTML-Only Reports (Implemented)
- Disabled PDF conversion entirely
- Reports now save as HTML files
- Users can view reports in browser
- No risk of heap space errors

## Configuration Constants (Not Currently Used)

The following constants were added for future optimization but are not currently used:

```java
private static final int MAX_JAVAX_REFERENCES_IN_TABLE = 100;
private static final int MAX_RECIPES_IN_TABLE = 50;
private static final int MAX_DEPENDENCIES_IN_TABLE = 100;
private static final int MAX_BLOCKERS_IN_LIST = 20;
```

These can be used for data truncation when PDF generation is re-enabled.

## Future Solutions for PDF Re-enablement

### Option 1: Data Truncation
- Limit tables to reasonable maximums (100-200 items)
- Show top N items by priority/relevance
- Add warnings when data is truncated
- Configuration-based limits via system properties

### Option 2: Streaming PDF Generation
- Use PDF libraries with streaming support
- Generate PDF page-by-page
- Keep memory usage constant regardless of data volume
- More complex implementation

### Option 3: Multiple Smaller PDFs
- Split reports by section (dependencies, scan results, etc.)
- Generate separate PDF files for each section
- User can combine if needed
- Reduces individual PDF memory footprint

### Option 4: Alternative PDF Libraries
- Evaluate libraries with better memory management
- Consider Apache PDFBox with streaming
- Investigate iText 7 with incremental rendering
- May require significant refactoring

### Option 5: Server-Side Generation
- Offload PDF generation to server
- Client sends HTML, server returns PDF
- Requires additional infrastructure
- Better for large-scale deployments

## Recommendations

### Immediate (Current State)
- Continue with HTML-only reports
- Provides formatted reports without memory issues
- Users can print HTML to PDF if needed
- Low-risk, simple solution

### Short Term
- Monitor user feedback on HTML-only reports
- Collect data on actual project sizes
- Evaluate if PDF is critical for users

### Long Term
- Implement data truncation with configuration
- Add system properties for adjustable limits
- Consider streaming PDF generation libraries
- Re-enable PDF with proper safeguards

## Testing

### Current Tests
- `RefactoringActionReportTest` - Updated to expect HTML output
- Tests verify HTML file generation
- Memory usage should be minimal

### Future Tests Needed
- Large dataset simulation (1000+ items)
- Memory profiling with truncation
- Configuration property validation
- PDF re-enablement testing

## Related Files

- `premium-core-engine/src/main/java/adrianmikula/jakartamigration/pdfreporting/service/impl/HtmlToPdfReportServiceImpl.java`
- `premium-core-engine/src/main/resources/pdf-reporting/css/` (CSS resource files)
- `premium-core-engine/src/test/java/adrianmikula/jakartamigration/pdfreporting/service/impl/RefactoringActionReportTest.java`

## Date Created
2026-04-25

## Status
PDF generation disabled. HTML-only reports implemented as temporary solution.
