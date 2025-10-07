package com.newsvisualizer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsvisualizer.model.NewsResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for fetching news data from external APIs
 */
public class NewsApiService {
    private static final Logger logger = LoggerFactory.getLogger(NewsApiService.class);
    
    // NewsAPI.org endpoints
    private static final String BASE_URL = "https://newsapi.org/v2";
    private static final String EVERYTHING_ENDPOINT = BASE_URL + "/everything";
    private static final String TOP_HEADLINES_ENDPOINT = BASE_URL + "/top-headlines";
    private static final String SOURCES_ENDPOINT = BASE_URL + "/sources";
    
    // Demo API key - replace with your actual API key
    private static final String API_KEY = "YOUR_API_KEY_HERE";
    
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public NewsApiService() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .build();
        
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Fetch top headlines for a specific country
     */
    public NewsResponse getTopHeadlines(String country, String category) {
        try {
            Map<String, String> params = new HashMap<>();
            if (country != null && !country.isEmpty()) {
                params.put("country", country);
            }
            if (category != null && !category.isEmpty()) {
                params.put("category", category);
            }
            params.put("pageSize", "100");
            
            String url = buildUrl(TOP_HEADLINES_ENDPOINT, params);
            return fetchNewsData(url);
        } catch (Exception e) {
            logger.error("Error fetching top headlines", e);
            return createErrorResponse("Failed to fetch top headlines: " + e.getMessage());
        }
    }
    
    /**
     * Search for news articles by keyword
     */
    public NewsResponse searchNews(String query, String sortBy, String language) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("q", query);
            if (sortBy != null && !sortBy.isEmpty()) {
                params.put("sortBy", sortBy);
            }
            if (language != null && !language.isEmpty()) {
                params.put("language", language);
            }
            params.put("pageSize", "100");
            
            String url = buildUrl(EVERYTHING_ENDPOINT, params);
            return fetchNewsData(url);
        } catch (Exception e) {
            logger.error("Error searching news", e);
            return createErrorResponse("Failed to search news: " + e.getMessage());
        }
    }
    
    /**
     * Get news from specific sources
     */
    public NewsResponse getNewsBySources(String sources) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("sources", sources);
            params.put("pageSize", "100");
            
            String url = buildUrl(EVERYTHING_ENDPOINT, params);
            return fetchNewsData(url);
        } catch (Exception e) {
            logger.error("Error fetching news by sources", e);
            return createErrorResponse("Failed to fetch news by sources: " + e.getMessage());
        }
    }
    
    /**
     * Get news for a specific date range
     */
    public NewsResponse getNewsInDateRange(String query, LocalDate from, LocalDate to) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("q", query != null ? query : "*");
            params.put("from", from.format(DateTimeFormatter.ISO_LOCAL_DATE));
            params.put("to", to.format(DateTimeFormatter.ISO_LOCAL_DATE));
            params.put("sortBy", "publishedAt");
            params.put("pageSize", "100");
            
            String url = buildUrl(EVERYTHING_ENDPOINT, params);
            return fetchNewsData(url);
        } catch (Exception e) {
            logger.error("Error fetching news in date range", e);
            return createErrorResponse("Failed to fetch news in date range: " + e.getMessage());
        }
    }
    
    /**
     * Build URL with parameters
     */
    private String buildUrl(String baseUrl, Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl);
        url.append("?apiKey=").append(API_KEY);
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                url.append("&")
                   .append(entry.getKey())
                   .append("=")
                   .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        
        return url.toString();
    }
    
    /**
     * Fetch news data from URL
     */
    private NewsResponse fetchNewsData(String url) {
        try {
            logger.info("Fetching news from: {}", url.replaceAll("apiKey=[^&]*", "apiKey=***"));
            
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "NewsVisualizer/1.0");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String jsonResponse = EntityUtils.toString(entity);
                    NewsResponse newsResponse = objectMapper.readValue(jsonResponse, NewsResponse.class);
                    
                    logger.info("Fetched {} articles", 
                        newsResponse.getArticles() != null ? newsResponse.getArticles().size() : 0);
                    
                    return newsResponse;
                }
            }
        } catch (IOException e) {
            logger.error("IO error while fetching news", e);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching news", e);
        }
        
        return createErrorResponse("Failed to fetch news data");
    }
    
    /**
     * Create error response
     */
    private NewsResponse createErrorResponse(String message) {
        NewsResponse response = new NewsResponse();
        response.setStatus("error");
        response.setMessage(message);
        response.setTotalResults(0);
        return response;
    }
    
    /**
     * Create demo/mock news data for testing when API is not available
     */
    public NewsResponse getMockNewsData() {
        NewsResponse response = new NewsResponse();
        response.setStatus("ok");
        response.setTotalResults(3);
        
        // This would contain mock articles for demonstration
        // In a real implementation, you'd populate this with actual mock data
        logger.info("Using mock news data for demonstration");
        
        return response;
    }
    
    /**
     * Close resources
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            logger.error("Error closing HTTP client", e);
        }
    }
    
    /**
     * Check if API key is configured
     */
    public boolean isApiKeyConfigured() {
        return API_KEY != null && !API_KEY.equals("YOUR_API_KEY_HERE") && !API_KEY.isEmpty();
    }
}