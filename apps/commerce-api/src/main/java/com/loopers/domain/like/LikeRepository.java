package com.loopers.domain.like;

import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository {
    void save(Long userId, Long productId);

    void delete(Long userId, Long productId);
}
