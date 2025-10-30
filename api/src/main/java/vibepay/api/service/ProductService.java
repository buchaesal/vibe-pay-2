package vibepay.api.service;

import vibepay.api.dto.request.ProductRegisterRequest;
import vibepay.api.dto.response.ProductResponse;

import java.util.List;

/**
 * 상품 서비스 인터페이스
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
public interface ProductService {
    /**
     * 상품 목록 조회
     */
    List<ProductResponse> getProductList();

    /**
     * 상품 등록
     */
    void registerProduct(ProductRegisterRequest request);
}
