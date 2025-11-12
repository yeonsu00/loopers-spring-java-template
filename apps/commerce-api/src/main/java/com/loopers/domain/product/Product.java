package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "products")
@Getter
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long brandId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "price", column = @Column(name = "price", nullable = false, unique = true))
    })
    private Price price;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "count", column = @Column(name = "like_count", nullable = false, unique = true))
    })
    private LikeCount likeCount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "quantity", column = @Column(name = "stock", nullable = false, unique = true))
    })
    private Stock stock;

    private boolean isDeleted;


}
