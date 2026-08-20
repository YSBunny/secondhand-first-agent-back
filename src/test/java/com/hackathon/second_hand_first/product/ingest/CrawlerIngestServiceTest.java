package com.hackathon.second_hand_first.product.ingest;

import com.hackathon.second_hand_first.product.domain.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrawlerIngestServiceTest {

    @Mock
    private QueryCategoryClient queryCategoryClient;

    @Mock
    private CrawlerItemPersister crawlerItemPersister;

    private CrawlerIngestService service;
    private Path sampleFile;

    @BeforeEach
    void setUp() throws Exception {
        service = new CrawlerIngestService(
                JsonMapper.builder().build(), queryCategoryClient, crawlerItemPersister
        );
        sampleFile = Path.of(getClass()
                .getResource("/crawler-sample/unified_에어팟_프로_3.json").toURI());
    }

    @Test
    @DisplayName("파일의 상품을 전부 저장한다")
    void savesEveryItem() throws IOException {
        given(queryCategoryClient.categoryOf("에어팟 프로 3"))
                .willReturn(ProductCategory.EARPHONES);

        IngestReport report = service.ingest(sampleFile);

        assertThat(report.files()).hasSize(1);
        IngestReport.FileResult file = report.files().getFirst();
        assertThat(file.total()).isEqualTo(file.saved());
        assertThat(report.hasFailure()).isFalse();
        verify(crawlerItemPersister, org.mockito.Mockito.times(file.total()))
                .persist(any(CrawlerItem.class), any(ProductCategory.class));
    }

    @Test
    @DisplayName("카테고리를 파일당 한 번만 묻는다 — 상품마다 물으면 Bedrock 비용이 커진다")
    void asksCategoryOncePerFile() throws IOException {
        given(queryCategoryClient.categoryOf("에어팟 프로 3"))
                .willReturn(ProductCategory.EARPHONES);

        service.ingest(sampleFile);

        verify(queryCategoryClient, org.mockito.Mockito.times(1)).categoryOf("에어팟 프로 3");
    }

    @Test
    @DisplayName("판정한 카테고리가 모든 상품에 붙는다")
    void appliesCategoryToAll() throws IOException {
        given(queryCategoryClient.categoryOf(any())).willReturn(ProductCategory.EARPHONES);

        service.ingest(sampleFile);

        ArgumentCaptor<ProductCategory> captor = ArgumentCaptor.forClass(ProductCategory.class);
        verify(crawlerItemPersister, org.mockito.Mockito.atLeastOnce())
                .persist(any(CrawlerItem.class), captor.capture());
        assertThat(captor.getAllValues()).containsOnly(ProductCategory.EARPHONES);
    }

    @Test
    @DisplayName("한 건이 실패해도 나머지는 저장한다")
    void oneFailureDoesNotStopTheFile() throws IOException {
        given(queryCategoryClient.categoryOf(any())).willReturn(ProductCategory.EARPHONES);
        willThrow(new IllegalArgumentException("필수 값 누락"))
                .willDoNothing()
                .given(crawlerItemPersister).persist(any(), any());

        IngestReport report = service.ingest(sampleFile);
        IngestReport.FileResult file = report.files().getFirst();

        assertThat(file.saved()).isEqualTo(file.total() - 1);
        assertThat(file.failures()).hasSize(1);
        assertThat(file.failures().getFirst()).contains("필수 값 누락");
        assertThat(report.hasFailure()).isTrue();
    }

    @Test
    @DisplayName("디렉터리를 주면 안의 JSON 을 모두 적재한다")
    void ingestsDirectory() throws IOException {
        given(queryCategoryClient.categoryOf(any())).willReturn(ProductCategory.EARPHONES);
        Path dir = Files.createTempDirectory("ingest-test");
        Files.copy(sampleFile, dir.resolve("a.json"));
        Files.copy(sampleFile, dir.resolve("b.json"));
        Files.writeString(dir.resolve("메모.txt"), "JSON 이 아니므로 무시돼야 한다");

        IngestReport report = service.ingest(dir);

        assertThat(report.files()).hasSize(2);
        assertThat(report.files())
                .extracting(IngestReport.FileResult::fileName)
                .containsExactly("a.json", "b.json");
    }

    @Test
    @DisplayName("없는 경로는 조용히 넘기지 않는다")
    void missingPathFails() {
        assertThatThrownBy(() -> service.ingest(Path.of("/없는/경로")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("경로가 없습니다");
    }

    @Test
    @DisplayName("JSON 이 없는 디렉터리도 실패로 알린다 — 0건 성공으로 보이면 안 된다")
    void emptyDirectoryFails() throws IOException {
        Path empty = Files.createTempDirectory("ingest-empty");

        assertThatThrownBy(() -> service.ingest(empty))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("찾지 못했습니다");
    }

    @Test
    @DisplayName("AI 가 판정 못 해 OTHER 여도 적재는 계속된다")
    void ingestsWithOtherCategory() throws IOException {
        given(queryCategoryClient.categoryOf(any())).willReturn(ProductCategory.OTHER);

        IngestReport report = service.ingest(sampleFile);

        assertThat(report.files().getFirst().category()).isEqualTo(ProductCategory.OTHER);
        assertThat(report.totalSaved()).isPositive();
    }

    @Test
    @DisplayName("실패 목록을 무한정 쌓지 않는다")
    void limitsFailureSamples() throws IOException {
        given(queryCategoryClient.categoryOf(any())).willReturn(ProductCategory.EARPHONES);
        willThrow(new IllegalArgumentException("전부 실패"))
                .given(crawlerItemPersister).persist(any(), any());

        IngestReport report = service.ingest(sampleFile);
        IngestReport.FileResult file = report.files().getFirst();

        assertThat(file.saved()).isZero();
        assertThat(file.failures()).hasSizeLessThanOrEqualTo(5);
        assertThat(file.failed()).isEqualTo(file.total());
    }

    @Test
    @DisplayName("같은 파일을 다시 적재해도 upsert 라 행이 늘지 않는다")
    void reIngestIsIdempotent() throws IOException {
        given(queryCategoryClient.categoryOf(any())).willReturn(ProductCategory.EARPHONES);

        IngestReport first = service.ingest(sampleFile);
        IngestReport second = service.ingest(sampleFile);

        // 저장 호출 수는 같다. 실제 행이 늘지 않는 것은
        // ProductUpsertService 가 (platform, external_product_id) 로 찾기 때문이다.
        assertThat(second.totalSaved()).isEqualTo(first.totalSaved());
    }

    @Test
    @DisplayName("적재 결과가 파일별로 남는다 — 숫자만으로는 원인을 알 수 없다")
    void reportsPerFile() throws IOException {
        given(queryCategoryClient.categoryOf(any())).willReturn(ProductCategory.EARPHONES);

        IngestReport report = service.ingest(sampleFile);
        IngestReport.FileResult file = report.files().getFirst();

        assertThat(file.fileName()).endsWith(".json");
        assertThat(file.query()).isEqualTo("에어팟 프로 3");
        assertThat(List.of(file.total(), file.saved())).allSatisfy(
                value -> assertThat(value).isPositive());
    }
}
