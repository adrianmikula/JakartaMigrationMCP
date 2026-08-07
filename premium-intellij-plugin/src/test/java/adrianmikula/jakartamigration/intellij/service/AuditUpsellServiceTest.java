package adrianmikula.jakartamigration.intellij.service;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the dial-based audit upsell CTA logic.
 */
class AuditUpsellServiceTest {

    @Test
    void shouldShowCta_returnsFalseWhenAllDialsAreSafe() {
        assertFalse(AuditUpsellService.shouldShowCta(0, 0, 100),
                "All dials safe should not trigger CTA");
    }

    @Test
    void shouldShowCta_returnsTrueWhenComplexityHigh() {
        assertTrue(AuditUpsellService.shouldShowCta(70, 0, 100),
                "Complexity at the high threshold should trigger CTA");
    }

    @Test
    void shouldShowCta_returnsTrueWhenRiskHigh() {
        assertTrue(AuditUpsellService.shouldShowCta(0, 70, 100),
                "Risk at the high threshold should trigger CTA");
    }

    @Test
    void shouldShowCta_returnsTrueWhenAutomationLow() {
        assertTrue(AuditUpsellService.shouldShowCta(0, 0, 30),
                "Automation at the low threshold should trigger CTA");
    }

    @Test
    void shouldShowCta_returnsFalseJustBelowThresholds() {
        assertFalse(AuditUpsellService.shouldShowCta(69, 69, 31),
                "Values just below thresholds should not trigger CTA");
    }

    @Test
    void createCtaWarningPanel_returnsNullWhenNoThresholdExceeded() {
        assertNull(AuditUpsellService.createCtaWarningPanel(null, 10, 10, 100),
                "No warning box when all dials are safe");
    }

    @Test
    void createCtaWarningPanel_returnsPanelWhenAnyDialExceedsThreshold() {
        JPanel panel = AuditUpsellService.createCtaWarningPanel(null, 75, 10, 100);
        assertNotNull(panel, "Warning box should be created when a threshold is exceeded");
        assertTrue(panel.getComponentCount() > 0, "Warning box should contain explanation and CTA");
    }

    @Test
    void getLandingPageUrl_loadedFromConfig() {
        assertEquals("https://codemedicconsulting.netlify.app/jakarta-migration-audit",
                AuditUpsellService.getLandingPageUrl());
    }

    @Test
    void getCtaButtonText_loadedFromConfig() {
        assertEquals("Get the $99 Audit", AuditUpsellService.getCtaButtonText());
    }
}
