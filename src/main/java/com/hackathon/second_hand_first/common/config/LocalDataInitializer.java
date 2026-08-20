package com.hackathon.second_hand_first.common.config;

import com.hackathon.second_hand_first.activity.domain.CarbonQuest;
import com.hackathon.second_hand_first.activity.domain.PlatformRedirectHistory;
import com.hackathon.second_hand_first.activity.domain.ProductViewRecord;
import com.hackathon.second_hand_first.activity.repository.CarbonQuestRepository;
import com.hackathon.second_hand_first.activity.repository.PlatformRedirectHistoryRepository;
import com.hackathon.second_hand_first.activity.repository.ProductViewRecordRepository;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.search.domain.SearchMessage;
import com.hackathon.second_hand_first.search.domain.SearchPriority;
import com.hackathon.second_hand_first.search.domain.SearchSession;
import com.hackathon.second_hand_first.search.domain.SearchSessionStatus;
import com.hackathon.second_hand_first.search.repository.SearchMessageRepository;
import com.hackathon.second_hand_first.search.repository.SearchSessionRepository;
import com.hackathon.second_hand_first.user.domain.User;
import com.hackathon.second_hand_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("local")
@Order(2)
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private static final String LOCAL_EMAIL = "myki011122@gmail.com";
    private static final String LOCAL_PASSWORD = "password123!";

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SearchSessionRepository searchSessionRepository;
    private final SearchMessageRepository searchMessageRepository;
    private final PlatformRedirectHistoryRepository redirectHistoryRepository;
    private final ProductViewRecordRepository productViewRecordRepository;
    private final CarbonQuestRepository carbonQuestRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User user = userRepository.findByEmail(LOCAL_EMAIL)
                .orElseGet(this::createLocalUser);
        List<Product> products = loadFixtureProducts();

        seedSearchSessions(user.getId());
        seedRedirectHistories(user.getId(), products);
        seedCarbonQuest(user.getId(), products);
    }

    private User createLocalUser() {
        return userRepository.save(User.create(
                "김민재",
                LOCAL_EMAIL,
                passwordEncoder.encode(LOCAL_PASSWORD),
                null,
                true,
                false
        ));
    }

    private List<Product> loadFixtureProducts() {
        return List.of(
                findProduct(Platform.DAANGN, "mock_1"),
                findProduct(Platform.JOONGGONARA, "mock_2"),
                findProduct(Platform.BUNGJANG, "mock_3")
        );
    }

    private Product findProduct(Platform platform, String externalProductId) {
        return productRepository.findByPlatformAndExternalProductId(platform, externalProductId)
                .orElseThrow(() -> new IllegalStateException("로컬 fixture 상품을 찾을 수 없습니다."));
    }

    private void seedSearchSessions(Long userId) {
        if (searchSessionRepository.countByUserIdAndStatus(userId, SearchSessionStatus.COMPLETED) > 0) {
            return;
        }
        createSearchSession(
                userId,
                "ss_local_01",
                "30만원으로 에어팟 사고 싶어, 중고 괜찮아",
                "에어팟",
                "30만원 이하, 중고 가능, 최고 가성비",
                "당근·번개장터·중고나라에서 12개 매물을 찾았어요.",
                300_000L,
                List.of(ProductCondition.LIKE_NEW, ProductCondition.GOOD),
                12
        );
        createSearchSession(
                userId,
                "ss_local_02",
                "맥북 에어 M2 100만원 아래",
                "맥북 에어 M2",
                "100만원 이하, 상태 좋은 상품",
                "가격과 상품 상태가 좋은 매물을 추렸어요.",
                1_000_000L,
                List.of(ProductCondition.LIKE_NEW, ProductCondition.GOOD),
                7
        );
        createSearchSession(
                userId,
                "ss_local_03",
                "애플워치 SE 미개봉",
                "애플워치 SE",
                "미개봉 상품 우선",
                "미개봉 애플워치 매물을 찾아봤어요.",
                null,
                List.of(ProductCondition.UNOPENED),
                4
        );
    }

    private void createSearchSession(
            Long userId,
            String sessionId,
            String query,
            String keyword,
            String summary,
            String assistantMessage,
            Long maxPrice,
            List<ProductCondition> conditions,
            int resultCount
    ) {
        SearchSession session = SearchSession.create(sessionId, userId, query);
        session.complete(
                keyword,
                summary,
                assistantMessage,
                maxPrice,
                SearchPriority.BEST_VALUE,
                conditions,
                resultCount
        );
        searchSessionRepository.save(session);
        searchMessageRepository.save(SearchMessage.create(
                "msg_" + sessionId,
                session,
                assistantMessage
        ));
    }

    private void seedRedirectHistories(Long userId, List<Product> products) {
        if (redirectHistoryRepository.countByUserId(userId) > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < 8; index++) {
            Product product = products.get(index % products.size());
            redirectHistoryRepository.save(PlatformRedirectHistory.create(
                    userId,
                    product,
                    product.getPlatformUrl(),
                    now.minusHours(index + 1L)
            ));
        }
    }

    private void seedCarbonQuest(Long userId, List<Product> products) {
        LocalDate today = LocalDate.now();
        if (carbonQuestRepository.findByUserIdAndQuestDate(userId, today).isPresent()) {
            return;
        }

        CarbonQuest quest = CarbonQuest.create(userId, today);
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            LocalDateTime viewedAt = now.minusMinutes(products.size() - index);
            productViewRecordRepository.save(ProductViewRecord.create(
                    userId,
                    product,
                    today,
                    viewedAt,
                    true
            ));
            quest.countView(viewedAt);
        }
        carbonQuestRepository.save(quest);
    }
}
