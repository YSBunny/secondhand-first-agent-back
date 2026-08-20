package com.hackathon.second_hand_first.product.support;

import com.hackathon.second_hand_first.product.domain.DeliveryFee;
import com.hackathon.second_hand_first.product.domain.DeliveryPayer;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;

import java.time.LocalDateTime;

public final class ProductFixture {

    public static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 8, 20, 9, 0);
    public static final LocalDateTime REFRESHED_AT = LocalDateTime.of(2026, 8, 20, 11, 0);

    private ProductFixture() {
    }

    public static Product airPodsPro2() {
        Product product = Product.create(
                Platform.NAVER_FLEAMARKET,
                "mock_1",
                "AirPods Pro 2 (USB-C)",
                "박스와 구성품이 모두 포함된 상품입니다.",
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
                PUBLISHED_AT,
                REFRESHED_AT
        );

        // 저장 후 @OrderBy가 실제로 순서를 보장하는지 확인하기 위해 역순으로 추가합니다.
        product.addImage("https://cdn.example.com/airpods-2.jpg", 1);
        product.addImage("https://cdn.example.com/airpods-1.jpg", 0);
        product.updateSellerSnapshot(
                "seller_1",
                "판교 판매자",
                92,
                32,
                92.0,
                REFRESHED_AT
        );

        return product;
    }
}
