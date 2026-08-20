package com.hackathon.second_hand_first.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "carbon_quests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_carbon_quests_user_date",
                columnNames = {"user_id", "quest_date"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarbonQuest {

    public static final int DAILY_GOAL = 3;
    public static final int COMPLETION_REWARD = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quest_date", nullable = false)
    private LocalDate questDate;

    @Column(name = "viewed_count", nullable = false)
    private int viewedCount;

    @Column(nullable = false)
    private int goal;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "earned_points", nullable = false)
    private int earnedPoints;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private CarbonQuest(Long userId, LocalDate questDate) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (questDate == null) {
            throw new IllegalArgumentException("미션 날짜는 필수입니다.");
        }
        this.userId = userId;
        this.questDate = questDate;
        this.viewedCount = 0;
        this.goal = DAILY_GOAL;
        this.completed = false;
        this.earnedPoints = 0;
    }

    public static CarbonQuest create(Long userId, LocalDate questDate) {
        return new CarbonQuest(userId, questDate);
    }

    public boolean countView(LocalDateTime viewedAt) {
        if (completed) {
            return false;
        }
        viewedCount = Math.min(viewedCount + 1, goal);
        if (viewedCount < goal) {
            return false;
        }
        completed = true;
        earnedPoints = COMPLETION_REWARD;
        completedAt = viewedAt;
        return true;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
