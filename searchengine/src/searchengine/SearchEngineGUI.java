package searchengine;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchEngineGUI extends JFrame {
    // Config Persistence
    private Properties config = new Properties();
    private static final String CONFIG_FILE = "config.properties";

    // UI Panels
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContentPanel = new JPanel(cardLayout);

    // Search Fields (Home & Top Bar Results)
    private JTextField homeSearchField;
    private JTextField resultSearchField;

    // Filters and History UI
    private JRadioButton webSearchRadio;
    private JRadioButton imageSearchRadio;
    private JCheckBox safeSearchCheck;
    private JComboBox<String> themeCombo;
    private DefaultListModel<String> historyModel = new DefaultListModel<>();
    private JList<String> historyListUI = new JList<>(historyModel);

    // Results display
    private JTextPane resultPane;

    public SearchEngineGUI() {
        // Load configurations
        loadConfig();

        setTitle("Explore-It");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Define Main layout (Sidebar + Main Content Card Panel)
        setLayout(new BorderLayout());

        // 1. Sidebar Panel
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        // 2. Main Content Card Panel
        JPanel homeCard = createHomeCard();
        JPanel resultsCard = createResultsCard();

        mainContentPanel.add(homeCard, "HOME");
        mainContentPanel.add(resultsCard, "RESULTS");
        add(mainContentPanel, BorderLayout.CENTER);

        // Sync history list UI
        loadHistory();

        // Apply initial theme from config
        SwingUtilities.invokeLater(() -> updateTheme(config.getProperty("theme", "dark")));
    }

    // Load settings from config.properties
    private void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            config.load(input);
        } catch (IOException ex) {
            // Apply defaults
            config.setProperty("api_key", "AIzaSyBARD34CxIvKzxW-R_lWsSkiHivnofPHAk");
            config.setProperty("search_id", "93e33348d9003411c");
            config.setProperty("safesearch", "off");
            config.setProperty("theme", "dark");
            config.setProperty("history", "");
            saveConfig();
        }
    }

    // Save settings to config.properties
    private void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            config.store(output, "Explore-It Configuration Settings");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Create Sidebar Panel on the Left
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new GridBagLayout());
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(80, 80, 80)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.weightx = 1.0;

        // SECTION A: Search Filters & Settings
        JPanel filterPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        filterPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)), 
                "Search Options", TitledBorder.LEFT, TitledBorder.TOP, 
                new Font("Segoe UI", Font.BOLD, 12)
        ));

        webSearchRadio = new JRadioButton("Web Search", "web".equalsIgnoreCase(config.getProperty("search_type", "web")));
        imageSearchRadio = new JRadioButton("Image Search", "image".equalsIgnoreCase(config.getProperty("search_type", "web")));
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(webSearchRadio);
        typeGroup.add(imageSearchRadio);
        filterPanel.add(webSearchRadio);
        filterPanel.add(imageSearchRadio);

        safeSearchCheck = new JCheckBox("SafeSearch", "active".equalsIgnoreCase(config.getProperty("safesearch", "off")));
        filterPanel.add(safeSearchCheck);

        // Theme combobox
        JPanel themePanel = new JPanel(new BorderLayout(5, 0));
        themePanel.add(new JLabel("Theme:"), BorderLayout.WEST);
        themeCombo = new JComboBox<>(new String[]{"Dark Theme", "Light Theme"});
        themeCombo.setSelectedIndex("light".equalsIgnoreCase(config.getProperty("theme", "dark")) ? 1 : 0);
        themePanel.add(themeCombo, BorderLayout.CENTER);
        filterPanel.add(themePanel);

        gbc.gridx = 0;
        gbc.gridy = 0;
        sidebar.add(filterPanel, gbc);

        // SECTION B: History List Panel
        JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
        historyPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)), 
                "Search History", TitledBorder.LEFT, TitledBorder.TOP, 
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
                        triggerSearch(selected);
                    }
                }
            }
        });

        JScrollPane historyScroll = new JScrollPane(historyListUI);
        historyScroll.setPreferredSize(new Dimension(0, 180));
        historyPanel.add(historyScroll, BorderLayout.CENTER);

        JButton clearHistoryBtn = new JButton("Clear History");
        clearHistoryBtn.putClientProperty("JButton.buttonType", "roundRect");
        clearHistoryBtn.addActionListener(e -> {
            historyModel.clear();
            config.setProperty("history", "");
            saveConfig();
        });
        historyPanel.add(clearHistoryBtn, BorderLayout.SOUTH);

        gbc.gridy = 1;
        sidebar.add(historyPanel, gbc);

        // SECTION C: API Credentials Panel
        JPanel credsPanel = new JPanel(new GridBagLayout());
        credsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)), 
                "API Credentials", TitledBorder.LEFT, TitledBorder.TOP, 
                new Font("Segoe UI", Font.BOLD, 12)
        ));

        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.fill = GridBagConstraints.HORIZONTAL;
        cGbc.insets = new Insets(4, 6, 4, 6);
        cGbc.weightx = 1.0;

        JTextField apiKeyField = new JTextField(config.getProperty("api_key"));
        apiKeyField.putClientProperty("JTextField.placeholderText", "Google API Key");
        JTextField searchIdField = new JTextField(config.getProperty("search_id"));
        searchIdField.putClientProperty("JTextField.placeholderText", "Search Engine ID (CX)");

        cGbc.gridx = 0; cGbc.gridy = 0;
        credsPanel.add(new JLabel("API Key:"), cGbc);
        cGbc.gridy = 1;
        credsPanel.add(apiKeyField, cGbc);
        cGbc.gridy = 2;
        credsPanel.add(new JLabel("Search Engine ID:"), cGbc);
        cGbc.gridy = 3;
        credsPanel.add(searchIdField, cGbc);

        JButton saveCredsBtn = new JButton("Save Config");
        saveCredsBtn.putClientProperty("JButton.buttonType", "roundRect");
        saveCredsBtn.addActionListener(e -> {
            config.setProperty("api_key", apiKeyField.getText().trim());
            config.setProperty("search_id", searchIdField.getText().trim());
            saveConfig();
            JOptionPane.showMessageDialog(this, "Credentials saved successfully!", "Explore-It Settings", JOptionPane.INFORMATION_MESSAGE);
        });
        cGbc.gridy = 4;
        cGbc.insets = new Insets(10, 6, 4, 6);
        credsPanel.add(saveCredsBtn, cGbc);

        gbc.gridy = 2;
        sidebar.add(credsPanel, gbc);

        // Sidebar spacers
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        sidebar.add(Box.createGlue(), gbc);

        // Event hooks for instant filter saves
        ActionListener saveFiltersListener = e -> {
            config.setProperty("search_type", imageSearchRadio.isSelected() ? "image" : "web");
            config.setProperty("safesearch", safeSearchCheck.isSelected() ? "active" : "off");
            saveConfig();
        };
        webSearchRadio.addActionListener(saveFiltersListener);
        imageSearchRadio.addActionListener(saveFiltersListener);
        safeSearchCheck.addActionListener(saveFiltersListener);

        themeCombo.addActionListener(e -> {
            String theme = themeCombo.getSelectedIndex() == 1 ? "light" : "dark";
            config.setProperty("theme", theme);
            saveConfig();
            updateTheme(theme);
        });

        return sidebar;
    }

    // Create standard search home card panel
    private JPanel createHomeCard() {
        JPanel homeCard = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 20, 15, 20);

        // Load logo with multiple resource fallback options
        ImageIcon logoIcon = null;
        try {
            URL imgUrl = SearchEngineGUI.class.getResource("finallogo.jpg");
            if (imgUrl == null) imgUrl = SearchEngineGUI.class.getResource("logo.jpg");
            if (imgUrl == null) imgUrl = SearchEngineGUI.class.getResource("LOGO1.jpg");
            if (imgUrl != null) {
                logoIcon = new ImageIcon(imgUrl);
                Image scaledImage = logoIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
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
        homeSearchField.putClientProperty("JTextField.placeholderText", "Search the web or type a URL...");
        homeSearchField.putClientProperty("JTextField.showClearButton", true);
        homeSearchField.putClientProperty("componentRound", 24);

        JButton homeSearchButton = new JButton("Explore");
        homeSearchButton.setPreferredSize(new Dimension(140, 40));
        homeSearchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        homeSearchButton.putClientProperty("componentRound", 20);
        homeSearchButton.putClientProperty("JButton.buttonType", "accent");
        homeSearchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Layout Assembly
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
                triggerSearch(query);
            }
        };
        homeSearchButton.addActionListener(triggerAction);
        homeSearchField.addActionListener(triggerAction);

        return homeCard;
    }

    // Create search results view card panel
    private JPanel createResultsCard() {
        JPanel resultsCard = new JPanel(new BorderLayout());

        // Top bar configuration
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

        // Result Pane initialization
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
                triggerSearch(query);
            }
        };
        resultSearchButton.addActionListener(triggerAction);
        resultSearchField.addActionListener(triggerAction);

        return resultsCard;
    }

    // Trigger Search & UI Card updates
    private void triggerSearch(String query) {
        addQueryToHistory(query);
        cardLayout.show(mainContentPanel, "RESULTS");
        resultPane.setText("<html><body style='font-family:sans-serif; color:#bdbdbd; margin:20px;'><h3>Searching for \"" + query + "\"...</h3></body></html>");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return performSearch(query);
            }

            @Override
            protected void done() {
                try {
                    String htmlResult = get();
                    resultPane.setText(htmlResult);
                    resultPane.setCaretPosition(0);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    resultPane.setText("<html><body style='font-family:sans-serif; color:red; margin:20px;'><h3>Error executing search request.</h3></body></html>");
                }
            }
        };
        worker.execute();
    }

    private String performSearch(String query) {
        try {
            boolean isImageSearch = imageSearchRadio.isSelected();
            String searchUrl = "https://www.googleapis.com/customsearch/v1?q="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&key=" + config.getProperty("api_key")
                    + "&cx=" + config.getProperty("search_id");

            if ("active".equalsIgnoreCase(config.getProperty("safesearch", "off"))) {
                searchUrl += "&safe=active";
            }
            if (isImageSearch) {
                searchUrl += "&searchType=image";
            }

            System.out.println("Executing API Request: " + searchUrl);

            URL url = new URL(searchUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return parseJSONToHTML(response.toString(), isImageSearch);
            } else {
                return "<html><body style='font-family:sans-serif; color:red; margin:20px;'><h3>HTTP Connection Error: " + responseCode + "</h3></body></html>";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "<html><body style='font-family:sans-serif; color:red; margin:20px;'><h3>Connection Exception: " + ex.getMessage() + "</h3></body></html>";
        }
    }

    // Parse Response to Google style Web Results or Image grids
    private String parseJSONToHTML(String jsonString, boolean isImageSearch) {
        try {
            JSONObject jsonResponse = new JSONObject(jsonString);
            if (!jsonResponse.has("items")) {
                return "<html><body style='font-family:sans-serif; color:#bdbdbd; margin:20px;'><h3>No search results found.</h3></body></html>";
            }

            JSONArray items = jsonResponse.getJSONArray("items");
            StringBuilder html = new StringBuilder();
            boolean isDark = "dark".equalsIgnoreCase(config.getProperty("theme", "dark"));

            // CSS Customization
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
                .append(".img-card img { border-radius: 4px; max-width: 130px; max-height: 130px; object-fit: cover; }")
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
            return "<html><body><h3>Error parsing search results.</h3></body></html>";
        }
    }

    // Load history list from settings properties
    private void loadHistory() {
        historyModel.clear();
        String historyStr = config.getProperty("history", "");
        if (!historyStr.isEmpty()) {
            String[] items = historyStr.split("\\|\\|");
            for (String item : items) {
                if (!item.trim().isEmpty()) {
                    historyModel.addElement(item);
                }
            }
        }
    }

    // Add query to history list and save it
    private void addQueryToHistory(String query) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();

        // Avoid duplicates in the visual list
        historyModel.removeElement(query);
        historyModel.insertElementAt(query, 0);

        // Max history items = 10
        while (historyModel.size() > 10) {
            historyModel.removeElementAt(10);
        }

        // Serialize history list back to configuration
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < historyModel.size(); i++) {
            if (i > 0) sb.append("||");
            sb.append(historyModel.getElementAt(i));
        }
        config.setProperty("history", sb.toString());
        saveConfig();
    }

    // Switch Application Themes dynamically
    private void updateTheme(String themeName) {
        try {
            if ("light".equalsIgnoreCase(themeName)) {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            } else {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
            }
            // Propagate theme update down to UI trees
            SwingUtilities.updateComponentTreeUI(this);
            // Refresh results rendering to adapt to new theme colors if active
            String currentQuery = resultSearchField.getText().trim();
            if (resultsCardActive() && !currentQuery.isEmpty()) {
                triggerSearch(currentQuery);
            }
        } catch (Exception ex) {
            System.err.println("Could not toggle LookAndFeel.");
        }
    }

    private boolean resultsCardActive() {
        for (Component comp : mainContentPanel.getComponents()) {
            if (comp.isVisible() && comp == mainContentPanel.getComponents()[1]) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        // Set Default look and feel
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception ex) {
            System.err.println("FlatLaf Dark Theme not found. Using default.");
        }

        SwingUtilities.invokeLater(() -> {
            SearchEngineGUI gui = new SearchEngineGUI();
            gui.setVisible(true);
        });
    }
}
