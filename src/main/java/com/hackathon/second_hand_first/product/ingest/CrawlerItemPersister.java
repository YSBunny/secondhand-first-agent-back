package com.hackathon.second_hand_first.product.ingest;

import com.hackathon.second_hand_first.location.service.ProductLocationEnrichmentService;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.service.ProductUpsertService;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 한 건을 좌표까지 채워 저장한다. <b>트랜잭션 경계가 여기다.</b>
 *
 * <p>{@code ProductLocationEnrichmentService} 는 이미 저장된 상품의 거래지역을 읽어
 * 좌표를 재사용한다. 그 컬렉션이 지연 로딩이라 <b>트랜잭션 밖에서 만지면
 * LazyInitializationException 이 난다.</b> 검색 경로는 서비스 전체가 트랜잭션 안이라
 * 드러나지 않지만, 적재는 파일을 훑는 루프라 경계를 따로 만들어야 한다.
 *
 * <p>클래스를 나눈 이유는 {@code @Transactional} 이 프록시로 동작하기 때문이다.
 * 같은 클래스 안에서 부르면 프록시를 거치지 않아 경계가 생기지 않는다.
 *
 * <p>한 건이 실패해도 그 건만 롤백된다. 파일 전체를 한 트랜잭션으로 묶으면
 * 한 상품 때문에 수십 건이 함께 사라진다.
 */
@Component
@RequiredArgsConstructor
public class CrawlerItemPersister {

    private final ProductUpsertService productUpsertService;
    private final ProductLocationEnrichmentService productLocationEnrichmentService;

    @Transactional
    public void persist(CrawlerItem item, ProductCategory category) {
        AiProductResponse product = CrawlerItemMapper.toProduct(item, category);
        // 주소 → 좌표. 이미 저장된 같은 주소는 카카오를 다시 부르지 않고 재사용한다.
        product = productLocationEnrichmentService.enrich(product);
        productUpsertService.upsert(product);
    }
}
