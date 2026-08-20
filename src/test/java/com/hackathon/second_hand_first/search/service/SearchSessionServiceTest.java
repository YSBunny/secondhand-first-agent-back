package com.hackathon.second_hand_first.search.service;

import com.hackathon.second_hand_first.carbon.service.CarbonSavingService;
import com.hackathon.second_hand_first.location.service.ProductLocationEnrichmentService;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.service.ProductUpsertService;
import com.hackathon.second_hand_first.search.application.AiSearchClient;
import com.hackathon.second_hand_first.search.domain.SearchPriority;
import com.hackathon.second_hand_first.search.domain.SearchMessage;
import com.hackathon.second_hand_first.search.domain.SearchSession;
import com.hackathon.second_hand_first.search.domain.SearchSessionStatus;
import com.hackathon.second_hand_first.search.dto.request.SearchSessionCreateRequest;
import com.hackathon.second_hand_first.search.dto.response.SearchSessionCreateResponse;
import com.hackathon.second_hand_first.search.dto.response.SearchSessionDetailResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiParsedConditionsResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchResponse;
import com.hackathon.second_hand_first.search.repository.SearchResultRepository;
import com.hackathon.second_hand_first.search.repository.SearchMessageRepository;
import com.hackathon.second_hand_first.search.repository.SearchSessionRepository;
import com.hackathon.second_hand_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchSessionServiceTest {

    @Mock
    private SearchSessionRepository searchSessionRepository;

    @Mock
    private SearchResultRepository searchResultRepository;

    @Mock
    private SearchMessageRepository searchMessageRepository;

    @Mock
    private AiSearchClient aiSearchClient;

    @Mock
    private ProductUpsertService productUpsertService;

    @Mock
    private CarbonSavingService carbonSavingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductLocationEnrichmentService productLocationEnrichmentService;

    private SearchSessionService searchSessionService;

    @BeforeEach
    void setUp() {
        searchSessionService = new SearchSessionService(
                searchSessionRepository,
                searchResultRepository,
                searchMessageRepository,
                aiSearchClient,
                productUpsertService,
                carbonSavingService,
                userRepository,
                productLocationEnrichmentService
        );
    }

    @Test
    void 분석과_상품검색결과로_완료된_검색세션을_저장한다() {
        AiParsedConditionsResponse analysis = new AiParsedConditionsResponse(
                "에어팟",
                300_000L,
                List.of(ProductCondition.LIKE_NEW, ProductCondition.LIGHTLY_USED),
                SearchPriority.BEST_VALUE,
                "30만원 이하, 중고 가능, 최고 가성비"
        );
        AiSearchResponse aiResponse = new AiSearchResponse(
                analysis,
                "당근·번개장터·중고나라에서 12개 매물을 찾았어요.",
                12,
                List.of()
        );
        when(aiSearchClient.search(any())).thenReturn(aiResponse);
        when(productLocationEnrichmentService.enrichRecommendations(aiResponse.products()))
                .thenReturn(aiResponse.products());
        when(searchSessionRepository.save(any(SearchSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SearchSessionCreateResponse response = searchSessionService.create(
                1L,
                new SearchSessionCreateRequest("30만원으로 에어팟 사고 싶어, 중고 괜찮아")
        );

        assertThat(response.sessionId()).startsWith("ss_");
        assertThat(response.status()).isEqualTo(SearchSessionStatus.COMPLETED);
        assertThat(response.parsedConditions().keyword()).isEqualTo("에어팟");
        assertThat(response.resultCount()).isEqualTo(12);
    }

    @Test
    void 본인의_검색세션과_AI_메시지를_조회한다() {
        SearchSession session = SearchSession.create("ss_01", 1L, "30만원으로 에어팟 사고 싶어");
        session.complete(
                "에어팟",
                "30만원 이하",
                "12개 매물을 찾았어요.",
                300_000L,
                SearchPriority.BEST_VALUE,
                List.of(ProductCondition.LIKE_NEW),
                12
        );
        SearchMessage message = SearchMessage.create("msg_01", session, "12개 매물을 찾았어요.");
        when(searchSessionRepository.findBySessionIdAndUserId("ss_01", 1L))
                .thenReturn(java.util.Optional.of(session));
        when(searchMessageRepository.findBySearchSessionIdOrderByCreatedAtAscIdAsc(null))
                .thenReturn(List.of(message));

        SearchSessionDetailResponse response = searchSessionService.getSession(1L, "ss_01");

        assertThat(response.originalQuery()).isEqualTo("30만원으로 에어팟 사고 싶어");
        assertThat(response.parsedConditions().keyword()).isEqualTo("에어팟");
        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().getFirst().id()).isEqualTo("msg_01");
        assertThat(response.messages().getFirst().content()).isEqualTo("12개 매물을 찾았어요.");
    }
}
