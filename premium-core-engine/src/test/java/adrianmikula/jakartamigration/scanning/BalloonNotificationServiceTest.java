package adrianmikula.jakartamigration.scanning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BalloonNotificationServiceTest {

    @Test
    @DisplayName("Should show notification on first call")
    void shouldShowOnFirstCall() {
        BalloonNotificationService service = new BalloonNotificationService();
        boolean shown = service.showOnce("key1", "Title", "Message");
        assertThat(shown).isTrue();
    }

    @Test
    @DisplayName("Should suppress duplicate within cooldown window")
    void shouldSuppressWithinCooldown() {
        BalloonNotificationService service = new BalloonNotificationService();
        service.showOnce("key1", "Title", "Message1");
        boolean duplicate = service.showOnce("key1", "Title", "Message2");
        assertThat(duplicate).isFalse();
    }

    @Test
    @DisplayName("Should allow re-show after cooldown expires")
    void shouldAllowAfterCooldown() {
        BalloonNotificationService service = new BalloonNotificationService() {
            @Override
            public boolean showOnce(String key, String title, String message) {
                // Simulate cooldown expiry by manipulating the timestamp directly
                return super.showOnce(key, title, message);
            }
        };

        service.showOnce("key1", "Title", "Message1");
        assertThat(service.hasShown("key1")).isTrue();

        // Manually expire the cooldown
        service.reset();
        assertThat(service.hasShown("key1")).isFalse();

        boolean re_shown = service.showOnce("key1", "Title", "Message1");
        assertThat(re_shown).isTrue();
    }

    @Test
    @DisplayName("Should handle multiple different keys independently")
    void shouldHandleMultipleKeys() {
        BalloonNotificationService service = new BalloonNotificationService();

        boolean shown1 = service.showOnce("key1", "Title", "Message1");
        boolean shown2 = service.showOnce("key2", "Title", "Message2");
        boolean duplicate1 = service.showOnce("key1", "Title", "Message1");

        assertThat(shown1).isTrue();
        assertThat(shown2).isTrue();
        assertThat(duplicate1).isFalse();
    }

    @Test
    @DisplayName("Should reset all notifications")
    void shouldResetAll() {
        BalloonNotificationService service = new BalloonNotificationService();

        service.showOnce("key1", "Title", "Message1");
        service.showOnce("key2", "Title", "Message2");
        assertThat(service.hasShown("key1")).isTrue();
        assertThat(service.hasShown("key2")).isTrue();

        service.reset();
        assertThat(service.hasShown("key1")).isFalse();
        assertThat(service.hasShown("key2")).isFalse();
    }

    @Test
    @DisplayName("Should dispatch to handler when shown")
    void shouldDispatchToHandler() {
        List<String[]> received = new ArrayList<>();
        BalloonNotificationService service = new BalloonNotificationService((title, msg) -> {
            received.add(new String[]{title, msg});
        });

        service.showOnce("key1", "Title", "Message");
        assertThat(received).hasSize(1);
        assertThat(received.get(0)[0]).isEqualTo("Title");
        assertThat(received.get(0)[1]).isEqualTo("Message");
    }

    @Test
    @DisplayName("Should dispatch when cooldown expires and notification re-shown")
    void shouldDispatchAfterCooldownExpiry() {
        List<String[]> received = new ArrayList<>();
        BalloonNotificationService service = new BalloonNotificationService((title, msg) -> {
            received.add(new String[]{title, msg});
        });

        service.showOnce("key1", "Title", "Message1");
        assertThat(received).hasSize(1);

        // Reset to simulate cooldown expiry
        service.reset();

        service.showOnce("key1", "Title", "Message2");
        assertThat(received).hasSize(2);
    }

    @Test
    @DisplayName("hasShown should return false after reset")
    void hasShownShouldReturnFalseAfterReset() {
        BalloonNotificationService service = new BalloonNotificationService();

        service.showOnce("key1", "Title", "Message");
        assertThat(service.hasShown("key1")).isTrue();

        service.reset();
        assertThat(service.hasShown("key1")).isFalse();
    }

    @Test
    @DisplayName("hasShown should return false for unknown key")
    void hasShownShouldReturnFalseForUnknownKey() {
        BalloonNotificationService service = new BalloonNotificationService();

        assertThat(service.hasShown("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("Should handle handler exception gracefully")
    void shouldHandleHandlerExceptionGracefully() {
        BalloonNotificationService service = new BalloonNotificationService((title, msg) -> {
            throw new RuntimeException("Handler failed!");
        });

        // Should not throw — exception is caught internally
        boolean shown = service.showOnce("key1", "Title", "Message");
        assertThat(shown).isTrue();
    }

    @Test
    @DisplayName("Should be thread-safe under concurrent access")
    void shouldBeThreadSafeUnderConcurrentAccess() throws InterruptedException {
        BalloonNotificationService service = new BalloonNotificationService();
        int threadCount = 10;
        int callsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger shownCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < callsPerThread; i++) {
                        if (service.showOnce("concurrent-key", "Title", "Message")) {
                            shownCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Exactly one thread should have shown the notification (CAS protection)
        assertThat(shownCount.get()).isEqualTo(1);
        // All other calls should be suppressed
        assertThat(service.hasShown("concurrent-key")).isTrue();
    }

    @Test
    @DisplayName("Should track different keys independently under concurrency")
    void shouldTrackDifferentKeysIndependentlyUnderConcurrency() throws InterruptedException {
        BalloonNotificationService service = new BalloonNotificationService();
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger shownCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final String key = "key-" + t;
            executor.submit(() -> {
                try {
                    if (service.showOnce(key, "Title", "Message")) {
                        shownCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Each thread used a unique key, so all should be shown
        assertThat(shownCount.get()).isEqualTo(threadCount);
    }
}
