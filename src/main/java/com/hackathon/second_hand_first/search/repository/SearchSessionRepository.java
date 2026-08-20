package com.hackathon.second_hand_first.search.repository;

import com.hackathon.second_hand_first.search.domain.SearchSession;
import com.hackathon.second_hand_first.search.domain.SearchSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SearchSessionRepository extends JpaRepository<SearchSession, Long> {

    Page<SearchSession> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Optional<SearchSession> findBySessionIdAndUserId(String sessionId, Long userId);

    long countByUserIdAndStatus(Long userId, SearchSessionStatus status);
}
