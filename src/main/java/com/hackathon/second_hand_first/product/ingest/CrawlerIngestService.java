package com.hackathon.second_hand_first.product.ingest;

import com.hackathon.second_hand_first.product.domain.ProductCategory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 통합 크롤러가 만든 JSON 을 읽어 상품 테이블에 넣는다.
 *
 * <p>저장은 {@code ProductUpsertService.upsert()} 가 한다.
 * <b>{@code (platform, external_product_id)} 로 찾아 없으면 만들고 있으면 갱신</b>하므로,
 * 같은 파일을 다시 적재해도 행이 늘지 않는다.
 *
 * <p>한 건이 실패해도 파일 전체를 포기하지 않는다. 크롤러가 새 값을 내보내
 * 몇 건이 안 들어가는 것과, 그 때문에 수백 건이 통째로 빠지는 것은 전혀 다른 문제다.
 */
@Service
@RequiredArgsConstructor
public class CrawlerIngestService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerIngestService.class);

    private static final String JSON_SUFFIX = ".json";

    /** 로그가 실패 목록으로 뒤덮이지 않도록 파일당 이만큼만 남긴다. */
    private static final int MAX_FAILURE_SAMPLES = 5;

    private final ObjectMapper objectMapper;
    private final QueryCategoryClient queryCategoryClient;

    /** 한 건을 저장하는 트랜잭션 경계. 왜 클래스를 나눴는지는 그쪽 주석에 있다. */
    private final CrawlerItemPersister crawlerItemPersister;


    /**
     * @param path 크롤러 JSON 파일 하나, 또는 그런 파일들이 있는 디렉터리
     */
    public IngestReport ingest(Path path) throws IOException {
        return ingest(path, null);
    }

    /**
     * @param path     크롤러 JSON 파일 하나, 또는 그런 파일들이 있는 디렉터리
     * @param override 카테고리를 직접 지정한다. {@code null} 이면 AI 에게 묻는다.
     *                 <p>AI 가 판정하지 못하는 검색어가 실제로 있다. «에어팟 프로 3»처럼
     *                 브랜드명만 있으면 본품인지 액세서리인지 단정하기 어려워 null 을 준다 —
     *                 <b>억지로 밀어 넣지 않는 것은 의도된 동작이다.</b> 사람이 아는 경우에는
     *                 이 값으로 지정하는 편이 OTHER 로 두는 것보다 낫다.
     *                 <p>디렉터리를 지정하면 <b>안의 모든 파일에 같은 값</b>이 적용되므로,
     *                 검색어가 섞인 디렉터리에는 쓰지 않는다.
     */
    public IngestReport ingest(Path path, ProductCategory override) throws IOException {
        List<Path> targets = resolveTargets(path);
        if (targets.isEmpty()) {
            throw new IOException("적재할 JSON 을 찾지 못했습니다: " + path);
        }

        List<IngestReport.FileResult> results = new ArrayList<>();
        for (Path target : targets) {
            results.add(ingestFile(target, override));
        }
        return new IngestReport(results);
    }

    private List<Path> resolveTargets(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("경로가 없습니다: " + path);
        }
        if (Files.isRegularFile(path)) {
            return List.of(path);
        }
        try (var stream = Files.list(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(JSON_SUFFIX))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private IngestReport.FileResult ingestFile(Path file, ProductCategory override)
            throws IOException {
        CrawlerFile crawled;
        try (var input = Files.newInputStream(file)) {
            crawled = objectMapper.readValue(input, CrawlerFile.class);
        }

        List<CrawlerItem> items = crawled.itemsOrEmpty();
        // 카테고리는 파일 하나에 한 번만 정한다. 상품마다 물으면 Bedrock 을
        // 상품 수만큼 부르게 되는데, 검색어가 같으니 답도 같다.
        ProductCategory category = override != null
                ? override
                : queryCategoryClient.categoryOf(crawled.query());
        if (override != null) {
            log.info("카테고리를 직접 지정 — file={} category={}", file.getFileName(), override);
        }

        List<String> failures = new ArrayList<>();
        int saved = 0;
        for (CrawlerItem item : items) {
            try {
                crawlerItemPersister.persist(item, category);
                saved++;
            } catch (Exception exception) {
                if (failures.size() < MAX_FAILURE_SAMPLES) {
                    failures.add("%s:%s — %s".formatted(
                            item.platform(), item.platformProductId(), exception.getMessage()
                    ));
                }
            }
        }

        log.info(
                "적재 완료 — file={} query={} category={} {}/{}건",
                file.getFileName(), crawled.query(), category, saved, items.size()
        );
        return new IngestReport.FileResult(
                file.getFileName().toString(), crawled.query(), category,
                items.size(), saved, failures
        );
    }
}
