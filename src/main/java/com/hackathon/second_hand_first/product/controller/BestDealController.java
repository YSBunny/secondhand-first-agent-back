package com.hackathon.second_hand_first.product.controller;

import com.hackathon.second_hand_first.common.response.ApiResponse;
import com.hackathon.second_hand_first.product.dto.response.BestDealPageResponse;
import com.hackathon.second_hand_first.product.service.BestDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BestDealController {

    private final BestDealService bestDealService;

    @GetMapping("/products/best-deals")
    public ResponseEntity<ApiResponse<BestDealPageResponse>> getBestDeals(
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "AI_RECOMMENDED") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        BestDealPageResponse response = bestDealService.getBestDeals(category, sort, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("오늘의 Best Deal을 조회했습니다.", response)
        );
    }
}
