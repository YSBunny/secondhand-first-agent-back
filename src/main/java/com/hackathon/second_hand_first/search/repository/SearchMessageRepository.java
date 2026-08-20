package com.hackathon.second_hand_first.search.repository;

import com.hackathon.second_hand_first.search.domain.SearchMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchMessageRepository extends JpaRepository<SearchMessage, Long> {

    List<SearchMessage> findBySearchSessionIdOrderByCreatedAtAscIdAsc(Long searchSessionId);
}
