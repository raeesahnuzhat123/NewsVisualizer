package com.newsvisualizer.utils;

import com.newsvisualizer.model.NewsArticle;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for analyzing news data
 */
public class NewsAnalyzer {
    
    // Simple sentiment words for basic analysis
    private static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
        "good", "great", "excellent", "amazing", "wonderful", "fantastic", "positive",
        "success", "win", "victory", "achievement", "breakthrough", "progress", "growth",
        "love", "happy", "joy", "celebrate", "triumph", "benefit", "gain", "improve"
    ));
    
    private static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
        "bad", "terrible", "awful", "horrible", "negative", "fail", "failure", "loss",
        "defeat", "crisis", "problem", "issue", "concern", "worry", "fear", "decline",
        "disaster", "tragedy", "death", "kill", "war", "conflict", "attack", "threat"
    ));
    
    /**
     * Perform basic sentiment analysis on news articles
     */
    public static void analyzeSentiment(List<NewsArticle> articles) {
        for (NewsArticle article : articles) {
            double sentiment = calculateSentiment(article);
            article.setSentimentScore(sentiment);
        }
    }
    
    /**
     * Calculate sentiment score for a single article
     */
    private static double calculateSentiment(NewsArticle article) {
        String text = combineTextContent(article).toLowerCase();
        String[] words = text.split("\\W+");
        
        int positiveCount = 0;
        int negativeCount = 0;
        
        for (String word : words) {
            if (POSITIVE_WORDS.contains(word)) {
                positiveCount++;
            } else if (NEGATIVE_WORDS.contains(word)) {
                negativeCount++;
            }
        }
        
        int totalWords = words.length;
        if (totalWords == 0) return 0.0;
        
        double positiveRatio = (double) positiveCount / totalWords;
        double negativeRatio = (double) negativeCount / totalWords;
        
        return positiveRatio - negativeRatio;
    }
    
    /**
     * Combine title, description, and content for analysis
     */
    private static String combineTextContent(NewsArticle article) {
        StringBuilder content = new StringBuilder();
        
        if (StringUtils.isNotBlank(article.getTitle())) {
            content.append(article.getTitle()).append(" ");
        }
        if (StringUtils.isNotBlank(article.getDescription())) {
            content.append(article.getDescription()).append(" ");
        }
        if (StringUtils.isNotBlank(article.getContent())) {
            content.append(article.getContent());
        }
        
        return content.toString();
    }
    
    /**
     * Extract keywords from articles
     */
    public static Map<String, Integer> extractKeywords(List<NewsArticle> articles, int topN) {
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        // Common stop words to ignore
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", 
            "with", "by", "is", "are", "was", "were", "be", "been", "being", "have", 
            "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they"
        ));
        
        for (NewsArticle article : articles) {
            String text = combineTextContent(article).toLowerCase();
            String[] words = text.split("\\W+");
            
            for (String word : words) {
                if (word.length() > 2 && !stopWords.contains(word) && !word.matches("\\d+")) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }
        
        return wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
    }
    
    /**
     * Group articles by publication date
     */
    public static Map<String, List<NewsArticle>> groupByDate(List<NewsArticle> articles) {
        return articles.stream()
                .filter(article -> article.getPublishedAt() != null)
                .collect(Collectors.groupingBy(
                    article -> article.getPublishedAt().toLocalDate().toString(),
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
    }
    
    /**
     * Group articles by source
     */
    public static Map<String, List<NewsArticle>> groupBySource(List<NewsArticle> articles) {
        return articles.stream()
                .collect(Collectors.groupingBy(
                    article -> article.getSource() != null ? article.getSource().getName() : "Unknown",
                    Collectors.toList()
                ));
    }
    
    /**
     * Group articles by sentiment
     */
    public static Map<String, List<NewsArticle>> groupBySentiment(List<NewsArticle> articles) {
        return articles.stream()
                .collect(Collectors.groupingBy(
                    NewsArticle::getSentimentText,
                    Collectors.toList()
                ));
    }
    
    /**
     * Get articles published in the last N hours
     */
    public static List<NewsArticle> getRecentArticles(List<NewsArticle> articles, int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minus(hours, ChronoUnit.HOURS);
        
        return articles.stream()
                .filter(article -> article.getPublishedAt() != null && 
                                 article.getPublishedAt().isAfter(cutoff))
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate average sentiment score
     */
    public static double getAverageSentiment(List<NewsArticle> articles) {
        return articles.stream()
                .mapToDouble(NewsArticle::getSentimentScore)
                .average()
                .orElse(0.0);
    }
    
    /**
     * Get sentiment distribution
     */
    public static Map<String, Integer> getSentimentDistribution(List<NewsArticle> articles) {
        Map<String, Integer> distribution = new HashMap<>();
        
        for (NewsArticle article : articles) {
            String sentiment = article.getSentimentText();
            distribution.put(sentiment, distribution.getOrDefault(sentiment, 0) + 1);
        }
        
        return distribution;
    }
    
    /**
     * Get source distribution
     */
    public static Map<String, Integer> getSourceDistribution(List<NewsArticle> articles) {
        Map<String, Integer> distribution = new HashMap<>();
        
        for (NewsArticle article : articles) {
            String source = article.getSource() != null ? article.getSource().getName() : "Unknown";
            distribution.put(source, distribution.getOrDefault(source, 0) + 1);
        }
        
        return distribution.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
    }
    
    /**
     * Get publication timeline (hourly distribution)
     */
    public static Map<String, Integer> getPublicationTimeline(List<NewsArticle> articles) {
        Map<String, Integer> timeline = new LinkedHashMap<>();
        
        articles.stream()
                .filter(article -> article.getPublishedAt() != null)
                .forEach(article -> {
                    String hour = article.getPublishedAt().toLocalDate().toString() + " " + 
                                 String.format("%02d:00", article.getPublishedAt().getHour());
                    timeline.put(hour, timeline.getOrDefault(hour, 0) + 1);
                });
        
        return timeline;
    }
}