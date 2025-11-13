package com.loopers.domain.like;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor

@Service
public class LikeService {

    private final LikeRepository likeRepository;

    public void recordLike(Long userId, Long productId) {
        likeRepository.save(userId, productId);
    }

    public void cancelLike(Long userId, Long productId) {
        likeRepository.delete(userId, productId);
    }

}
