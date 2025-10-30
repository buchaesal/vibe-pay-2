package vibe.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vibe.api.common.dto.Response;
import vibe.api.dto.request.ProductRegisterRequest;
import vibe.api.dto.response.ProductResponse;
import vibe.api.service.ProductService;

import java.util.List;

/**
 * 상품 컨트롤러
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록 조회 API
     */
    @GetMapping
    public Response<List<ProductResponse>> getProductList() {
        log.info("상품 목록 조회 요청");

        List<ProductResponse> products = productService.getProductList();

        return new Response<>(products);
    }

    /**
     * 상품 등록 API
     */
    @PostMapping
    public Response<Void> registerProduct(@RequestBody @Valid ProductRegisterRequest request) {
        log.info("상품 등록 요청: productName={}", request.getProductName());

        productService.registerProduct(request);

        return new Response<>();
    }
}
