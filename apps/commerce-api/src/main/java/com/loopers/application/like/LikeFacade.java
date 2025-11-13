package com.loopers.application.like;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class LikeFacade {

    private final UserService userService;
    private final LikeService likeService;
    private final ProductService productService;

    @Transactional
    public LikeInfo recordLike(LikeCommand.LikeProductCommand command) {
        User user = userService.findUserByLoginId(command.loginId())
                .orElseThrow(() -> new RuntimeException(command.loginId() + " 사용자를 찾을 수 없습니다."));

        Product product = productService.increaseLikeCount(command.productId());
        likeService.recordLike(user.getId(), product.getId());

        return LikeInfo.from(
                product.getId(),
                product.getLikeCount().getCount()
        );
    }

}
