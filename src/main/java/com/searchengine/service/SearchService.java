package com.searchengine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Service
public class SearchService {

    @Value("${google.search.api-key}")
    private String apiKey;

    @Value("${google.search.engine-id}")
    private String searchEngineId;

    // Cache the searches to prevent redundant external API queries
    @Cacheable(value = "searches", key = "#query + '-' + #searchType + '-' + #safeSearch")
    public String executeSearch(String query, String searchType, String safeSearch) {
        System.out.println("CACHE MISS: Performing remote Google Custom Search query for: \"" + query + "\" [type=" + searchType + ", safe=" + safeSearch + "]");
        try {
            String searchUrl = "https://www.googleapis.com/customsearch/v1?q="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&key=" + apiKey
                    + "&cx=" + searchEngineId;

            if ("active".equalsIgnoreCase(safeSearch)) {
                searchUrl += "&safe=active";
            }
            if ("image".equalsIgnoreCase(searchType)) {
                searchUrl += "&searchType=image";
            }

            URL url = new URL(searchUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                throw new RuntimeException("Google Custom Search API returned HTTP code: " + responseCode);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred during search fetch: " + ex.getMessage(), ex);
        }
    }

    public void updateCredentials(String apiKey, String searchEngineId) {
        this.apiKey = apiKey;
        this.searchEngineId = searchEngineId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSearchEngineId() {
        return searchEngineId;
    }
}
