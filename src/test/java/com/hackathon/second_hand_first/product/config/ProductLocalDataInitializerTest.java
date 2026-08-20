package com.hackathon.second_hand_first.product.config;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=local")
class ProductLocalDataInitializerTest {

    @Autowired
    private ProductLocalDataInitializer initializer;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void local_프로필에서_샘플상품을_중복없이_저장한다() throws Exception {
        assertThat(productRepository.count()).isEqualTo(3);
        assertThat(productRepository.existsByPlatformAndExternalProductId(Platform.DAANGN, "mock_1"))
                .isTrue();

        initializer.run(null);

        assertThat(productRepository.count()).isEqualTo(3);
    }
}
