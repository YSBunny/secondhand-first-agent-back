package com.hackathon.second_hand_first.activity.repository;

import com.hackathon.second_hand_first.activity.domain.PlatformRedirectHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformRedirectHistoryRepository extends JpaRepository<PlatformRedirectHistory, Long> {

    long countByUserId(Long userId);
}
