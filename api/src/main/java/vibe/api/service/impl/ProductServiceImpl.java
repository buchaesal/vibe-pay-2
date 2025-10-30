package vibe.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.api.common.enums.ErrorCode;
import vibe.api.common.exception.ApiException;
import vibe.api.dto.request.ProductRegisterRequest;
import vibe.api.dto.response.ProductResponse;
import vibe.api.entity.Product;
import vibe.api.repository.ProductMapper;
import vibe.api.repository.ProductTrxMapper;
import vibe.api.service.ProductService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 서비스 구현
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductTrxMapper productTrxMapper;

    /**
     * 상품 목록 조회
     */
    @Override
    public List<ProductResponse> getProductList() {
        log.debug("상품 목록 조회");

        List<Product> products = productMapper.selectProductList();

        // Entity -> Response DTO 변환
        return products.stream()
            .map(product -> {
                ProductResponse response = new ProductResponse();
                response.setProductNo(product.getProductNo());
                response.setProductName(product.getProductName());
                response.setPrice(product.getPrice());
                return response;
            })
            .collect(Collectors.toList());
    }

    /**
     * 상품 등록
     */
    @Override
    @Transactional
    public void registerProduct(ProductRegisterRequest request) {
        log.debug("상품 등록 시도: productName={}", request.getProductName());

        // 중복 확인
        int count = productMapper.countByProductName(request.getProductName());
        if (count > 0) {
            throw new ApiException(ErrorCode.DUPLICATE_PRODUCT);
        }

        // 상품번호 채번
        String productNo = productMapper.selectNextProductNo();

        // Entity 생성
        Product product = new Product();
        product.setProductNo(productNo);
        product.setProductName(request.getProductName());
        product.setPrice(request.getPrice());

        // 상품 등록
        int result = productTrxMapper.insertProduct(product);
        if (result != 1) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        log.info("상품 등록 성공: productNo={}, productName={}", productNo, request.getProductName());
    }
}
