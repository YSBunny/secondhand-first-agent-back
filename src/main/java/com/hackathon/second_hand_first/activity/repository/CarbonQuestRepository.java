package com.hackathon.second_hand_first.activity.repository;

import com.hackathon.second_hand_first.activity.domain.CarbonQuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CarbonQuestRepository extends JpaRepository<CarbonQuest, Long> {

    Optional<CarbonQuest> findByUserIdAndQuestDate(Long userId, LocalDate questDate);
}
