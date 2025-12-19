package com.loopers.domain.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxService {

    private static final int MAX_RETRIES = 3;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveOutbox(String topic, String partitionKey, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            Outbox outbox = Outbox.create(topic, partitionKey, payload);
            outboxRepository.saveOutbox(outbox);
        } catch (Exception e) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "이벤트 저장에 실패했습니다.");
        }
    }

    public void publishPendingOutboxes(int batchSize) {
        List<Outbox> pendingOutboxes = outboxRepository.findPendingOutboxes(batchSize);

        for (Outbox outbox : pendingOutboxes) {
            kafkaTemplate
                    .send(
                            outbox.getTopic(),
                            outbox.getPartitionKey(),
                            outbox.getPayload()
                    )
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            outboxRepository.markAsPublished(outbox.getId());
                        } else {
                            outboxRepository.markAsFailed(outbox.getId(), exception.getMessage());
                        }
                    });
        }
    }

    public void retryFailedOutboxes(int batchSize) {
        List<Outbox> failedOutboxes = outboxRepository.findFailedAndRetryableOutboxes(batchSize, MAX_RETRIES);

        for (Outbox outbox : failedOutboxes) {
            if (outbox.hasExceededMaxRetries(MAX_RETRIES)) {
                log.warn("Outbox 재시도 횟수 초과: outboxId={}, retryCount={}, topic={}",
                        outbox.getId(), outbox.getRetryCount(), outbox.getTopic());
                continue;
            }

            kafkaTemplate
                    .send(
                            outbox.getTopic(),
                            outbox.getPartitionKey(),
                            outbox.getPayload()
                    )
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            outboxRepository.markAsPublished(outbox.getId());
                        } else {
                            outboxRepository.markAsFailed(outbox.getId(), exception.getMessage());
                        }
                    });
        }
    }
}

