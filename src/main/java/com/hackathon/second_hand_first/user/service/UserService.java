package com.hackathon.second_hand_first.user.service;

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

    public UserProfileResponse getProfile(Long userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = findUser(userId);
        user.updateProfile(request.name(), request.profileImageUrl());
        return toResponse(user);
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
}
