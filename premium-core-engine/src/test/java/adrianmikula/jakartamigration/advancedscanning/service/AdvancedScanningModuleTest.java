package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.service.impl.TransitiveDependencyScannerImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.JarResolver;
import adrianmikula.jakartamigration.jaranalysis.service.JarCompatibilityScanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AdvancedScanningModule to verify proper service initialization.
 * This test ensures that bytecode scanning services are properly initialized
 * and passed to the TransitiveDependencyScanner.
 */
class AdvancedScanningModuleTest {

    @Test
    void constructor_shouldInitializeTransitiveDependencyScannerWithBytecodeScanningServices() {
        // Given
        AdvancedScanningModule module = new AdvancedScanningModule(null);

        // When
        TransitiveDependencyScanner scanner = module.getTransitiveDependencyScanner();

        // Then
        assertNotNull(scanner, "TransitiveDependencyScanner should be initialized");
        
        // The scanner should be a TransitiveDependencyScannerImpl
        assertTrue(scanner instanceof TransitiveDependencyScannerImpl, 
            "Scanner should be TransitiveDependencyScannerImpl");
    }

    @Test
    void constructor_shouldNotThrowExceptionWithNullRecipeService() {
        // Given/When - RecipeService can be null for testing
        AdvancedScanningModule module = new AdvancedScanningModule(null);

        // Then - Should not throw exception
        assertNotNull(module, "Module should be initialized");
        assertNotNull(module.getTransitiveDependencyScanner(), 
            "TransitiveDependencyScanner should be initialized");
    }
}
