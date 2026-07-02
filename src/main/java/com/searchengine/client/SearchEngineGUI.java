package com.searchengine.client;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

public class SearchEngineGUI extends JFrame {
    private static final String BASE_URL = "http://localhost:8080/api";
    
    // Client UI configurations
    private Properties clientConfig = new Properties();
    private static final String CLIENT_CONFIG_FILE = "client_config.properties";

    // Panels
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContentPanel = new JPanel(cardLayout);

    // Search inputs
    private JTextField homeSearchField;
    private JTextField resultSearchField;

    // Filters and Settings UI
    private JRadioButton webSearchRadio;
    private JRadioButton imageSearchRadio;
    private JCheckBox safeSearchCheck;
    private JComboBox<String> themeCombo;
    private DefaultListModel<String> historyModel = new DefaultListModel<>();
    private JList<String> historyListUI = new JList<>(historyModel);

    // Results area
    private JTextPane resultPane;

    public SearchEngineGUI() {
        // Load client properties
        loadClientProperties();

        setTitle("Explore-It Client");
        setSize(950, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // Create UI components
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        JPanel homeCard = createHomeCard();
        JPanel resultsCard = createResultsCard();

        mainContentPanel.add(homeCard, "HOME");
        mainContentPanel.add(resultsCard, "RESULTS");
        add(mainContentPanel, BorderLayout.CENTER);

        // Fetch configurations and history from backend REST service
        SwingWorker<Void, Void> initWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                loadHistoryFromBackend();
                return null;
            }
        };
        initWorker.execute();

        // Apply theme settings
        SwingUtilities.invokeLater(() -> updateTheme(clientConfig.getProperty("theme", "dark")));
    }

    private void loadClientProperties() {
        try (FileInputStream input = new FileInputStream(CLIENT_CONFIG_FILE)) {
            clientConfig.load(input);
        } catch (Exception ex) {
            clientConfig.setProperty("theme", "dark");
            saveClientProperties();
        }
    }

    private void saveClientProperties() {
        try (FileOutputStream output = new FileOutputStream(CLIENT_CONFIG_FILE)) {
            clientConfig.store(output, "Explore-It Local Client Configuration");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Sidebar panel containing Settings, Filters, and History
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new GridBagLayout());
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(80, 80, 80)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.weightx = 1.0;

        // Section 1: Filters & Themes
        JPanel filterPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        filterPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)), 
                "Options & Theme", TitledBorder.LEFT, TitledBorder.TOP, 
                new Font("Segoe UI", Font.BOLD, 12)
        ));

        webSearchRadio = new JRadioButton("Web Search", true);
        imageSearchRadio = new JRadioButton("Image Search", false);
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(webSearchRadio);
        typeGroup.add(imageSearchRadio);
        filterPanel.add(webSearchRadio);
        filterPanel.add(imageSearchRadio);

        safeSearchCheck = new JCheckBox("SafeSearch", false);
        filterPanel.add(safeSearchCheck);

        JPanel themePanel = new JPanel(new BorderLayout(5, 0));
        themePanel.add(new JLabel("Theme:"), BorderLayout.WEST);
        themeCombo = new JComboBox<>(new String[]{"Dark Theme", "Light Theme"});
        themeCombo.setSelectedIndex("light".equalsIgnoreCase(clientConfig.getProperty("theme", "dark")) ? 1 : 0);
        themePanel.add(themeCombo, BorderLayout.CENTER);
        filterPanel.add(themePanel);

        gbc.gridx = 0; gbc.gridy = 0;
        sidebar.add(filterPanel, gbc);

        // Section 2: Search History
        JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
        historyPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)), 
                "Search History (DB)", TitledBorder.LEFT, TitledBorder.TOP, 
                new Font("Segoe UI", Font.BOLD, 12)
        ));

        historyListUI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyListUI.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyListUI.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = historyListUI.getSelectedValue();
                    if (selected != null && !selected.trim().isEmpty()) {
                        homeSearchField.setText(selected);
                        resultSearchField.setText(selected);
                        executeSearchQuery(selected);
                    }
                }
            }
        });

        JScrollPane historyScroll = new JScrollPane(historyListUI);
        historyScroll.setPreferredSize(new Dimension(0, 160));
        historyPanel.add(historyScroll, BorderLayout.CENTER);

        JButton clearHistoryBtn = new JButton("Clear DB Logs");
        clearHistoryBtn.putClientProperty("JButton.buttonType", "roundRect");
        clearHistoryBtn.addActionListener(e -> clearHistoryFromBackend());
        historyPanel.add(clearHistoryBtn, BorderLayout.SOUTH);

        gbc.gridy = 1;
        sidebar.add(historyPanel, gbc);

        // Section 3: Credentials Panel
        JPanel credsPanel = new JPanel(new GridBagLayout());
        credsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)), 
                "REST Backend Settings", TitledBorder.LEFT, TitledBorder.TOP, 
                new Font("Segoe UI", Font.BOLD, 12)
        ));

        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.fill = GridBagConstraints.HORIZONTAL;
        cGbc.insets = new Insets(4, 6, 4, 6);
        cGbc.weightx = 1.0;

        JTextField apiKeyField = new JTextField();
        apiKeyField.putClientProperty("JTextField.placeholderText", "Fetching from API...");
        JTextField searchIdField = new JTextField();
        searchIdField.putClientProperty("JTextField.placeholderText", "Fetching from API...");

        cGbc.gridx = 0; cGbc.gridy = 0;
        credsPanel.add(new JLabel("API Key:"), cGbc);
        cGbc.gridy = 1;
        credsPanel.add(apiKeyField, cGbc);
        cGbc.gridy = 2;
        credsPanel.add(new JLabel("Search Engine ID:"), cGbc);
        cGbc.gridy = 3;
        credsPanel.add(searchIdField, cGbc);

        JButton saveCredsBtn = new JButton("Push to Server");
        saveCredsBtn.putClientProperty("JButton.buttonType", "roundRect");
        saveCredsBtn.addActionListener(e -> {
            String key = apiKeyField.getText().trim();
            String cx = searchIdField.getText().trim();
            saveCredentialsToBackend(key, cx);
        });
        cGbc.gridy = 4;
        cGbc.insets = new Insets(10, 6, 4, 6);
        credsPanel.add(saveCredsBtn, cGbc);

        gbc.gridy = 2;
        sidebar.add(credsPanel, gbc);

        // Spacers
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        sidebar.add(Box.createGlue(), gbc);

        // Dynamic theme combobox listener
        themeCombo.addActionListener(e -> {
            String theme = themeCombo.getSelectedIndex() == 1 ? "light" : "dark";
            clientConfig.setProperty("theme", theme);
            saveClientProperties();
            updateTheme(theme);
        });

        // Load credentials from backend asynchronously on startup
        SwingWorker<Void, Void> credsWorker = new SwingWorker<>() {
            private String key = "";
            private String cx = "";

            @Override
            protected Void doInBackground() {
                try {
                    String json = sendGetRequest(BASE_URL + "/config");
                    JSONObject obj = new JSONObject(json);
                    key = obj.optString("apiKey");
                    cx = obj.optString("searchId");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                apiKeyField.setText(key);
                searchIdField.setText(cx);
            }
        };
        credsWorker.execute();

        return sidebar;
    }

    private JPanel createHomeCard() {
        JPanel homeCard = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 20, 15, 20);

        ImageIcon logoIcon = null;
        try {
            URL imgUrl = SearchEngineGUI.class.getResource("finallogo.jpg");
            if (imgUrl == null) imgUrl = SearchEngineGUI.class.getResource("logo.jpg");
            if (imgUrl == null) imgUrl = SearchEngineGUI.class.getResource("LOGO1.jpg");
            if (imgUrl != null) {
                logoIcon = new ImageIcon(imgUrl);
                Image scaledImage = logoIcon.getImage().getScaledInstance(140, 140, Image.SCALE_SMOOTH);
                logoIcon = new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JLabel logoLabel;
        if (logoIcon != null) {
            logoLabel = new JLabel(logoIcon);
        } else {
            logoLabel = new JLabel("Explore-It", SwingConstants.CENTER);
            logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
            logoLabel.setForeground(new Color(138, 180, 248));
        }
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        homeSearchField = new JTextField();
        homeSearchField.setPreferredSize(new Dimension(450, 44));
        homeSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        homeSearchField.putClientProperty("JTextField.placeholderText", "Search via Spring Boot backend REST service...");
        homeSearchField.putClientProperty("JTextField.showClearButton", true);
        homeSearchField.putClientProperty("componentRound", 24);

        JButton homeSearchButton = new JButton("Explore");
        homeSearchButton.setPreferredSize(new Dimension(140, 40));
        homeSearchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        homeSearchButton.putClientProperty("componentRound", 20);
        homeSearchButton.putClientProperty("JButton.buttonType", "accent");
        homeSearchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        homeCard.add(logoLabel, gbc);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        homeCard.add(homeSearchField, gbc);

        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        homeCard.add(homeSearchButton, gbc);

        ActionListener triggerAction = e -> {
            String query = homeSearchField.getText().trim();
            if (!query.isEmpty()) {
                resultSearchField.setText(query);
                executeSearchQuery(query);
            }
        };
        homeSearchButton.addActionListener(triggerAction);
        homeSearchField.addActionListener(triggerAction);

        return homeCard;
    }

    private JPanel createResultsCard() {
        JPanel resultsCard = new JPanel(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JButton backButton = new JButton("Home");
        backButton.putClientProperty("JButton.buttonType", "roundRect");
        backButton.setPreferredSize(new Dimension(80, 36));
        backButton.addActionListener(e -> {
            homeSearchField.setText("");
            cardLayout.show(mainContentPanel, "HOME");
        });

        resultSearchField = new JTextField();
        resultSearchField.setPreferredSize(new Dimension(0, 36));
        resultSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        resultSearchField.putClientProperty("JTextField.placeholderText", "Search something else...");
        resultSearchField.putClientProperty("JTextField.showClearButton", true);
        resultSearchField.putClientProperty("componentRound", 18);

        JButton resultSearchButton = new JButton("Search");
        resultSearchButton.putClientProperty("JButton.buttonType", "accent");
        resultSearchButton.setPreferredSize(new Dimension(100, 36));

        topBar.add(backButton, BorderLayout.WEST);
        topBar.add(resultSearchField, BorderLayout.CENTER);
        topBar.add(resultSearchButton, BorderLayout.EAST);

        resultsCard.add(topBar, BorderLayout.NORTH);

        resultPane = new JTextPane();
        resultPane.setContentType("text/html");
        resultPane.setEditable(false);

        resultPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        resultsCard.add(scrollPane, BorderLayout.CENTER);

        ActionListener triggerAction = e -> {
            String query = resultSearchField.getText().trim();
            if (!query.isEmpty()) {
                homeSearchField.setText(query);
                executeSearchQuery(query);
            }
        };
        resultSearchButton.addActionListener(triggerAction);
        resultSearchField.addActionListener(triggerAction);

        return resultsCard;
    }

    // Call local Spring Boot REST endpoint to perform search
    private void executeSearchQuery(String query) {
        cardLayout.show(mainContentPanel, "RESULTS");
        resultPane.setText("<html><body style='font-family:sans-serif; color:#bdbdbd; margin:20px;'><h3>Connecting to backend service...</h3></body></html>");

        SwingWorker<String, Void> searchWorker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    String searchType = imageSearchRadio.isSelected() ? "image" : "web";
                    String safeSearch = safeSearchCheck.isSelected() ? "active" : "off";

                    String urlStr = BASE_URL + "/search?q=" + URLEncoder.encode(query, "UTF-8")
                            + "&type=" + searchType + "&safe=" + safeSearch;

                    String jsonResponse = sendGetRequest(urlStr);
                    return renderJsonToHtml(jsonResponse, "image".equals(searchType));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return "<html><body style='font-family:sans-serif; color:red; margin:20px;'><h3>Error calling backend REST API</h3><p>" 
                            + ex.getMessage() + "</p></body></html>";
                }
            }

            @Override
            protected void done() {
                try {
                    resultPane.setText(get());
                    resultPane.setCaretPosition(0);
                    // Refresh history list automatically after search logs database
                    loadHistoryFromBackend();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        searchWorker.execute();
    }

    private void loadHistoryFromBackend() {
        try {
            String json = sendGetRequest(BASE_URL + "/history");
            JSONArray arr = new JSONArray(json);
            SwingUtilities.invokeLater(() -> {
                historyModel.clear();
                for (int i = 0; i < arr.length(); i++) {
                    historyModel.addElement(arr.getString(i));
                }
            });
        } catch (Exception ex) {
            System.err.println("Could not load history from backend: " + ex.getMessage());
        }
    }

    private void clearHistoryFromBackend() {
        try {
            sendDeleteRequest(BASE_URL + "/history");
            historyModel.clear();
            JOptionPane.showMessageDialog(this, "Search database history logs cleared!", "Explore-It Client", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to clear database logs: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveCredentialsToBackend(String apiKey, String searchId) {
        try {
            String urlStr = BASE_URL + "/config?apiKey=" + URLEncoder.encode(apiKey, "UTF-8")
                    + "&searchId=" + URLEncoder.encode(searchId, "UTF-8");
            sendPostRequest(urlStr);
            JOptionPane.showMessageDialog(this, "API Credentials updated on backend server!", "Explore-It Client", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to sync credentials: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // JSON Parser to display HTML
    private String renderJsonToHtml(String jsonString, boolean isImageSearch) {
        try {
            JSONObject jsonResponse = new JSONObject(jsonString);
            if (jsonResponse.has("error")) {
                return "<html><body style='font-family:sans-serif; color:red; margin:20px;'><h3>Backend Error</h3><p>" 
                        + jsonResponse.optString("error") + "</p></body></html>";
            }
            if (!jsonResponse.has("items")) {
                return "<html><body style='font-family:sans-serif; color:#bdbdbd; margin:20px;'><h3>No search results found.</h3></body></html>";
            }

            JSONArray items = jsonResponse.getJSONArray("items");
            StringBuilder html = new StringBuilder();
            boolean isDark = "dark".equalsIgnoreCase(clientConfig.getProperty("theme", "dark"));

            String bgColor = isDark ? "#1e1e1e" : "#ffffff";
            String cardColor = isDark ? "#252526" : "#f1f3f4";
            String textColor = isDark ? "#e0e0e0" : "#202124";
            String linkColor = isDark ? "#8ab4f8" : "#1a0dab";
            String snippetColor = isDark ? "#bdc1c6" : "#4d5156";
            String borderColor = isDark ? "#3c3c3c" : "#e0e0e0";

            html.append("<html><head><style>")
                .append("body { font-family: 'Segoe UI', sans-serif; background-color: ").append(bgColor).append("; color: ").append(textColor).append("; margin: 20px; line-height: 1.5; }")
                .append(".container { max-width: 720px; margin: 0 auto; }")
                .append(".result { margin-bottom: 18px; padding: 15px; background-color: ").append(cardColor).append("; border-radius: 8px; border: 1px solid ").append(borderColor).append("; }")
                .append(".site-url { font-size: 11px; color: #34a853; margin-bottom: 4px; word-break: break-all; }")
                .append(".title { font-size: 17px; font-weight: bold; margin-bottom: 6px; }")
                .append(".title a { color: ").append(linkColor).append("; text-decoration: none; }")
                .append(".title a:hover { text-decoration: underline; }")
                .append(".snippet { font-size: 13px; color: ").append(snippetColor).append("; }")
                .append(".img-grid { display: block; width: 100%; text-align: left; }")
                .append(".img-card { display: inline-block; width: 150px; margin: 8px; padding: 6px; background-color: ").append(cardColor).append("; border: 1px solid ").append(borderColor).append("; border-radius: 8px; text-align: center; vertical-align: top; }")
                .append(".img-card img { border-radius: 4px; max-width: 130px; max-height: 130px; }")
                .append(".img-title { font-size: 10px; color: ").append(snippetColor).append("; margin-top: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }")
                .append("</style></head><body><div class='container'>");

            if (isImageSearch) {
                html.append("<div class='img-grid'>");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String link = item.getString("link");
                    String title = item.getString("title");
                    String context = item.has("image") && item.getJSONObject("image").has("contextLink") 
                            ? item.getJSONObject("image").getString("contextLink") 
                            : link;

                    html.append("<div class='img-card'>")
                        .append("<a href='").append(context).append("'><img src='").append(link).append("'></a>")
                        .append("<div class='img-title'>").append(title).append("</div>")
                        .append("</div>");
                }
                html.append("</div>");
            } else {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String title = item.getString("title");
                    String link = item.getString("link");
                    String snippet = item.has("snippet") ? item.getString("snippet") : "";

                    html.append("<div class='result'>")
                        .append("<div class='site-url'>").append(link).append("</div>")
                        .append("<div class='title'><a href='").append(link).append("'>").append(title).append("</a></div>")
                        .append("<div class='snippet'>").append(snippet).append("</div>")
                        .append("</div>");
                }
            }

            html.append("</div></body></html>");
            return html.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "<html><body><h3>Error parsing search response</h3></body></html>";
        }
    }

    // Helper HTTP Request methods
    private String sendGetRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);

        int code = conn.getResponseCode();
        if (code == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            return response.toString();
        } else {
            throw new RuntimeException("Server responded with HTTP error: " + code);
        }
    }

    private String sendPostRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setDoOutput(true);

        int code = conn.getResponseCode();
        if (code == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            return response.toString();
        } else {
            throw new RuntimeException("Server responded with HTTP error: " + code);
        }
    }

    private void sendDeleteRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Server responded with HTTP error: " + code);
        }
    }

    private void updateTheme(String themeName) {
        try {
            if ("light".equalsIgnoreCase(themeName)) {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            } else {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
            }
            SwingUtilities.updateComponentTreeUI(this);
            String currentQuery = resultSearchField.getText().trim();
            if (!currentQuery.isEmpty()) {
                executeSearchQuery(currentQuery);
            }
        } catch (Exception ex) {
            System.err.println("Could not toggle LookAndFeel: " + ex.getMessage());
        }
    }
}
