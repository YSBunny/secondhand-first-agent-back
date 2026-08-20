package com.hackathon.second_hand_first.product.config;

import com.hackathon.second_hand_first.product.domain.DeliveryFee;
import com.hackathon.second_hand_first.product.domain.DeliveryPayer;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Profile("local")
@Order(1)
@RequiredArgsConstructor
public class ProductLocalDataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        saveIfAbsent(airPodsPro2());
        saveIfAbsent(macBookAirM2());
        saveIfAbsent(appleWatchSe());
    }

    private void saveIfAbsent(Product product) {
        if (productRepository.existsByPlatformAndExternalProductId(
                product.getPlatform(),
                product.getExternalProductId()
        )) {
            return;
        }
        productRepository.save(product);
    }

    private Product airPodsPro2() {
        LocalDateTime refreshedAt = LocalDateTime.now();
        Product product = Product.create(
                Platform.NAVER_FLEAMARKET,
                "mock_1",
                "AirPods Pro 2 (USB-C)",
                "박스와 충전 케이블을 포함한 구성품이 모두 있으며 깨끗하게 사용한 상품입니다.",
                ProductCategory.EARPHONES,
                180_000L,
                299_000L,
                ProductCondition.LIKE_NEW,
                ProductStatus.SELLING,
                "판교",
                true,
                true,
                DeliveryFee.of(3_000L, 3_000L, DeliveryPayer.BUYER),
                true,
                "https://fleamarket.naver.com/products/mock_1",
                128L,
                refreshedAt.minusHours(2),
                refreshedAt
        );
        product.addImage("https://placehold.co/1200x900?text=AirPods+Front", 0);
        product.addImage("https://placehold.co/1200x900?text=AirPods+Case", 1);
        product.updateSellerSnapshot("seller_1", "판교 판매자", 92, 32, 92.0, refreshedAt);
        return product;
    }

    private Product macBookAirM2() {
        LocalDateTime refreshedAt = LocalDateTime.now();
        Product product = Product.create(
                Platform.JOONGNA,
                "mock_2",
                "맥북 에어 M2",
                "배터리 사이클이 낮고 박스와 기본 구성품을 모두 보관한 상품입니다.",
                ProductCategory.LAPTOP,
                980_000L,
                1_300_000L,
                ProductCondition.LIGHTLY_USED,
                ProductStatus.SELLING,
                "강남",
                true,
                true,
                DeliveryFee.of(3_000L, 3_000L, DeliveryPayer.BUYER),
                true,
                "https://web.joongna.com/product/mock_2",
                86L,
                refreshedAt.minusHours(4),
                refreshedAt
        );
        product.addImage("https://placehold.co/1200x900?text=MacBook+Air+M2", 0);
        product.addImage("https://placehold.co/1200x900?text=MacBook+Accessories", 1);
        product.updateSellerSnapshot("seller_2", "강남 판매자", 88, 24, null, refreshedAt);
        return product;
    }

    private Product appleWatchSe() {
        LocalDateTime refreshedAt = LocalDateTime.now();
        Product product = Product.create(
                Platform.BUNJANG,
                "mock_3",
                "애플워치 SE",
                "개봉하지 않은 미사용 상품이며 직거래와 택배 거래가 모두 가능합니다.",
                ProductCategory.SMARTWATCH,
                150_000L,
                245_000L,
                ProductCondition.NEW,
                ProductStatus.SELLING,
                "분당",
                true,
                true,
                DeliveryFee.of(3_000L, 3_000L, DeliveryPayer.BUYER),
                true,
                "https://m.bunjang.co.kr/products/mock_3",
                54L,
                refreshedAt.minusHours(6),
                refreshedAt
        );
        product.addImage("https://placehold.co/1200x900?text=Apple+Watch+SE", 0);
        product.updateSellerSnapshot("seller_3", "분당 판매자", 85, 18, null, refreshedAt);
        return product;
    }
}
