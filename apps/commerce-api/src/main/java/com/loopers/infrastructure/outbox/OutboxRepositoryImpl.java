package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.Outbox;
import com.loopers.domain.outbox.OutboxRepository;
import com.loopers.domain.outbox.OutboxStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Repository
public class OutboxRepositoryImpl implements OutboxRepository {

    private final OutboxJpaRepository outboxJpaRepository;

    @Override
    public void saveOutbox(Outbox outbox) {
        outboxJpaRepository.save(outbox);
    }

    @Override
    public List<Outbox> findPendingOutboxes(int limit) {
        return outboxJpaRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
                .stream()
                .limit(limit)
                .toList();
    }

    @Override
    public List<Outbox> findFailedAndRetryableOutboxes(int limit, int maxRetries) {
        return outboxJpaRepository.findFailedOutboxesForRetry(OutboxStatus.FAILED, maxRetries)
                .stream()
                .limit(limit)
                .toList();
    }

    @Override
    @Transactional
    public void markAsPublished(Long id) {
        outboxJpaRepository.updateOutboxPublishedStatus(id);
    }

    @Override
    @Transactional
    public void markAsFailed(Long id, String message) {
        outboxJpaRepository.updateOutboxFailedStatusAndErrorMessageAndRetryCount(id, message);
    }
}

