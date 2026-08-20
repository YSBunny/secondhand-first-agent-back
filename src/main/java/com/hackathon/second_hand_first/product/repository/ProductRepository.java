package com.hackathon.second_hand_first.product.repository;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByPlatformAndExternalProductId(
            Platform platform,
            String externalProductId
    );

    boolean existsByPlatformAndExternalProductId(
            Platform platform,
            String externalProductId
    );

    @EntityGraph(attributePaths = {"images", "sellerSnapshot"})
    List<Product> findDistinctByStatus(ProductStatus status);

    @EntityGraph(attributePaths = {"images", "sellerSnapshot"})
    @Query("select distinct product from Product product where product.id = :productId")
    Optional<Product> findDetailById(@Param("productId") Long productId);
}
