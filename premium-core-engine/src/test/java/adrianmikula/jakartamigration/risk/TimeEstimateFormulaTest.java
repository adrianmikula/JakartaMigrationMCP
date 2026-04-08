package adrianmikula.jakartamigration.risk;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify the time estimate formula produces expected values.
 *
 * Expected values for average team (5 devs) and single customer:
 * - Risk 1-10: ~1 week
 * - Risk 25: 5 weeks
 * - Risk 50: 10 weeks
 * - Risk 75: 20 weeks
 * - Risk 100: 40 weeks
 */
public class TimeEstimateFormulaTest {

    private final RiskScoringService riskService = RiskScoringService.getInstance();

    @Test
    void testLowRiskWithAverageTeam() {
        // Risk 1-10 should give ~1 week for average team (5 devs, 1 customer)
        int weeksAtRisk5 = riskService.calculateMigrationTimeWeeks(5, 5, 1);
        int weeksAtRisk10 = riskService.calculateMigrationTimeWeeks(10, 5, 1);

        assertThat(weeksAtRisk5).isEqualTo(1);
        assertThat(weeksAtRisk10).isEqualTo(1);
    }

    @Test
    void testRisk25WithAverageTeam() {
        // Risk 25 should give 5 weeks for average team
        int weeks = riskService.calculateMigrationTimeWeeks(25, 5, 1);
        assertThat(weeks).isEqualTo(5);
    }

    @Test
    void testRisk50WithAverageTeam() {
        // Risk 50 should give 10 weeks for average team
        int weeks = riskService.calculateMigrationTimeWeeks(50, 5, 1);
        assertThat(weeks).isEqualTo(10);
    }

    @Test
    void testRisk75WithAverageTeam() {
        // Risk 75 should give 20 weeks for average team
        int weeks = riskService.calculateMigrationTimeWeeks(75, 5, 1);
        assertThat(weeks).isEqualTo(20);
    }

    @Test
    void testRisk100WithAverageTeam() {
        // Risk 100 should give 40 weeks for average team
        int weeks = riskService.calculateMigrationTimeWeeks(100, 5, 1);
        assertThat(weeks).isEqualTo(40);
    }

    @Test
    void testLargerTeamReducesTime() {
        // Larger team should reduce time
        int weeksWith5Devs = riskService.calculateMigrationTimeWeeks(50, 5, 1);
        int weeksWith10Devs = riskService.calculateMigrationTimeWeeks(50, 10, 1);

        assertThat(weeksWith10Devs).isLessThan(weeksWith5Devs);
        // 10 devs = half the time of 5 devs
        assertThat(weeksWith10Devs).isEqualTo(weeksWith5Devs / 2);
    }

    @Test
    void testMoreEnvironmentsIncreasesTimeLogarithmically() {
        // More environments should increase time logarithmically (diminishing returns)
        int weeksWith1Env = riskService.calculateMigrationTimeWeeks(50, 5, 1);
        int weeksWith10Envs = riskService.calculateMigrationTimeWeeks(50, 5, 10);
        int weeksWith100Envs = riskService.calculateMigrationTimeWeeks(50, 5, 100);

        // Logarithmic scale: 1 env = 1.0, 10 envs = 1.5, 100 envs = 2.0
        // So 10 envs should be 1.5x, 100 envs should be 2.0x
        assertThat(weeksWith10Envs).isGreaterThan(weeksWith1Env);
        assertThat(weeksWith100Envs).isGreaterThan(weeksWith10Envs);

        // Verify logarithmic scaling (not linear)
        // 10x increase from 1->10 gives 1.5x multiplier
        // 10x increase from 10->100 gives only additional 0.5x multiplier (diminishing returns)
        double ratio10x = (double) weeksWith10Envs / weeksWith1Env;
        double ratio100x = (double) weeksWith100Envs / weeksWith1Env;

        assertThat(ratio10x).isBetween(1.4, 1.6); // ~1.5x
        assertThat(ratio100x).isBetween(1.9, 2.1); // ~2.0x (not 10x like linear would be)
    }

    @Test
    void testDefaultValues() {
        // Default should use 5 devs and 1 customer
        int weeksWithDefaults = riskService.calculateMigrationTimeWeeks(50);
        int weeksExplicit = riskService.calculateMigrationTimeWeeks(50, 5, 1);

        assertThat(weeksWithDefaults).isEqualTo(weeksExplicit);
    }

    @Test
    void testInvalidInputsUseDefaults() {
        // Invalid inputs (<= 0) should use defaults
        int weeksWithZeroTeam = riskService.calculateMigrationTimeWeeks(50, 0, 1);
        int weeksWithZeroCustomers = riskService.calculateMigrationTimeWeeks(50, 5, 0);
        int weeksWithBothZero = riskService.calculateMigrationTimeWeeks(50, 0, 0);

        // Should use default (5, 1)
        assertThat(weeksWithZeroTeam).isEqualTo(10); // 5 devs default
        assertThat(weeksWithZeroCustomers).isEqualTo(10); // 1 customer default
        assertThat(weeksWithBothZero).isEqualTo(10); // both defaults
    }

    @Test
    void testFormattedOutput() {
        String singleWeek = riskService.formatMigrationTime(5, 5, 1);
        String multipleWeeks = riskService.formatMigrationTime(50, 5, 1);

        assertThat(singleWeek).isEqualTo("1 week");
        assertThat(multipleWeeks).isEqualTo("10 weeks");
    }
}
