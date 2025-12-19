package com.loopers.interfaces.scheduler;

import com.loopers.domain.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OutboxRelayScheduler {

    private final OutboxService outboxService;
    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 3000)
    public void relayOutboxEvents() {
        outboxService.publishPendingOutboxes(BATCH_SIZE);
    }

    @Scheduled(fixedDelay = 5000)
    public void retryFailedOutboxEvents() {
        outboxService.retryFailedOutboxes(BATCH_SIZE);
    }
}

