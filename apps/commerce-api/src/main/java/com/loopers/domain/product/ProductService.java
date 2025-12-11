package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Optional<Product> findProductById(Long productId) {
        return productRepository.findProductById(productId);
    }

    public Product getProductById(Long productId) {
        return productRepository.findProductById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    @Transactional
    public void reduceStock(Product product, Integer quantity) {
        product.reduceStock(quantity);
        productRepository.saveProduct(product);
    }

    /**
     * 재고를 차감합니다. 재고가 부족한 경우 CoreException을 던집니다.
     * @param productId 상품 ID
     * @param quantity 차감할 수량
     * @throws CoreException 재고가 부족한 경우
     */
    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = getProductById(productId);
        product.reduceStock(quantity);
        productRepository.saveProduct(product);
    }

    public void restoreStock(Product product, Integer quantity) {
        product.restoreStock(quantity);
    }

    public List<Product> findProductsByLatest(Long brandId, int page, int size) {
        return productRepository.findProductsByLatest(brandId, page, size);
    }

    public List<Product> findProductsByPriceAsc(Long brandId, int page, int size) {
        return productRepository.findProductsByPriceAsc(brandId, page, size);
    }

    public List<Product> findProductsByLikesDesc(Long brandId, int page, int size) {
        return productRepository.findProductsByLikesDesc(brandId, page, size);
    }

    public Product increaseLikeCount(Long productId) {
        Product product = productRepository.findProductById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        product.increaseLikeCount();
        return product;
    }

    public Product decreaseLikeCount(Long productId) {
        Product product = productRepository.findProductById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        product.decreaseLikeCount();
        return product;
    }
}
