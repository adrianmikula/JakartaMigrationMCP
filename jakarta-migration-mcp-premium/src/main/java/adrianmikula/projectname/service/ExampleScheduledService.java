package adrianmikula.projectname.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Example scheduled task service.
 * This is a template example - replace with your own scheduled tasks.
 */
@Service
@Slf4j
public class ExampleScheduledService {
    
    /**
     * Example scheduled task - runs every 5 minutes.
     * Use @Scheduled with fixedDelay, fixedRate, or cron expressions.
     */
    @Scheduled(fixedDelayString = "${app.example.task.interval-ms:300000}", initialDelay = 30000)
    public void scheduledTask() {
        log.info("Example scheduled task executed");
        // Add your scheduled task logic here
    }
}

