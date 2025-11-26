import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ============================================================================
// k6 성능 테스트 스크립트 - 상품 목록 조회 API
// ============================================================================
// 
// 테스트 시나리오:
// 1. 최신순 정렬 조회 (LATEST)
// 2. 가격 오름차순 정렬 조회 (PRICE_ASC)
// 3. 좋아요 내림차순 정렬 조회 (LIKES_DESC)
// 4. 브랜드 필터링 조회
// 5. 페이지네이션 테스트
//
// 실행 방법:
// 
// 기본 부하 테스트:
// k6 run k6_product_list.js
//
// 스트레스 테스트:
// TEST_MODE=stress k6 run k6_product_list.js
//
// 옵션 예시:
// k6 run --vus 50 --duration 30s k6_product_list.js
// TEST_MODE=stress k6 run k6_product_list.js
//
// ============================================================================

// 테스트 모드 설정 (기본: load, 스트레스: stress)
const TEST_MODE = __ENV.TEST_MODE || 'load';

// 커스텀 메트릭
const errorRate = new Rate('errors');
const responseTimeLatest = new Trend('response_time_latest');
const responseTimePriceAsc = new Trend('response_time_price_asc');
const responseTimeLikesDesc = new Trend('response_time_likes_desc');
const responseTimeBrandFilter = new Trend('response_time_brand_filter');
const responseTimePagination = new Trend('response_time_pagination');

// 테스트 설정 (모드에 따라 다름)
function getTestOptions() {
    if (TEST_MODE === 'stress') {
        // 스트레스 테스트 설정: 시스템의 한계점을 찾기 위한 높은 부하
        return {
            stages: [
                // 빠른 ramp-up으로 시스템에 부하 가하기
                { duration: '1m', target: 100 },   // 1분 동안 100명으로 증가
                { duration: '2m', target: 200 },   // 2분 동안 200명으로 증가
                { duration: '3m', target: 300 },   // 3분 동안 300명으로 증가
                { duration: '5m', target: 500 },  // 5분 동안 500명으로 증가 (최대 부하)
                { duration: '5m', target: 500 },  // 5분 동안 500명 유지 (최대 부하 지속)
                { duration: '2m', target: 300 },   // 2분 동안 300명으로 감소
                { duration: '1m', target: 100 },   // 1분 동안 100명으로 감소
                { duration: '1m', target: 0 },     // 1분 동안 0명으로 감소
            ],
            thresholds: {
                // 스트레스 테스트는 한계점을 찾는 것이므로 더 관대한 임계값
                // 하지만 여전히 모니터링은 필요
                http_req_duration: ['p(95)<2000', 'p(99)<5000'],  // 스트레스 상황에서는 더 느릴 수 있음
                errors: ['rate<0.10'],  // 스트레스 상황에서는 10%까지 허용
                http_req_failed: ['rate<0.20'],  // 스트레스 상황에서는 20%까지 허용
            },
        };
    } else {
        // 기본 부하 테스트 설정
        return {
            stages: [
                // Ramp-up: 점진적으로 부하 증가
                { duration: '30s', target: 50 },   // 30초 동안 50명의 가상 사용자로 증가
                { duration: '1m', target: 100 },   // 1분 동안 100명으로 증가
                { duration: '2m', target: 100 },   // 2분 동안 100명 유지
                { duration: '30s', target: 50 },    // 30초 동안 50명으로 감소
                { duration: '30s', target: 0 },   // 30초 동안 0명으로 감소
            ],
            thresholds: {
                // 전체 요청의 95%가 500ms 이내에 완료되어야 함
                http_req_duration: ['p(95)<500', 'p(99)<1000'],
                // 에러율이 1% 미만이어야 함
                errors: ['rate<0.01'],
                // HTTP 상태 코드가 200인 비율이 95% 이상이어야 함
                http_req_failed: ['rate<0.05'],
            },
        };
    }
}

export const options = getTestOptions();

// 기본 URL 설정
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_BASE = `${BASE_URL}/api/v1/products`;

// 브랜드 ID 목록 (1~100 사이 랜덤 선택)
function getRandomBrandId() {
    return Math.floor(Math.random() * 100) + 1;
}

// 랜덤 페이지 번호 (0~9)
function getRandomPage() {
    return Math.floor(Math.random() * 10);
}

// 쿼리 파라미터를 URL 문자열로 변환
function buildQueryString(params) {
    const parts = [];
    for (const key in params) {
        if (params[key] !== null && params[key] !== undefined) {
            parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`);
        }
    }
    return parts.join('&');
}

// 상품 목록 조회 헬퍼 함수
function getProducts(params, metric) {
    const queryString = buildQueryString(params);
    const url = queryString ? `${API_BASE}?${queryString}` : API_BASE;
    const startTime = Date.now();
    
    const response = http.get(url, {
        tags: { name: params.sort || 'latest' },
    });
    
    const duration = Date.now() - startTime;
    
    // 메트릭 기록
    if (metric) {
        metric.add(duration);
    }
    
    // 응답 검증
    const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                // ApiResponse 구조: { meta: { result: 'SUCCESS' }, data: { products: [...] } }
                return body.meta && 
                       body.meta.result === 'SUCCESS' && 
                       body.data && 
                       body.data.products && 
                       Array.isArray(body.data.products);
            } catch (e) {
                return false;
            }
        },
        'response time < 1000ms': (r) => r.timings.duration < 1000,
    });
    
    errorRate.add(!success);
    
    return { response, success, duration };
}

// ============================================================================
// 테스트 시나리오
// ============================================================================

export default function () {
    // 스트레스 테스트 모드에서는 sleep 시간을 줄여 더 빠르게 요청
    // 일반 모드에서도 sleep 시간을 줄여 더 많은 요청 처리
    const sleepTime = TEST_MODE === 'stress' ? 0.05 : 0.2;
    
    // 시나리오 1: 최신순 정렬 조회 (LATEST)
    const latestParams = {
        sort: 'latest',
        page: getRandomPage(),
        size: 20,
    };
    const latestResult = getProducts(latestParams, responseTimeLatest);
    
    if (!latestResult.success) {
        console.error('Latest sort failed:', latestResult.response.status);
    }
    
    sleep(sleepTime); // 요청 간 대기 (스트레스 모드: 0.1초, 일반 모드: 1초)
    
    // 시나리오 2: 가격 오름차순 정렬 조회 (PRICE_ASC)
    const priceAscParams = {
        sort: 'price_asc',
        page: getRandomPage(),
        size: 20,
    };
    const priceAscResult = getProducts(priceAscParams, responseTimePriceAsc);
    
    if (!priceAscResult.success) {
        console.error('Price ASC sort failed:', priceAscResult.response.status);
    }
    
    sleep(sleepTime);
    
    // 시나리오 3: 좋아요 내림차순 정렬 조회 (LIKES_DESC)
    const likesDescParams = {
        sort: 'likes_desc',
        page: getRandomPage(),
        size: 20,
    };
    const likesDescResult = getProducts(likesDescParams, responseTimeLikesDesc);
    
    if (!likesDescResult.success) {
        console.error('Likes DESC sort failed:', likesDescResult.response.status);
    }
    
    sleep(sleepTime);
    
    // 시나리오 4: 브랜드 필터링 조회
    const brandFilterParams = {
        brandId: getRandomBrandId(),
        sort: 'latest',
        page: 0,
        size: 20,
    };
    const brandFilterResult = getProducts(brandFilterParams, responseTimeBrandFilter);
    
    if (!brandFilterResult.success) {
        console.error('Brand filter failed:', brandFilterResult.response.status);
    }
    
    sleep(sleepTime);
    
    // 시나리오 5: 페이지네이션 테스트 (다양한 페이지 크기)
    const pageSizes = [10, 20, 50];
    const randomPageSize = pageSizes[Math.floor(Math.random() * pageSizes.length)];
    
    const paginationParams = {
        sort: 'latest',
        page: getRandomPage(),
        size: randomPageSize,
    };
    const paginationResult = getProducts(paginationParams, responseTimePagination);
    
    if (!paginationResult.success) {
        console.error('Pagination failed:', paginationResult.response.status);
    }
    
    sleep(sleepTime);
    
    // 시나리오 6: 브랜드 필터링 검증 테스트
    const brandId = getRandomBrandId();
    const brandValidationParams = {
        brandId: brandId,
        sort: 'latest',
        page: 0,
        size: 20,
    };
    const brandValidationResult = getProducts(brandValidationParams, responseTimeBrandFilter);
    
    if (brandValidationResult.success) {
        try {
            const body = JSON.parse(brandValidationResult.response.body);
            check(brandValidationResult.response, {
                'brand filter validation': () => {
                    if (body.data && body.data.products && body.data.products.length > 0) {
                        // 모든 상품이 같은 브랜드인지 확인
                        return body.data.products.every(product => product.brandId === brandId);
                    }
                    return true; // 빈 결과도 유효
                },
            });
        } catch (e) {
            // JSON 파싱 실패는 무시
        }
    }
    
    sleep(sleepTime);
    
    // 시나리오 7: 정렬 순서 검증 테스트
    // 최신순 정렬 검증
    const latestValidationParams = { sort: 'latest', page: 0, size: 10 };
    const latestValidationResult = getProducts(latestValidationParams, responseTimeLatest);
    
    if (latestValidationResult.success) {
        try {
            const body = JSON.parse(latestValidationResult.response.body);
            if (body.data && body.data.products && body.data.products.length > 1) {
                const firstCreatedAt = new Date(body.data.products[0].createdAt);
                const secondCreatedAt = new Date(body.data.products[1].createdAt);
                check(latestValidationResult.response, {
                    'latest sort validation': () => firstCreatedAt >= secondCreatedAt,
                });
            }
        } catch (e) {
            // JSON 파싱 실패는 무시
        }
    }
    
    sleep(sleepTime);
    
    // 가격 오름차순 정렬 검증
    const priceAscValidationParams = { sort: 'price_asc', page: 0, size: 10 };
    const priceAscValidationResult = getProducts(priceAscValidationParams, responseTimePriceAsc);
    
    if (priceAscValidationResult.success) {
        try {
            const body = JSON.parse(priceAscValidationResult.response.body);
            if (body.data && body.data.products && body.data.products.length > 1) {
                const firstPrice = body.data.products[0].price;
                const secondPrice = body.data.products[1].price;
                check(priceAscValidationResult.response, {
                    'price asc sort validation': () => firstPrice <= secondPrice,
                });
            }
        } catch (e) {
            // JSON 파싱 실패는 무시
        }
    }
    
    sleep(sleepTime);
    
    // 좋아요 내림차순 정렬 검증
    const likesDescValidationParams = { sort: 'likes_desc', page: 0, size: 10 };
    const likesDescValidationResult = getProducts(likesDescValidationParams, responseTimeLikesDesc);
    
    if (likesDescValidationResult.success) {
        try {
            const body = JSON.parse(likesDescValidationResult.response.body);
            if (body.data && body.data.products && body.data.products.length > 1) {
                const firstLikes = body.data.products[0].likeCount;
                const secondLikes = body.data.products[1].likeCount;
                check(likesDescValidationResult.response, {
                    'likes desc sort validation': () => firstLikes >= secondLikes,
                });
            }
        } catch (e) {
            // JSON 파싱 실패는 무시
        }
    }
    
    sleep(sleepTime);
}

// ============================================================================
// 설정 함수 (테스트 시작 전 실행)
// ============================================================================

export function setup() {
    // 테스트 시작 전 초기 설정
    console.log(`Starting k6 test against: ${BASE_URL}`);
    console.log(`API endpoint: ${API_BASE}`);
    console.log(`Test mode: ${TEST_MODE}`);
    
    if (TEST_MODE === 'stress') {
        console.log('⚠️  STRESS TEST MODE: Testing system limits with high load');
        console.log('   - Max VUs: 500');
        console.log('   - More lenient thresholds');
        console.log('   - Faster request rate');
    } else {
        console.log('📊 LOAD TEST MODE: Normal performance testing');
        console.log('   - Max VUs: 100');
        console.log('   - Standard thresholds');
    }
    
    return {
        baseUrl: BASE_URL,
        apiBase: API_BASE,
        testMode: TEST_MODE,
        startTime: new Date().toISOString(),
    };
}

// ============================================================================
// 정리 함수 (테스트 종료 후 실행)
// ============================================================================

export function teardown(data) {
    console.log(`Test completed at: ${new Date().toISOString()}`);
    console.log(`Test started at: ${data.startTime}`);
}

