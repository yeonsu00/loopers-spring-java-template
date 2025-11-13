package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "likes")
@Getter
public class Like extends BaseEntity {

    private Long userId;

    private Long productId;

    protected Like() {
    }

    private Like(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public static Like createLike(Long userId, Long productId) {
        return new Like(userId, productId);
    }
}
