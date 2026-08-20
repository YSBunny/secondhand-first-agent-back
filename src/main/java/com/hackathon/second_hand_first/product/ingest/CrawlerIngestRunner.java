package com.hackathon.second_hand_first.product.ingest;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.hackathon.second_hand_first.product.domain.ProductCategory;

import java.nio.file.Path;

/**
 * 적재를 실행하는 진입점. <b>일회성 배치다.</b>
 *
 * <pre>
 * java -jar app.jar --ingest.enabled=true --ingest.path=출력디렉터리
 * </pre>
 *
 * <p>HTTP 엔드포인트로 만들지 않은 이유가 있다. 엔드포인트를 열면 인증 설정을
 * 건드려야 하는데, <b>적재는 사람이 가끔 돌리는 작업이라 상시 열어 둘 이유가 없다.</b>
 * 열어 두면 아무나 부를 수 있는 쓰기 API 가 하나 생긴다.
 *
 * <p>{@code ingest.enabled} 가 없으면 빈 자체가 만들어지지 않는다.
 * 평소 기동에는 아무 영향이 없다.
 *
 * <p>끝나면 애플리케이션을 종료한다. 실패가 있으면 종료 코드 1을 준다 —
 * 스크립트로 돌릴 때 성패를 알 수 있어야 한다.
 */
@Component
@ConditionalOnProperty(name = "ingest.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CrawlerIngestRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CrawlerIngestRunner.class);

    private final CrawlerIngestService crawlerIngestService;
    private final ApplicationContext applicationContext;

    @Value("${ingest.path:}")
    private String ingestPath;

    /**
     * 카테고리를 직접 지정한다. 비우면 AI 에게 묻는다.
     *
     * <p>«에어팟 프로 3»처럼 AI 가 판정하지 못하는 검색어가 있다. 사람이 아는 경우
     * 이 값으로 지정하면 OTHER 로 들어가지 않는다. 단 <b>지정한 값은 대상 파일 전부에
     * 적용</b>되므로 파일 하나를 적재할 때만 쓴다.
     */
    @Value("${ingest.category:}")
    private String ingestCategory;

    @Override
    public void run(ApplicationArguments args) {
        if (ingestPath == null || ingestPath.isBlank()) {
            log.error("ingest.path 가 없습니다. 예: --ingest.path=crawler/unified/output");
            exit(1);
            return;
        }

        ProductCategory override;
        try {
            override = parseCategory();
        } catch (IllegalArgumentException exception) {
            log.error("모르는 카테고리입니다: {} — 가능한 값 {}",
                    ingestCategory, java.util.Arrays.toString(ProductCategory.values()));
            exit(1);
            return;
        }

        try {
            IngestReport report = crawlerIngestService.ingest(Path.of(ingestPath), override);
            print(report);
            exit(report.hasFailure() ? 1 : 0);
        } catch (Exception exception) {
            log.error("적재 실패 — path={}", ingestPath, exception);
            exit(1);
        }
    }

    private ProductCategory parseCategory() {
        if (ingestCategory == null || ingestCategory.isBlank()) {
            return null;
        }
        return ProductCategory.valueOf(ingestCategory.trim().toUpperCase());
    }

    private void print(IngestReport report) {
        log.info("───── 적재 결과 ─────");
        for (IngestReport.FileResult file : report.files()) {
            log.info(
                    "  {} | query={} category={} | {}/{}건 저장",
                    file.fileName(), file.query(), file.category(), file.saved(), file.total()
            );
            for (String failure : file.failures()) {
                log.warn("    실패: {}", failure);
            }
            if (file.failed() > file.failures().size()) {
                log.warn("    … 외 {}건 더 실패", file.failed() - file.failures().size());
            }
        }
        log.info(
                "  합계 {}/{}건 저장, {}건 실패",
                report.totalSaved(), report.totalItems(), report.totalFailed()
        );
    }

    private void exit(int code) {
        SpringApplication.exit(applicationContext, () -> code);
    }
}
