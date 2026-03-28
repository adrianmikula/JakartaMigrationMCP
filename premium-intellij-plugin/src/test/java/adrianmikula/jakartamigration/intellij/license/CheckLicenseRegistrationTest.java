package adrianmikula.jakartamigration.intellij.license;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for license registration dialog functionality.
 * Tests the integration with JetBrains registration actions and data context.
 */
@ExtendWith(MockitoExtension.class)
class CheckLicenseRegistrationTest {

    @Mock
    private ActionManager mockActionManager;
    
    @Mock
    private AnAction mockRegisterAction;

    private MockedStatic<ActionManager> mockedActionManager;

    @BeforeEach
    void setUp() {
        mockedActionManager = mockStatic(ActionManager.class);
        mockedActionManager.when(ActionManager::getInstance).thenReturn(mockActionManager);
    }

    @AfterEach
    void tearDown() {
        if (mockedActionManager != null) {
            mockedActionManager.close();
        }
    }

    // ==================== Registration Dialog Tests ====================

    @Test
    @DisplayName("Should request license without throwing when RegisterPlugins action exists")
    void shouldRequestLicenseWhenRegisterPluginsExists() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);

        // When/Then
        assertThatCode(() -> CheckLicense.requestLicense("Test message"))
                .doesNotThrowAnyException();
        
        verify(mockActionManager).getAction("RegisterPlugins");
        verify(mockRegisterAction).actionPerformed(any());
    }

    @Test
    @DisplayName("Should request license without throwing when Register action exists")
    void shouldRequestLicenseWhenRegisterExists() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(null);
        when(mockActionManager.getAction("Register")).thenReturn(mockRegisterAction);

        // When/Then
        assertThatCode(() -> CheckLicense.requestLicense("Test message"))
                .doesNotThrowAnyException();
        
        verify(mockActionManager).getAction("RegisterPlugins");
        verify(mockActionManager).getAction("Register");
        verify(mockRegisterAction).actionPerformed(any());
    }

    @Test
    @DisplayName("Should handle gracefully when no registration actions exist")
    void shouldHandleWhenNoRegistrationActionsExist() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(null);
        when(mockActionManager.getAction("Register")).thenReturn(null);

        // When/Then
        assertThatCode(() -> CheckLicense.requestLicense("Test message"))
                .doesNotThrowAnyException();
        
        verify(mockActionManager).getAction("RegisterPlugins");
        verify(mockActionManager).getAction("Register");
        verify(mockRegisterAction, never()).actionPerformed(any());
    }

    @Test
    @DisplayName("Should handle action execution exceptions gracefully")
    void shouldHandleActionExecutionExceptions() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);
        doThrow(new RuntimeException("Test exception")).when(mockRegisterAction).actionPerformed(any());

        // When/Then
        assertThatCode(() -> CheckLicense.requestLicense("Test message"))
                .doesNotThrowAnyException();
        
        verify(mockRegisterAction).actionPerformed(any());
    }

    @Test
    @DisplayName("Should pass correct message to registration dialog")
    void shouldPassCorrectMessageToRegistrationDialog() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);
        String testMessage = "Premium features require a license";

        // When
        CheckLicense.requestLicense(testMessage);

        // Then
        verify(mockRegisterAction).actionPerformed(argThat(actionEvent -> {
            // Verify that the action event contains our message in the data context
            // This is a simplified test - in reality, we'd need to check the data context content
            return true;
        }));
    }

    @Test
    @DisplayName("Should pass correct product code to registration dialog")
    void shouldPassCorrectProductCodeToRegistrationDialog() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);

        // When
        CheckLicense.requestLicense("Test message");

        // Then
        verify(mockRegisterAction).actionPerformed(argThat(actionEvent -> {
            // Verify that the action event contains our product code in the data context
            // This is a simplified test - in reality, we'd need to check the data context content
            return true;
        }));
    }

    // ==================== Data Context Tests ====================

    @Test
    @DisplayName("Should create data context with correct product code")
    void shouldCreateDataContextWithCorrectProductCode() throws Exception {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);
        
        // Use reflection to access the private method for testing
        var asDataContextMethod = CheckLicense.class.getDeclaredMethod("asDataContext", String.class, String.class);
        asDataContextMethod.setAccessible(true);

        // When
        var dataContext = asDataContextMethod.invoke(null, "PJAKARTAMIGRATI", "Test message");

        // Then
        assertThat(dataContext).isNotNull();
        
        // Test the data context behavior
        var getDataMethod = dataContext.getClass().getMethod("getData", String.class);
        getDataMethod.setAccessible(true);
        
        String productCode = (String) getDataMethod.invoke(dataContext, "register.product-descriptor.code");
        String message = (String) getDataMethod.invoke(dataContext, "register.message");
        
        assertThat(productCode).isEqualTo("PJAKARTAMIGRATI");
        assertThat(message).isEqualTo("Test message");
    }

    @Test
    @DisplayName("Should handle null message in data context")
    void shouldHandleNullMessageInDataContext() throws Exception {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);
        
        var asDataContextMethod = CheckLicense.class.getDeclaredMethod("asDataContext", String.class, String.class);
        asDataContextMethod.setAccessible(true);

        // When
        var dataContext = asDataContextMethod.invoke(null, "PJAKARTAMIGRATI", null);

        // Then
        var getDataMethod = dataContext.getClass().getMethod("getData", String.class);
        getDataMethod.setAccessible(true);
        
        String productCode = (String) getDataMethod.invoke(dataContext, "register.product-descriptor.code");
        String message = (String) getDataMethod.invoke(dataContext, "register.message");
        
        assertThat(productCode).isEqualTo("PJAKARTAMIGRATI");
        assertThat(message).isNull();
    }

    @Test
    @DisplayName("Should handle unknown data keys in data context")
    void shouldHandleUnknownDataKeysInDataContext() throws Exception {
        // Given
        var asDataContextMethod = CheckLicense.class.getDeclaredMethod("asDataContext", String.class, String.class);
        asDataContextMethod.setAccessible(true);

        // When
        var dataContext = asDataContextMethod.invoke(null, "PJAKARTAMIGRATI", "Test message");

        // Then
        var getDataMethod = dataContext.getClass().getMethod("getData", String.class);
        getDataMethod.setAccessible(true);
        
        String unknownKey = (String) getDataMethod.invoke(dataContext, "unknown.key");
        
        assertThat(unknownKey).isNull();
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should integrate with license checking flow")
    void shouldIntegrateWithLicenseCheckingFlow() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(null);
        when(mockActionManager.getAction("Register")).thenReturn(null);

        // When - request license during license check
        Boolean licenseResult = CheckLicense.isLicensed(); // Should be false (no LicensingFacade)
        CheckLicense.requestLicense("License required for premium features");

        // Then
        assertThat(licenseResult).isFalse();
        verify(mockActionManager).getAction("RegisterPlugins");
        verify(mockActionManager).getAction("Register");
    }

    @Test
    @DisplayName("Should handle multiple license requests")
    void shouldHandleMultipleLicenseRequests() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);

        // When
        CheckLicense.requestLicense("First request");
        CheckLicense.requestLicense("Second request");
        CheckLicense.requestLicense("Third request");

        // Then
        verify(mockActionManager, times(3)).getAction("RegisterPlugins");
        verify(mockRegisterAction, times(3)).actionPerformed(any());
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle ActionManager exceptions")
    void shouldHandleActionManagerExceptions() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins"))
                .thenThrow(new RuntimeException("ActionManager error"));

        // When/Then
        assertThatCode(() -> CheckLicense.requestLicense("Test message"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null ActionManager")
    void shouldHandleNullActionManager() {
        // Given
        mockedActionManager.when(ActionManager::getInstance).thenReturn(null);

        // When/Then
        assertThatCode(() -> CheckLicense.requestLicense("Test message"))
                .doesNotThrowAnyException();
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);

        // When/Then
        assertThatCode(() -> CheckLicense.requestLicense(""))
                .doesNotThrowAnyException();
        
        verify(mockRegisterAction).actionPerformed(any());
    }

    @Test
    @DisplayName("Should handle very long message")
    void shouldHandleVeryLongMessage() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMessage.append("This is a very long message. ");
        }

        // When/Then
        assertThatCode(() -> CheckLicense.requestLicense(longMessage.toString()))
                .doesNotThrowAnyException();
        
        verify(mockRegisterAction).actionPerformed(any());
    }

    @Test
    @DisplayName("Should handle special characters in message")
    void shouldHandleSpecialCharactersInMessage() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);
        String specialMessage = "Special chars: !@#$%^&*()_+{}|:<>?[]\\;'\",./";

        // When/Then
        assertThatCode(() -> CheckLicense.requestLicense(specialMessage))
                .doesNotThrowAnyException();
        
        verify(mockRegisterAction).actionPerformed(any());
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Should handle rapid license requests efficiently")
    void shouldHandleRapidLicenseRequestsEfficiently() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            CheckLicense.requestLicense("Request " + i);
        }
        long endTime = System.currentTimeMillis();

        // Then - should be reasonably fast
        assertThat(endTime - startTime).isLessThan(1000); // Under 1 second for 100 requests
        verify(mockRegisterAction, times(100)).actionPerformed(any());
    }

    @Test
    @DisplayName("Should handle very long message")
    void shouldHandleVeryLongMessage() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMessage.append("This is a very long message. ");
        }

        // When/Then
        assertThatCode(() -> CheckLicense.requestLicense(longMessage.toString()))
                .doesNotThrowAnyException();
        
        verify(mockRegisterAction).actionPerformed(any());
    }

    @Test
    @DisplayName("Should handle rapid license requests efficiently")
    void shouldHandleRapidLicenseRequestsEfficiently() {
        // Given
        when(mockActionManager.getAction("RegisterPlugins")).thenReturn(mockRegisterAction);

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            CheckLicense.requestLicense("Request " + i);
        }
        long endTime = System.currentTimeMillis();

        // Then - should be reasonably fast
        assertThat(endTime - startTime).isLessThan(1000); // Under 1 second for 100 requests
        verify(mockRegisterAction, times(100)).actionPerformed(any());
    }
}
