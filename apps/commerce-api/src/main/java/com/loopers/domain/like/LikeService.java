package com.loopers.domain.like;

import java.util.List;
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

    public List<Long> findLikedProductIds(Long userId) {
        return likeRepository.findProductIdsByUserId(userId);
    }

}
