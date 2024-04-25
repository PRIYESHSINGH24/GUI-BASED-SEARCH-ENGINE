import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

public class SearchEngineGUI extends JFrame {
    private JTextField searchField;
    private JButton searchButton;
    private JTextArea resultArea;

    // Google API key (replace with your actual API key)
    private static final String API_KEY = "AIzaSyBARD34CxIvKzxW-R_lWsSkiHivnofPHAk";

    // Google Custom Search Engine ID (replace with your actual search engine ID)
    private static final String SEARCH_ENGINE_ID = "456f26a8800d04773";

    public SearchEngineGUI() {
        setTitle("Search Engine");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create components
        searchField = new JTextField();
        searchButton = new JButton("Search");
        resultArea = new JTextArea();

        // Set layout manager
        setLayout(new BorderLayout());

        // Add components to the frame
        add(searchField, BorderLayout.NORTH);
        add(searchButton, BorderLayout.CENTER);
        add(new JScrollPane(resultArea), BorderLayout.SOUTH);

        // Attach action listener to search button
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Capture user input
                String query = searchField.getText();
                // Trigger search action
                performSearch(query);
            }
        });
    }

    // Method to perform the search action
    private void performSearch(String query) {
        try {
            // Construct URL for Google Custom Search JSON API request
            String searchUrl = "https://www.googleapis.com/customsearch/v1?q="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&key=" + API_KEY
                    + "&cx=" + SEARCH_ENGINE_ID;

            // Create HTTP connection
            URL url = new URL(searchUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray items = jsonResponse.getJSONArray("items");

            // Display search results
            resultArea.setText("");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String title = item.getString("title");
                String snippet = item.getString("snippet");
                resultArea.append(title + "\n" + snippet + "\n\n");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            resultArea.setText("Error occurred while performing search.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SearchEngineGUI gui = new SearchEngineGUI();
                gui.setVisible(true);
            }
        });
    }
}

