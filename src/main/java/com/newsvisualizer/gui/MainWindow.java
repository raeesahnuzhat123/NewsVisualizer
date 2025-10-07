package com.newsvisualizer.gui;

import com.newsvisualizer.model.NewsArticle;
import com.newsvisualizer.model.NewsResponse;
import com.newsvisualizer.service.NewsApiService;
import com.newsvisualizer.utils.NewsAnalyzer;
import com.newsvisualizer.visualization.ChartGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Main window for the News Visualizer application
 */
public class MainWindow extends JFrame {
    private NewsApiService newsService;
    private List<NewsArticle> currentArticles;
    
    // GUI Components
    private JTextField searchField;
    private JComboBox<String> countryCombo;
    private JComboBox<String> categoryCombo;
    private JButton searchButton;
    private JButton analyzeButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JTabbedPane tabbedPane;
    private DefaultTableModel tableModel;
    private JTable articlesTable;
    
    public MainWindow() {
        initializeServices();
        initializeGUI();
        setupEventHandlers();
        setLocationRelativeTo(null);
    }
    
    private void initializeServices() {
        newsService = new NewsApiService();
    }
    
    private void initializeGUI() {
        setTitle("News Visualizer - Analyze News Data & Trends");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());
        
        // Create main panels
        createControlPanel();
        createContentArea();
        createStatusBar();
    }
    
    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlPanel.setBackground(new Color(245, 245, 245));
        
        // Search section
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(new Color(245, 245, 245));
        
        searchPanel.add(new JLabel("Search:"));
        searchField = new JTextField(20);
        searchField.setToolTipText("Enter keywords to search for news articles");
        searchPanel.add(searchField);
        
        searchPanel.add(new JLabel("Country:"));
        countryCombo = new JComboBox<>(new String[]{
            "", "us", "gb", "ca", "au", "de", "fr", "in", "jp", "cn"
        });
        countryCombo.setToolTipText("Select country for top headlines");
        searchPanel.add(countryCombo);
        
        searchPanel.add(new JLabel("Category:"));
        categoryCombo = new JComboBox<>(new String[]{
            "", "business", "entertainment", "general", "health", "science", "sports", "technology"
        });
        categoryCombo.setToolTipText("Select news category");
        searchPanel.add(categoryCombo);
        
        searchButton = new JButton("Fetch News");
        searchButton.setToolTipText("Fetch news articles based on search criteria");
        searchPanel.add(searchButton);
        
        analyzeButton = new JButton("Analyze Data");
        analyzeButton.setToolTipText("Analyze and visualize the fetched news data");
        analyzeButton.setEnabled(false);
        searchPanel.add(analyzeButton);
        
        controlPanel.add(searchPanel, BorderLayout.CENTER);
        
        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        controlPanel.add(progressBar, BorderLayout.SOUTH);
        
        add(controlPanel, BorderLayout.NORTH);
    }
    
    private void createContentArea() {
        tabbedPane = new JTabbedPane();
        
        // Articles table tab
        createArticlesTab();
        
        // Analysis tabs (will be populated after analysis)
        createPlaceholderTabs();
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private void createArticlesTab() {
        // Create table model
        String[] columnNames = {"Title", "Source", "Published", "Sentiment"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        articlesTable = new JTable(tableModel);
        articlesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        articlesTable.getColumnModel().getColumn(0).setPreferredWidth(400);
        articlesTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        articlesTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        articlesTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        
        JScrollPane scrollPane = new JScrollPane(articlesTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("News Articles"));
        
        tabbedPane.addTab("Articles", scrollPane);
    }
    
    private void createPlaceholderTabs() {
        // Sentiment Analysis tab
        JPanel sentimentPanel = new JPanel(new BorderLayout());
        sentimentPanel.add(new JLabel("Fetch and analyze news to view sentiment analysis", SwingConstants.CENTER));
        tabbedPane.addTab("Sentiment Analysis", sentimentPanel);
        
        // Source Distribution tab
        JPanel sourcePanel = new JPanel(new BorderLayout());
        sourcePanel.add(new JLabel("Fetch and analyze news to view source distribution", SwingConstants.CENTER));
        tabbedPane.addTab("Source Distribution", sourcePanel);
        
        // Keywords tab
        JPanel keywordPanel = new JPanel(new BorderLayout());
        keywordPanel.add(new JLabel("Fetch and analyze news to view keyword analysis", SwingConstants.CENTER));
        tabbedPane.addTab("Keywords", keywordPanel);
        
        // Timeline tab
        JPanel timelinePanel = new JPanel(new BorderLayout());
        timelinePanel.add(new JLabel("Fetch and analyze news to view publication timeline", SwingConstants.CENTER));
        tabbedPane.addTab("Timeline", timelinePanel);
    }
    
    private void createStatusBar() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusLabel = new JLabel("Ready - Enter search criteria and click 'Fetch News'");
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchNews();
            }
        });
        
        analyzeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                analyzeData();
            }
        });
        
        // Enter key in search field triggers search
        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchNews();
            }
        });
    }
    
    private void fetchNews() {
        // Run in background thread to not block UI
        SwingWorker<NewsResponse, Void> worker = new SwingWorker<NewsResponse, Void>() {
            @Override
            protected NewsResponse doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    searchButton.setEnabled(false);
                    analyzeButton.setEnabled(false);
                    progressBar.setIndeterminate(true);
                    progressBar.setString("Fetching news...");
                    statusLabel.setText("Fetching news articles...");
                });
                
                String query = searchField.getText().trim();
                String country = (String) countryCombo.getSelectedItem();
                String category = (String) categoryCombo.getSelectedItem();
                
                NewsResponse response;
                
                if (!query.isEmpty()) {
                    // Search for specific query
                    response = newsService.searchNews(query, "publishedAt", "en");
                } else if (!country.isEmpty() || !category.isEmpty()) {
                    // Get top headlines
                    response = newsService.getTopHeadlines(country, category);
                } else {
                    // Default: get general news
                    response = newsService.getTopHeadlines("us", "");
                }
                
                return response;
            }
            
            @Override
            protected void done() {
                try {
                    NewsResponse response = get();
                    processNewsResponse(response);
                } catch (Exception e) {
                    showError("Error fetching news: " + e.getMessage());
                } finally {
                    searchButton.setEnabled(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("Ready");
                }
            }
        };
        
        worker.execute();
    }
    
    private void processNewsResponse(NewsResponse response) {
        if (response.isSuccess() && response.hasArticles()) {
            currentArticles = response.getArticles();
            updateArticlesTable();
            analyzeButton.setEnabled(true);
            statusLabel.setText("Fetched " + currentArticles.size() + " articles. Click 'Analyze Data' to generate visualizations.");
        } else {
            String message = response.getMessage() != null ? response.getMessage() : "No articles found";
            showError("Error: " + message);
            statusLabel.setText("Error fetching news");
        }
    }
    
    private void updateArticlesTable() {
        tableModel.setRowCount(0); // Clear existing data
        
        for (NewsArticle article : currentArticles) {
            Object[] row = {
                truncateText(article.getTitle(), 60),
                article.getSource() != null ? article.getSource().getName() : "Unknown",
                article.getPublishedAt() != null ? 
                    article.getPublishedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Unknown",
                article.getSentimentText()
            };
            tableModel.addRow(row);
        }
    }
    
    private void analyzeData() {
        if (currentArticles == null || currentArticles.isEmpty()) {
            showError("No articles to analyze. Please fetch news first.");
            return;
        }
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> {
                    analyzeButton.setEnabled(false);
                    progressBar.setIndeterminate(true);
                    progressBar.setString("Analyzing data...");
                    statusLabel.setText("Analyzing news data and generating visualizations...");
                });
                
                // Perform sentiment analysis
                NewsAnalyzer.analyzeSentiment(currentArticles);
                
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    generateVisualizations();
                    updateArticlesTable(); // Refresh table with sentiment data
                    analyzeButton.setEnabled(true);
                    statusLabel.setText("Analysis complete. View results in the different tabs.");
                } catch (Exception e) {
                    showError("Error analyzing data: " + e.getMessage());
                } finally {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("Ready");
                }
            }
        };
        
        worker.execute();
    }
    
    private void generateVisualizations() {
        // Generate analysis data
        Map<String, Integer> sentimentDist = NewsAnalyzer.getSentimentDistribution(currentArticles);
        Map<String, Integer> sourceDist = NewsAnalyzer.getSourceDistribution(currentArticles);
        Map<String, Integer> keywords = NewsAnalyzer.extractKeywords(currentArticles, 20);
        Map<String, List<NewsArticle>> dateGroups = NewsAnalyzer.groupByDate(currentArticles);
        
        // Convert date groups to timeline data
        Map<String, Integer> timeline = NewsAnalyzer.getPublicationTimeline(currentArticles);
        
        // Calculate summary statistics
        double avgSentiment = NewsAnalyzer.getAverageSentiment(currentArticles);
        int uniqueSources = sourceDist.size();
        String topKeyword = keywords.isEmpty() ? "N/A" : keywords.keySet().iterator().next();
        
        // Create summary panel
        JPanel summaryPanel = ChartGenerator.createSummaryPanel(
            currentArticles.size(), avgSentiment, uniqueSources, topKeyword);
        
        // Update tabs with visualizations
        // Sentiment Analysis tab
        JPanel sentimentPanel = new JPanel(new BorderLayout());
        sentimentPanel.add(summaryPanel, BorderLayout.NORTH);
        if (!sentimentDist.isEmpty()) {
            JPanel chartPanel = ChartGenerator.createSentimentChart("Sentiment Distribution", sentimentDist);
            sentimentPanel.add(chartPanel, BorderLayout.CENTER);
        }
        tabbedPane.setComponentAt(1, sentimentPanel);
        
        // Source Distribution tab
        if (!sourceDist.isEmpty()) {
            JPanel sourceChart = ChartGenerator.createHorizontalBarChart(
                "Articles by Source", "Sources", "Number of Articles", sourceDist);
            tabbedPane.setComponentAt(2, sourceChart);
        }
        
        // Keywords tab
        if (!keywords.isEmpty()) {
            JPanel keywordChart = ChartGenerator.createWordFrequencyChart(
                "Top Keywords", keywords);
            tabbedPane.setComponentAt(3, keywordChart);
        }
        
        // Timeline tab
        if (!timeline.isEmpty()) {
            // For timeline, we need date-only data
            Map<String, Integer> dateData = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, List<NewsArticle>> entry : dateGroups.entrySet()) {
                dateData.put(entry.getKey(), entry.getValue().size());
            }
            
            if (!dateData.isEmpty()) {
                JPanel timelineChart = ChartGenerator.createTimeSeriesChart(
                    "Publication Timeline", "Date", "Number of Articles", dateData);
                tabbedPane.setComponentAt(4, timelineChart);
            }
        }
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
    
    @Override
    public void dispose() {
        if (newsService != null) {
            newsService.close();
        }
        super.dispose();
    }
}
