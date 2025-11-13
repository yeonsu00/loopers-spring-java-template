package com.loopers.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class Price {

    @Column(nullable = false)
    private Integer price;

}
