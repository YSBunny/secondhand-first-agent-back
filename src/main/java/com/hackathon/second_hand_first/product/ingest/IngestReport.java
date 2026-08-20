package com.hackathon.second_hand_first.product.ingest;

import com.hackathon.second_hand_first.product.domain.ProductCategory;

import java.util.List;

/**
 * 적재 결과. 사람이 로그로 읽고 «제대로 들어갔나»를 판단하는 데 쓴다.
 *
 * <p>실패 건수만이 아니라 <b>왜 실패했는지</b>를 함께 남긴다. 숫자만 보면
 * 크롤러 스키마가 바뀐 것인지 데이터 한 건이 이상한 것인지 알 수 없다.
 */
public record IngestReport(
        List<FileResult> files
) {
    public record FileResult(
            String fileName,
            String query,
            ProductCategory category,
            int total,
            int saved,
            List<String> failures
    ) {
        public int failed() {
            return total - saved;
        }
    }

    public int totalItems() {
        return files.stream().mapToInt(FileResult::total).sum();
    }

    public int totalSaved() {
        return files.stream().mapToInt(FileResult::saved).sum();
    }

    public int totalFailed() {
        return totalItems() - totalSaved();
    }

    public boolean hasFailure() {
        return totalFailed() > 0;
    }
}
