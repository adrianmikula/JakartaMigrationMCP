package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ResultComparator {

    public ComparisonReport compare(ExperimentResult a, ExperimentResult b) {
        String comparisonId = UUID.randomUUID().toString();
        String recommended = determineRecommended(a, b);

        List<ComparisonReport.ComparisonReason> reasons = new ArrayList<>();
        reasons.add(compareMetric("testFailures", safeFailures(a), safeFailures(b), a, b));
        reasons.add(compareMetric("filesChanged", safeFilesChanged(a), safeFilesChanged(b), a, b));
        reasons.add(compareMetric("status", a.status().name(), b.status().name(), a, b));

        List<ComparisonReport.StepDivergence> stepDivergence = new ArrayList<>();
        int maxSteps = Math.max(a.stepResults().size(), b.stepResults().size());
        for (int i = 0; i < maxSteps; i++) {
            StepResult stepA = i < a.stepResults().size() ? a.stepResults().get(i) : null;
            StepResult stepB = i < b.stepResults().size() ? b.stepResults().get(i) : null;
            if (stepA != null && stepB != null && (stepA.success() != stepB.success() || !Objects.equals(stepA.message(), stepB.message()))) {
                stepDivergence.add(new ComparisonReport.StepDivergence(i, stepA, stepB));
            }
        }

        return new ComparisonReport(
            comparisonId,
            new ComparisonReport.ExperimentResultSummary(a.runId(), a.sequenceName(), a.status()),
            new ComparisonReport.ExperimentResultSummary(b.runId(), b.sequenceName(), b.status()),
            recommended,
            reasons,
            stepDivergence
        );
    }

    private String determineRecommended(ExperimentResult a, ExperimentResult b) {
        if (a.isSuccess() && !b.isSuccess()) return "A";
        if (!a.isSuccess() && b.isSuccess()) return "B";
        if (!a.isSuccess() && !b.isSuccess()) return "A";

        int aFailures = safeFailures(a);
        int bFailures = safeFailures(b);
        if (aFailures != bFailures) return aFailures <= bFailures ? "A" : "B";

        int aFiles = safeFilesChanged(a);
        int bFiles = safeFilesChanged(b);
        return aFiles <= bFiles ? "A" : "B";
    }

    private ComparisonReport.ComparisonReason compareMetric(String metric, Object valueA, Object valueB, ExperimentResult a, ExperimentResult b) {
        String winner;
        if (metric.equals("testFailures")) {
            int av = ((Number) valueA).intValue();
            int bv = ((Number) valueB).intValue();
            winner = av <= bv ? "A" : "B";
        } else if (metric.equals("filesChanged")) {
            int av = ((Number) valueA).intValue();
            int bv = ((Number) valueB).intValue();
            winner = av <= bv ? "A" : "B";
        } else if (metric.equals("status")) {
            winner = (a.isSuccess() && !b.isSuccess()) ? "A" : (!a.isSuccess() && b.isSuccess()) ? "B" : "tie";
        } else {
            winner = "tie";
        }
        return new ComparisonReport.ComparisonReason(metric, valueA, valueB, winner);
    }

    private int safeFailures(ExperimentResult result) {
        if (result.testOutcome() == null) return Integer.MAX_VALUE;
        return result.testOutcome().failed();
    }

    private int safeFilesChanged(ExperimentResult result) {
        if (result.stepResults() == null || result.stepResults().isEmpty()) return 0;
        return result.stepResults().stream()
            .mapToInt(step -> Math.max(0, step.filesChanged()))
            .sum();
    }
}
