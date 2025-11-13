package com.loopers.domain.like;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor

@Service
public class LikeService {

    private final LikeRepository likeRepository;

    public void recordLike(Long userId, Long productId) {
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        likeRepository.save(userId, productId);
    }

    public void cancelLike(Long userId, Long productId) {
        if (!likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        likeRepository.delete(userId, productId);
    }

    public List<Long> findLikedProductIds(Long userId) {
        return likeRepository.findProductIdsByUserId(userId);
    }

    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return likeRepository.existsByUserIdAndProductId(userId, productId);
    }

}
