package vibepay.api.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vibepay.api.entity.Product;

import java.util.List;
import java.util.Optional;

/**
 * 상품 Mapper (조회 전용)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface ProductMapper {
    /**
     * 상품 목록 조회
     */
    List<Product> selectProductList();

    /**
     * 상품 단건 조회
     */
    Optional<Product> selectProductByProductNo(@Param("productNo") String productNo);

    /**
     * 상품명 중복 확인
     */
    int countByProductName(@Param("productName") String productName);

    /**
     * 상품번호 채번
     */
    String selectNextProductNo();
}
