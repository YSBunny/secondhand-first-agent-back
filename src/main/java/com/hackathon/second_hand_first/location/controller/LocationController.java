package com.hackathon.second_hand_first.location.controller;

import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import com.hackathon.second_hand_first.location.service.KakaoLocalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("users/me/location")
public class LocationController {

    private final KakaoLocalService kakaoLocalService;

    @PatchMapping
    public CoordinateResponse getCoordinate(
            @RequestParam String address
    ) {
        return kakaoLocalService.getCoordinate(address);
    }
}