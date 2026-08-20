package com.hackathon.second_hand_first.product.controller;

import com.hackathon.second_hand_first.activity.domain.CarbonQuestCountedReason;
import com.hackathon.second_hand_first.activity.dto.ProductViewResponse;
import com.hackathon.second_hand_first.activity.dto.CarbonQuestResponse;
import com.hackathon.second_hand_first.activity.service.CarbonQuestService;
import com.hackathon.second_hand_first.activity.service.PlatformRedirectService;
import com.hackathon.second_hand_first.activity.dto.PlatformRedirectResponse;
import com.hackathon.second_hand_first.auth.security.CustomUserDetails;
import com.hackathon.second_hand_first.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductActivityController {

    private final CarbonQuestService carbonQuestService;
    private final PlatformRedirectService platformRedirectService;

    @GetMapping("/users/me/carbon-quest")
    public ResponseEntity<ApiResponse<CarbonQuestResponse>> getTodayCarbonQuest(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CarbonQuestResponse response = carbonQuestService.getTodayQuest(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("오늘의 탄소 절감 미션을 조회했습니다.", response));
    }

    @PostMapping("/products/{productId}/views")
    public ResponseEntity<ApiResponse<ProductViewResponse>> recordProductView(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId
    ) {
        ProductViewResponse response = carbonQuestService.recordProductView(
                userDetails.getUserId(),
                productId
        );
        return ResponseEntity.ok(ApiResponse.success(messageFor(response.countedReason()), response));
    }

    @PostMapping("/products/{productId}/redirect")
    public ResponseEntity<ApiResponse<PlatformRedirectResponse>> redirectToPlatform(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId
    ) {
        PlatformRedirectResponse response = platformRedirectService.record(
                userDetails.getUserId(),
                productId
        );
        return ResponseEntity.ok(
                ApiResponse.success("외부 플랫폼 이동을 기록했습니다.", response)
        );
    }

    private String messageFor(CarbonQuestCountedReason reason) {
        return switch (reason) {
            case COUNTED -> "상품 조회를 기록했습니다.";
            case ALREADY_VIEWED -> "이미 오늘 조회한 상품입니다.";
            case NOT_ELIGIBLE -> "탄소 절감 미션 대상 상품이 아닙니다.";
            case QUEST_ALREADY_COMPLETED -> "오늘의 탄소 절감 미션을 이미 완료했습니다.";
        };
    }
}
