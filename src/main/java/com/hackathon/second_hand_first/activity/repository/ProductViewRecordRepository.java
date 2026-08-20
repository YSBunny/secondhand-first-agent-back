package com.hackathon.second_hand_first.activity.repository;

import com.hackathon.second_hand_first.activity.domain.ProductViewRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ProductViewRecordRepository extends JpaRepository<ProductViewRecord, Long> {

    boolean existsByUserIdAndProductIdAndViewedDate(
            Long userId,
            Long productId,
            LocalDate viewedDate
    );
}
