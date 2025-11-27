package com.loopers.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductInfo;
import com.loopers.application.product.ProductSort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
public class ProductCacheService {

    private static final String PRODUCT_DETAIL_PREFIX = "product:detail:";
    private static final String PRODUCT_LIST_PREFIX = "product:list:";
    
    private static final Duration PRODUCT_DETAIL_TTL = Duration.ofHours(6);
    private static final Duration PRODUCT_LIST_TTL = Duration.ofMinutes(10);
    private static final long REFRESH_THRESHOLD_SECONDS = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Object> redisTemplateMaster;
    private final ObjectMapper objectMapper;
    private final DistributedLock distributedLock;

    public ProductCacheService(
            @Qualifier("redisTemplateObject") RedisTemplate<String, Object> redisTemplate,
            @Qualifier("redisTemplateObjectMaster") RedisTemplate<String, Object> redisTemplateMaster,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            DistributedLock distributedLock
    ) {
        this.redisTemplate = redisTemplate;
        this.redisTemplateMaster = redisTemplateMaster;
        this.objectMapper = objectMapper;
        this.distributedLock = distributedLock;
    }

    /**
     * 상품 상세 캐시 키 생성
     */
    public String getProductDetailKey(Long productId) {
        return PRODUCT_DETAIL_PREFIX + productId;
    }

    /**
     * 상품 목록 캐시 키 생성
     */
    public String getProductListKey(Long brandId, ProductSort sort, int page, int size) {
        String brandPart = brandId != null ? "brandId=" + brandId : "brandId=all";
        String sortPart = "sort=" + sort.getValue();
        String pagePart = "page=" + page;
        String sizePart = "size=" + size;
        return PRODUCT_LIST_PREFIX + brandPart + "&" + sortPart + "&" + pagePart + "&" + sizePart;
    }

    /**
     * 상품 상세 조회 (Look-Aside, Stale-While-Revalidate)
     */
    public ProductInfo getProduct(Long productId, Supplier<ProductInfo> dbSupplier) {
        String cacheKey = getProductDetailKey(productId);
        ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();

        // 1. 캐시 조회
        Object cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) {
            log.info("상품 상세 캐시 히트: productId={}", productId);
            ProductInfo productInfo = objectMapper.convertValue(cachedValue, ProductInfo.class);

            // 2. Stale-While-Revalidate
            Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
            if (ttl != null && ttl < REFRESH_THRESHOLD_SECONDS) {
                log.info("키 {}의 TTL이 임계값 이하입니다. 비동기 새로고침을 실행합니다.", cacheKey);
                refreshProductCache(productId, cacheKey, dbSupplier, PRODUCT_DETAIL_TTL);
            }
            return productInfo;
        }

        // 3. 캐시 미스 -> DB 조회 및 캐시 저장
        log.info("상품 상세 캐시 미스: productId={}, DB에서 조회합니다.", productId);
        ProductInfo productInfo = dbSupplier.get();
        redisTemplateMaster.opsForValue().set(cacheKey, productInfo, PRODUCT_DETAIL_TTL);
        return productInfo;
    }

    /**
     * 상품 목록 조회 (Look-Aside, Stale-While-Revalidate)
     */
    public List<ProductInfo> getProductList(Long brandId, ProductSort sort, int page, int size, Supplier<List<ProductInfo>> dbSupplier) {
        String cacheKey = getProductListKey(brandId, sort, page, size);
        ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();

        // 1. 캐시 조회
        Object cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) {
            log.info("상품 목록 캐시 히트: cacheKey={}", cacheKey);
            List<ProductInfo> productList = objectMapper.convertValue(cachedValue, new TypeReference<List<ProductInfo>>() {});

            // 2. Stale-While-Revalidate
            Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
            if (ttl != null && ttl < REFRESH_THRESHOLD_SECONDS) {
                log.info("키 {}의 TTL이 임계값 이하입니다. 비동기 새로고침을 실행합니다.", cacheKey);
                refreshProductListCache(cacheKey, dbSupplier, PRODUCT_LIST_TTL);
            }
            return productList;
        }

        // 3. 캐시 미스 -> DB 조회 및 캐시 저장
        log.info("상품 목록 캐시 미스: cacheKey={}, DB에서 조회합니다.", cacheKey);
        List<ProductInfo> productList = dbSupplier.get();
        redisTemplateMaster.opsForValue().set(cacheKey, productList, PRODUCT_LIST_TTL);
        return productList;
    }

    /**
     * 비동기로 상품 상세 캐시 갱신
     */
    @Async("cacheRefreshExecutor")
    public void refreshProductCache(Long productId, String cacheKey, Supplier<ProductInfo> dbSupplier, Duration ttl) {
        String lockKey = "lock:" + cacheKey;
        DistributedLock.LockHandle lockHandle = distributedLock.tryLock(lockKey, Duration.ofSeconds(1), Duration.ofSeconds(10));

        if (lockHandle == null) {
            log.debug("다른 스레드가 이미 캐시를 갱신 중입니다: cacheKey={}", cacheKey);
            return;
        }

        try {
            log.info("캐시 갱신을 위한 락 획득: cacheKey={}", cacheKey);
            ProductInfo productInfo = dbSupplier.get();
            redisTemplateMaster.opsForValue().set(cacheKey, productInfo, ttl);
            log.info("상품 상세 캐시 갱신 완료: productId={}", productId);
        } catch (Exception e) {
            log.error("캐시 갱신 실패: cacheKey={}", cacheKey, e);
        } finally {
            distributedLock.releaseLock(lockHandle);
        }
    }


    /**
     * 비동기로 상품 목록 캐시 갱신
     */
    @Async("cacheRefreshExecutor")
    public void refreshProductListCache(String cacheKey, Supplier<List<ProductInfo>> dbSupplier, Duration ttl) {
        String lockKey = "lock:" + cacheKey;
        DistributedLock.LockHandle lockHandle = distributedLock.tryLock(lockKey, Duration.ofSeconds(1), Duration.ofSeconds(10));

        if (lockHandle == null) {
            log.debug("다른 스레드가 이미 캐시를 갱신 중입니다: cacheKey={}", cacheKey);
            return;
        }

        try {
            log.info("캐시 갱신을 위한 락 획득: cacheKey={}", cacheKey);
            List<ProductInfo> productList = dbSupplier.get();
            redisTemplateMaster.opsForValue().set(cacheKey, productList, ttl);
            log.info("상품 목록 캐시 갱신 완료: cacheKey={}", cacheKey);
        } catch (Exception e) {
            log.error("캐시 갱신 실패: cacheKey={}", cacheKey, e);
        } finally {
            distributedLock.releaseLock(lockHandle);
        }
    }

    /**
     * 상품 상세 캐시 무효화
     */
    public void invalidateProduct(Long productId) {
        String key = getProductDetailKey(productId);
        redisTemplateMaster.delete(key);
        log.info("상품 상세 캐시 무효화 완료: productId={}", productId);
    }

    /**
     * 상품 상세 캐시를 즉시 갱신 (Write-Through)
     */
    public void updateProductCache(Long productId, ProductInfo productInfo) {
        String cacheKey = getProductDetailKey(productId);
        redisTemplateMaster.opsForValue().set(cacheKey, productInfo, PRODUCT_DETAIL_TTL);
        log.info("상품 상세 캐시 갱신 완료: productId={}", productId);
    }

    /**
     * 상품 상세 캐시를 비동기로 갱신 (Write-Behind)
     */
    @Async("cacheRefreshExecutor")
    public void updateProductCacheAsync(Long productId, ProductInfo productInfo) {
        String cacheKey = getProductDetailKey(productId);
        
        Boolean exists = redisTemplateMaster.hasKey(cacheKey);
        if (exists) {
            redisTemplateMaster.opsForValue().set(cacheKey, productInfo, PRODUCT_DETAIL_TTL);
            log.info("상품 상세 캐시 비동기 갱신 완료: productId={}", productId);
        } else {
            log.debug("상품 상세 캐시가 존재하지 않아 갱신하지 않음: productId={}", productId);
        }
    }

    /**
     * 상품 목록 캐시 무효화
     */
    public void invalidateProductList(Long brandId) {
        String pattern;
        if (brandId != null) {
            pattern = PRODUCT_LIST_PREFIX + "brandId=" + brandId + "*";
        } else {
            pattern = PRODUCT_LIST_PREFIX + "*";
        }

        redisTemplateMaster.keys(pattern).forEach(redisTemplateMaster::delete);
        log.info("상품 목록 캐시 무효화 완료: brandId={}", brandId);
    }

    /**
     * 상품 목록 캐시를 비동기로 무효화
     */
    @Async("cacheRefreshExecutor")
    public void invalidateProductListAsync(Long brandId) {
        String pattern;
        if (brandId != null) {
            pattern = PRODUCT_LIST_PREFIX + "brandId=" + brandId + "*";
        } else {
            pattern = PRODUCT_LIST_PREFIX + "*";
        }

        redisTemplateMaster.keys(pattern).forEach(redisTemplateMaster::delete);
        log.info("상품 목록 캐시 비동기 무효화 완료: brandId={}", brandId);
    }
}
