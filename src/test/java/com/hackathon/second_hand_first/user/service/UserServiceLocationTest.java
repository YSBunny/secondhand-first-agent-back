package com.hackathon.second_hand_first.user.service;

import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import com.hackathon.second_hand_first.user.domain.User;
import com.hackathon.second_hand_first.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceLocationTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

    @Test
    void returnsStoredUserCoordinates() {
        User user = createUser();
        user.updateLocation("서울특별시 중구 명동", 37.5609, 126.9860);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CoordinateResponse result = userService.getLocation(1L);

        assertThat(result).isEqualTo(new CoordinateResponse(
                "서울특별시 중구 명동",
                37.5609,
                126.9860
        ));
    }

    @Test
    void rejectsRankingBeforeUserSetsLocation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));

        assertThatThrownBy(() -> userService.getLocation(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자의 활동 지역을 먼저 설정해 주세요.");
    }

    private User createUser() {
        return User.create(
                "사용자",
                "user@example.com",
                "encoded-password",
                null,
                true,
                false
        );
    }
}
