package com.hackathon.second_hand_first.search.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 검색 세션에서 AI 어시스턴트가 반환한 메시지만 저장합니다.
 * 사용자 최초 질문은 SearchSession.originalQuery로 관리합니다.
 */
@Getter
@Entity
@Table(
        name = "search_messages",
        indexes = @Index(
                name = "idx_search_messages_session_created",
                columnList = "search_session_id,created_at"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 50)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "search_session_id", nullable = false)
    private SearchSession searchSession;

    @Column(nullable = false, length = 2_000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private SearchMessage(String messageId, SearchSession searchSession, String content) {
        this.messageId = requireText(messageId, "메시지 ID는 필수입니다.", 50);
        if (searchSession == null) {
            throw new IllegalArgumentException("검색 세션은 필수입니다.");
        }
        this.searchSession = searchSession;
        this.content = requireText(content, "메시지 내용은 필수입니다.", 2_000);
        this.createdAt = LocalDateTime.now();
    }

    public static SearchMessage create(
            String messageId,
            SearchSession searchSession,
            String content
    ) {
        return new SearchMessage(messageId, searchSession, content);
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private static String requireText(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("입력값은 " + maxLength + "자를 넘을 수 없습니다.");
        }
        return trimmed;
    }
}
