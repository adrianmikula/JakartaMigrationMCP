package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.intellij.ui.ScanProgressListener;
import adrianmikula.jakartamigration.intellij.ui.ThrottledProgressListener;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.assertj.core.api.Assertions;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;

/**
 * Performance tests verifying that scan operations do not block or freeze the IDE UI.
 *
 * <p>Scans run on background threads via CompletableFuture chains and bounded thread pools.
 * The EDT (Event Dispatch Thread) must remain responsive throughout all scan phases so users
 * can continue interacting with the IDE while scans execute.</p>
 *
 * @see adrianmikula.jakartamigration.scanning.DependencyAnalysisPipelinePerformanceTest
 */
public class ScanUiNonBlockingPerformanceTest extends BasePlatformTestCase {

    private AdvancedScanningService advancedScanningService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        RecipeService recipeService = mock(RecipeService.class);
        advancedScanningService = new AdvancedScanningService(recipeService);
        createTestProjectFiles();
    }

    /**
     * Verifies that scanAll() does not block the EDT.
     *
     * <p>A heartbeat task is posted to the EDT via {@link SwingUtilities#invokeAndWait} while
     * scanAll() executes on a background thread. If the scan blocks the EDT, the heartbeat
     * will take longer than the budget.</p>
     */
    public void testScanAllDoesNotBlockEdt() throws Exception {
        Path projectPath = Paths.get(getProject().getBasePath());
        long edtBudgetMs = 2000;
        AtomicBoolean edtTaskCompleted = new AtomicBoolean(false);
        CountDownLatch scanStarted = new CountDownLatch(1);

        CompletableFuture<AdvancedScanningService.AdvancedScanSummary> scanFuture =
                CompletableFuture.supplyAsync(() -> {
                    scanStarted.countDown();
                    return advancedScanningService.scanAll(projectPath);
                });

        scanStarted.await(5, TimeUnit.SECONDS);

        long edtStart = System.currentTimeMillis();
        SwingUtilities.invokeAndWait(() -> edtTaskCompleted.set(true));
        long edtDuration = System.currentTimeMillis() - edtStart;

        Assertions.assertThat(edtTaskCompleted.get()).isTrue();
        Assertions.assertThat(edtDuration)
                .as("EDT heartbeat must complete within %dms during scan", edtBudgetMs)
                .isLessThan(edtBudgetMs);

        AdvancedScanningService.AdvancedScanSummary summary = scanFuture.get(60, TimeUnit.SECONDS);
        Assertions.assertThat(summary).isNotNull();
    }

    /**
     * Verifies that a full scanAll() completes within a reasonable wall-clock budget.
     */
    public void testScanAllCompletesWithinBudget() throws Exception {
        Path projectPath = Paths.get(getProject().getBasePath());
        long budgetMs = 30_000;

        long start = System.currentTimeMillis();
        AdvancedScanningService.AdvancedScanSummary summary =
                advancedScanningService.scanAll(projectPath);
        long duration = System.currentTimeMillis() - start;

        Assertions.assertThat(summary).isNotNull();
        Assertions.assertThat(duration)
                .as("scanAll() must complete within %dms", budgetMs)
                .isLessThan(budgetMs);
    }

    /**
     * Verifies that {@link ThrottledProgressListener} does not introduce excessive overhead
     * when handling rapid progress updates.
     */
    public void testThrottledListenerDoesNotIntroduceExcessiveOverhead() throws Exception {
        int callbackCount = 100;
        long dispatchBudgetMs = 500;

        AtomicInteger delegatePhaseCalls = new AtomicInteger(0);
        ScanProgressListener delegate = new ScanProgressListener() {
            @Override
            public void onScanPhase(String phase, int completed, int total) {
                delegatePhaseCalls.incrementAndGet();
            }

            @Override
            public void onScanComplete() {}

            @Override
            public void onScanError(Exception error) {}
        };

        ThrottledProgressListener throttled = new ThrottledProgressListener(delegate, 10, 5);

        long start = System.currentTimeMillis();
        for (int i = 0; i < callbackCount; i++) {
            throttled.onScanPhase("Phase " + i, i, callbackCount);
        }
        throttled.shutdown();
        long duration = System.currentTimeMillis() - start;

        Assertions.assertThat(duration)
                .as("Throttled listener dispatch of %d callbacks must complete within %dms",
                        callbackCount, dispatchBudgetMs)
                .isLessThan(dispatchBudgetMs);

        Assertions.assertThat(delegatePhaseCalls.get())
                .as("Delegate should receive at least 1 phase update")
                .isGreaterThanOrEqualTo(1);
    }

    /**
     * Verifies that scanAll() and concurrent EDT tasks do not interfere with each other.
     *
     * <p>Posts 50 tasks to the EDT via invokeAndWait while scanAll() runs on a background
     * thread. No deadlocks or missed tasks should occur.</p>
     */
    public void testConcurrentEdtAndScanDoNotInterfere() throws Exception {
        Path projectPath = Paths.get(getProject().getBasePath());
        int edtTaskCount = 50;
        AtomicInteger edtTasksCompleted = new AtomicInteger(0);
        AtomicBoolean scanCompleted = new AtomicBoolean(false);
        CountDownLatch scanStarted = new CountDownLatch(1);

        CompletableFuture<Void> scanFuture = CompletableFuture.runAsync(() -> {
            scanStarted.countDown();
            advancedScanningService.scanAll(projectPath);
            scanCompleted.set(true);
        });

        scanStarted.await(5, TimeUnit.SECONDS);

        for (int i = 0; i < edtTaskCount; i++) {
            SwingUtilities.invokeAndWait(() -> edtTasksCompleted.incrementAndGet());
        }

        scanFuture.get(60, TimeUnit.SECONDS);

        Assertions.assertThat(edtTasksCompleted.get())
                .as("All %d EDT tasks must complete", edtTaskCount)
                .isEqualTo(edtTaskCount);
        Assertions.assertThat(scanCompleted.get()).isTrue();
    }

    private void createTestProjectFiles() throws IOException {
        Path basePath = Path.of(getProject().getBasePath());
        Path srcDir = basePath.resolve("src/main/java/com/example");

        Files.createDirectories(srcDir);

        Files.writeString(srcDir.resolve("UserService.java"), """
                package com.example;

                import javax.servlet.http.HttpServletRequest;
                import javax.persistence.Entity;
                import javax.validation.constraints.NotNull;

                @Entity
                public class UserService {
                    @NotNull
                    private String name;

                    public void handleRequest(HttpServletRequest request) {
                        this.name = request.getParameter("name");
                    }
                }
                """);

        Files.writeString(srcDir.resolve("OrderService.java"), """
                package com.example;

                import javax.servlet.http.HttpServletResponse;
                import javax.persistence.Column;
                import javax.inject.Inject;

                @javax.persistence.Entity
                public class OrderService {
                    @Inject
                    private UserService userService;

                    @Column(name = "order_id")
                    private Long orderId;

                    public void process(HttpServletResponse response) {
                        // business logic
                    }
                }
                """);

        Files.writeString(srcDir.resolve("PaymentProcessor.java"), """
                package com.example;

                import javax.ws.rs.GET;
                import javax.ws.rs.Path;
                import javax.ejb.Stateless;

                @Stateless
                @Path("/payment")
                public class PaymentProcessor {
                    @GET
                    public String processPayment() {
                        return "ok";
                    }
                }
                """);

        Files.writeString(srcDir.resolve("ReportGenerator.java"), """
                package com.example;

                import javax.servlet.http.HttpSession;
                import javax.validation.Valid;

                public class ReportGenerator {
                    public void generate(@Valid HttpSession session) {
                        // report logic
                    }
                }
                """);

        Files.writeString(srcDir.resolve("EmailService.java"), """
                package com.example;

                import javax.jms.Connection;
                import javax.persistence.PersistenceContext;
                import javax.annotation.Resource;

                public class EmailService {
                    @Resource
                    private Connection connection;

                    @PersistenceContext
                    private javax.persistence.EntityManager em;

                    public void sendEmail() {
                        // email logic
                    }
                }
                """);
    }
}
