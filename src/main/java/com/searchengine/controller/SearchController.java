package com.searchengine.controller;

import com.searchengine.entity.SearchHistory;
import com.searchengine.repository.SearchHistoryRepository;
import com.searchengine.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    // REST Endpoint to execute searches and log queries to H2 Database
    @GetMapping(value = "/search", produces = "application/json")
    public ResponseEntity<String> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "web") String type,
            @RequestParam(defaultValue = "off") String safe) {

        String query = q.trim();
        if (query.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\":\"Query is empty\"}");
        }

        // 1. Log query to database
        try {
            SearchHistory history = new SearchHistory(query, type);
            searchHistoryRepository.save(history);
        } catch (Exception ex) {
            System.err.println("Could not save search history: " + ex.getMessage());
        }

        // 2. Perform external search (Cached)
        try {
            String jsonResult = searchService.executeSearch(query, type, safe);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("{\"error\":\"" + ex.getMessage() + "\"}");
        }
    }

    // REST Endpoint to fetch queries history (Unique, most recent first)
    @GetMapping("/history")
    public ResponseEntity<List<String>> getHistory() {
        List<SearchHistory> histories = searchHistoryRepository.findAllByOrderByTimestampDesc();
        // Return unique query strings in order of occurrence
        List<String> uniqueQueries = histories.stream()
                .map(SearchHistory::getQueryText)
                .distinct()
                .limit(10) // Limit to 10 items
                .collect(Collectors.toList());
        return ResponseEntity.ok(uniqueQueries);
    }

    // REST Endpoint to clear search log history
    @DeleteMapping("/history")
    public ResponseEntity<String> clearHistory() {
        searchHistoryRepository.deleteAll();
        return ResponseEntity.ok("{\"message\":\"History cleared successfully\"}");
    }

    // REST Endpoint to update API keys dynamically
    @PostMapping("/config")
    public ResponseEntity<Map<String, String>> updateConfig(
            @RequestParam String apiKey,
            @RequestParam String searchId) {

        searchService.updateCredentials(apiKey.trim(), searchId.trim());
        return ResponseEntity.ok(Map.of(
                "apiKey", searchService.getApiKey(),
                "searchId", searchService.getSearchEngineId()
        ));
    }

    // REST Endpoint to retrieve active API keys configs
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "apiKey", searchService.getApiKey(),
                "searchId", searchService.getSearchEngineId()
        ));
    }
}
