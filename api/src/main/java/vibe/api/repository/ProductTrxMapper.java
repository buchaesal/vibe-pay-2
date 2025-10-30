package vibe.api.repository;

import org.apache.ibatis.annotations.Mapper;
import vibe.api.entity.Product;

/**
 * 상품 TrxMapper (등록/수정/삭제)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface ProductTrxMapper {
    /**
     * 상품 등록
     */
    int insertProduct(Product product);

    /**
     * 상품 정보 수정
     */
    int updateProduct(Product product);

    /**
     * 상품 삭제
     */
    int deleteProduct(String productNo);
}
