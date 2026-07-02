package com.searchengine.repository;

import com.searchengine.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    // Retrieve searches ordered by most recent first
    List<SearchHistory> findAllByOrderByTimestampDesc();
}
