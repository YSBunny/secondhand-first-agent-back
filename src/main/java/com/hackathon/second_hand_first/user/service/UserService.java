package com.hackathon.second_hand_first.user.service;

import com.hackathon.second_hand_first.auth.token.RefreshTokenService;
import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import com.hackathon.second_hand_first.user.domain.User;
import com.hackathon.second_hand_first.user.dto.request.UserProfileUpdateRequest;
import com.hackathon.second_hand_first.user.dto.response.UserProfileResponse;
import com.hackathon.second_hand_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public UserProfileResponse getProfile(Long userId) {
        return toResponse(findUser(userId));
    }

    public CoordinateResponse getLocation(Long userId) {
        User user = findUser(userId);
        if (user.getLatitude() == null || user.getLongitude() == null) {
            throw new IllegalArgumentException("사용자의 활동 지역을 먼저 설정해 주세요.");
        }
        return new CoordinateResponse(
                user.getRegion(),
                user.getLatitude(),
                user.getLongitude()
        );
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = findUser(userId);
        user.updateProfile(request.name(), request.profileImageUrl());
        return toResponse(user);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUser(userId);
        refreshTokenService.deleteByUserId(userId);
        userRepository.delete(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(), user.getName(), user.getEmail(),
                user.getProfileImageUrl(), user.getCreatedAt()
        );
    }

    @Transactional
    public CoordinateResponse updateLocation(
            Long userId,
            CoordinateResponse coordinate
    ) {
        User user = findUser(userId);

        user.updateLocation(
                coordinate.region(),
                coordinate.latitude(),
                coordinate.longitude()
        );

        return new CoordinateResponse(
                user.getRegion(),
                user.getLatitude(),
                user.getLongitude()
        );
    }
}
