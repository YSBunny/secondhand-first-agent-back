package com.hackathon.second_hand_first.location.service;

import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import com.hackathon.second_hand_first.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final KakaoLocalService kakaoLocalService;
    private final UserService userService;

    public CoordinateResponse updateLocation(Long userId, String address) {
        // 외부 API 호출은 DB 트랜잭션 시작 전에 수행
        CoordinateResponse coordinate =
                kakaoLocalService.getCoordinate(address);

        return userService.updateLocation(userId, coordinate);
    }
}