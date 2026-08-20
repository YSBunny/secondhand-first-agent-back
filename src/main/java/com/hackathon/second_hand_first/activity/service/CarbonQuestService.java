package com.hackathon.second_hand_first.activity.service;

import com.hackathon.second_hand_first.activity.domain.CarbonQuest;
import com.hackathon.second_hand_first.activity.domain.CarbonQuestCountedReason;
import com.hackathon.second_hand_first.activity.domain.ProductViewRecord;
import com.hackathon.second_hand_first.activity.dto.ProductViewResponse;
import com.hackathon.second_hand_first.activity.dto.CarbonQuestResponse;
import com.hackathon.second_hand_first.activity.repository.CarbonQuestRepository;
import com.hackathon.second_hand_first.activity.repository.ProductViewRecordRepository;
import com.hackathon.second_hand_first.auth.exception.UnauthorizedException;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.exception.ProductNotFoundException;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class CarbonQuestService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductViewRecordRepository productViewRecordRepository;
    private final CarbonQuestRepository carbonQuestRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CarbonQuestResponse getTodayQuest(Long userId) {
        LocalDate today = LocalDate.now(clock.withZone(SEOUL));
        return carbonQuestRepository.findByUserIdAndQuestDate(userId, today)
                .map(CarbonQuestResponse::from)
                .orElseGet(() -> CarbonQuestResponse.empty(today));
    }

    @Transactional
    public ProductViewResponse recordProductView(Long userId, Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("요청 값이 올바르지 않습니다.");
        }
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UnauthorizedException("인증이 필요합니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);
        LocalDate today = LocalDate.now(clock.withZone(SEOUL));
        LocalDateTime now = LocalDateTime.now(clock.withZone(SEOUL));
        CarbonQuest quest = carbonQuestRepository.findByUserIdAndQuestDate(userId, today)
                .orElse(null);

        if (!product.isCarbonReductionEligible()) {
            return quest == null
                    ? ProductViewResponse.empty(CarbonQuestCountedReason.NOT_ELIGIBLE, today)
                    : ProductViewResponse.of(false, CarbonQuestCountedReason.NOT_ELIGIBLE, false, quest);
        }
        if (productViewRecordRepository.existsByUserIdAndProductIdAndViewedDate(userId, productId, today)) {
            return quest == null
                    ? ProductViewResponse.empty(CarbonQuestCountedReason.ALREADY_VIEWED, today)
                    : ProductViewResponse.of(false, CarbonQuestCountedReason.ALREADY_VIEWED, false, quest);
        }

        if (quest == null) {
            quest = CarbonQuest.create(userId, today);
        }
        if (quest.isCompleted()) {
            productViewRecordRepository.save(ProductViewRecord.create(userId, product, today, now, false));
            return ProductViewResponse.of(
                    false,
                    CarbonQuestCountedReason.QUEST_ALREADY_COMPLETED,
                    false,
                    quest
            );
        }

        productViewRecordRepository.save(ProductViewRecord.create(userId, product, today, now, true));
        boolean rewarded = quest.countView(now);
        carbonQuestRepository.save(quest);
        return ProductViewResponse.of(true, CarbonQuestCountedReason.COUNTED, rewarded, quest);
    }
}
