package com.hackathon.second_hand_first.product.repository;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductImage;
import com.hackathon.second_hand_first.product.support.ProductFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 상품과_상세_연관정보를_함께_저장하고_조회한다() {
        Product saved = productRepository.saveAndFlush(ProductFixture.airPodsPro2());
        Long productId = saved.getId();
        entityManager.clear();

        Product found = productRepository.findDetailById(productId).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("AirPods Pro 2 (USB-C)");
        assertThat(found.getImages())
                .extracting(ProductImage::getDisplayOrder)
                .containsExactly(0, 1);
        assertThat(found.getSellerSnapshot().getTradeCount()).isEqualTo(32);
        assertThat(found.getSellerSnapshot().getMannerTemperature()).isEqualTo(92.0);
    }

    @Test
    void 플랫폼과_외부상품아이디로_상품을_조회한다() {
        productRepository.saveAndFlush(ProductFixture.airPodsPro2());
        entityManager.clear();

        Product found = productRepository
                .findByPlatformAndExternalProductId(Platform.NAVER_FLEAMARKET, "mock_1")
                .orElseThrow();

        assertThat(found.getPrice()).isEqualTo(180_000L);
        assertThat(productRepository.existsByPlatformAndExternalProductId(Platform.NAVER_FLEAMARKET, "mock_1"))
                .isTrue();
    }

    @Test
    void 같은_플랫폼의_외부상품아이디는_중복될_수_없다() {
        productRepository.saveAndFlush(ProductFixture.airPodsPro2());
        entityManager.clear();

        Product duplicate = ProductFixture.airPodsPro2();

        assertThatThrownBy(() -> productRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 상품을_삭제하면_이미지와_판매자정보도_삭제한다() {
        Product saved = productRepository.saveAndFlush(ProductFixture.airPodsPro2());
        Long productId = saved.getId();

        productRepository.delete(saved);
        productRepository.flush();
        entityManager.clear();

        Number imageCount = (Number) entityManager.createNativeQuery(
                        "select count(*) from product_images where product_id = :productId"
                )
                .setParameter("productId", productId)
                .getSingleResult();
        Number sellerCount = (Number) entityManager.createNativeQuery(
                        "select count(*) from seller_snapshots where product_id = :productId"
                )
                .setParameter("productId", productId)
                .getSingleResult();

        assertThat(imageCount.longValue()).isZero();
        assertThat(sellerCount.longValue()).isZero();
    }

    @Test
    void 절약금액과_절약률을_계산한다() {
        Product product = ProductFixture.airPodsPro2();

        assertThat(product.calculateSavingsAmount()).isEqualTo(119_000L);
        assertThat(product.calculateSavingsRate()).isEqualTo(40);
    }

    @Test
    void 같은_이미지_노출순서는_추가할_수_없다() {
        Product product = ProductFixture.airPodsPro2();

        assertThatThrownBy(() -> product.addImage("https://cdn.example.com/duplicate.jpg", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 이미지 노출 순서를 중복해서 사용할 수 없습니다.");
    }
}
