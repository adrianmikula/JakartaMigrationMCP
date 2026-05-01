# Dependency Scanner Duplication Issue

## Problem Description

The codebase contains duplicated Maven POM parsing logic across multiple dependency scanner implementations. This duplication leads to maintenance overhead and inconsistent behavior between scanners.

## Affected Components

### 1. MavenDependencyGraphBuilder (community-core-engine)
- **Location**: `community-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/service/impl/MavenDependencyGraphBuilder.java`
- **Approach**: Uses DOM XML parsing
- **Property Resolution**: Recently fixed to handle custom properties
- **Status**: ✅ Fixed property resolution issue

### 2. TransitiveDependencyScannerImpl (premium-core-engine)
- **Location**: `premium-core-engine/src/main/java/adrianmikula/jakartamigration/advancedscanning/service/impl/TransitiveDependencyScannerImpl.java`
- **Approach**: Primary: Maven command execution, Fallback: Regex-based parsing
- **Property Resolution**: Recently added to fallback parser
- **Status**: ✅ Fixed property resolution issue in fallback parser

## Duplication Details

### XML Parsing Logic
Both implementations contain separate logic for:
- Parsing Maven pom.xml files
- Extracting dependency information
- Resolving version numbers
- Handling property substitution

### Property Resolution
- **MavenDependencyGraphBuilder**: Uses `resolveProperty()` method with DOM navigation
- **TransitiveDependencyScannerImpl**: Uses regex-based `extractMavenProperties()` method

### Test Coverage
- **MavenDependencyGraphBuilder**: Comprehensive test coverage added
- **TransitiveDependencyScannerImpl**: No specific tests for property resolution (TODO)

## Recent Fixes Applied

### Property Resolution Implementation
1. **Fixed MavenDependencyGraphBuilder.resolveVersion()**:
   - Added direct property resolution for dependencies
   - Previously had placeholder comment at line 333

2. **Fixed TransitiveDependencyScannerImpl fallback parser**:
   - Added `extractMavenProperties()` method
   - Added `parseDependenciesWithProperties()` method
   - Enhanced fallback parser to resolve `${property.name}` references

## Technical Debt Impact

### Maintenance Overhead
- Two separate code paths to maintain for the same functionality
- Bug fixes need to be applied in multiple places
- Inconsistent behavior between scanners

### Code Quality Issues
- Violates DRY principle
- Increases cognitive load for developers
- Potential for divergence in behavior

## Recommended Solutions

### Short-term (Immediate)
- ✅ Fix property resolution in both scanners (COMPLETED)
- Add comprehensive test coverage for both implementations

### Medium-term (Next Sprint)
- Create shared utility class for Maven POM parsing
- Consolidate property resolution logic
- Standardize test scenarios across both scanners

### Long-term (Future Refactoring)
- Consider using a single, unified dependency scanner
- Evaluate using existing Maven libraries (e.g., Maven Model API)
- Implement strategy pattern for different scanning approaches

## Implementation Status

| Component | Property Resolution Fixed | Tests Added | Status |
|-----------|---------------------------|-------------|---------|
| MavenDependencyGraphBuilder | ✅ | ✅ | Complete |
| TransitiveDependencyScannerImpl | ✅ | ❌ | Pending tests |

## Future Considerations

1. **Performance**: DOM parsing vs Regex parsing performance characteristics
2. **Accuracy**: Which approach provides more accurate dependency resolution?
3. **Extensibility**: How to handle complex Maven features (profiles, inheritance, etc.)
4. **Integration**: How to better integrate with existing Maven tooling

## Related Issues

- Property resolution not working for custom variables in pom.xml files
- Inconsistent dependency version detection between scanners
- Missing test coverage for edge cases in property resolution

## Last Updated

- **Date**: 2026-04-24
- **Author**: AI Assistant
- **Version**: 1.0
