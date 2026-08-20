package com.hackathon.second_hand_first.product.controller;

import com.hackathon.second_hand_first.auth.security.CustomUserDetails;
import com.hackathon.second_hand_first.common.response.ApiResponse;
import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import com.hackathon.second_hand_first.product.dto.request.ProductDistanceRankingRequest;
import com.hackathon.second_hand_first.product.service.ProductDistanceRankingService;
import com.hackathon.second_hand_first.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductDistanceController {

    private final UserService userService;
    private final ProductDistanceRankingService productDistanceRankingService;

    @PostMapping("/rank-by-distance")
    public ResponseEntity<ApiResponse<List<ObjectNode>>> rankByDistance(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProductDistanceRankingRequest request
    ) {
        CoordinateResponse userCoordinates =
                userService.getLocation(userDetails.getUserId());
        List<ObjectNode> rankedProducts = productDistanceRankingService
                .rankByDistance(userCoordinates, request.products());

        return ResponseEntity.ok(
                ApiResponse.success("가까운 상품 순으로 정렬했습니다.", rankedProducts)
        );
    }
}
