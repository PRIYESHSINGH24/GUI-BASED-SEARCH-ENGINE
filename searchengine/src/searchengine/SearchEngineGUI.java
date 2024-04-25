package searchengine;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class SearchEngineGUI extends JFrame {
    private JTextField searchField;
    private JButton searchButton;

    // Google API key (replace with your actual API key)
    private static final String API_KEY = "AIzaSyBARD34CxIvKzxW-R_lWsSkiHivnofPHAk";

    // Google Custom Search Engine ID (replace with your actual search engine ID)
    private static final String SEARCH_ENGINE_ID = "456f26a8800d04773";

    	public SearchEngineGUI() {
    	    setTitle("Explore-It");
    	    setSize(600, 400); // Adjusted size for better visualization
    	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    	    // Create components
    	    JPanel mainPanel = new JPanel(new GridBagLayout());
    	    GridBagConstraints gbc = new GridBagConstraints();
    	    mainPanel.setBackground(Color.BLACK); // Set background color to black

    	    searchField = new JTextField();
    	    searchField.setPreferredSize(new Dimension(300, 30)); // Adjust width and height
    	    searchButton = new JButton("Search");

    	    ImageIcon logoIcon = null;
    	    // Load and scale the image
    	    try {
    	        logoIcon = new ImageIcon("C:\\Users\\priye\\Desktop\\finallogo.jpg");
    	        // Scale the image
    	        Image scaledImage = logoIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
    	        logoIcon = new ImageIcon(scaledImage);
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	        System.out.println(e.getMessage());
    	    }
    	    JLabel logoLabel = new JLabel(logoIcon);

    	    // Set font for searchField
    	    searchField.setFont(new Font("Arial", Font.PLAIN, 16));

    	    // Set layout manager
    	    setLayout(new BorderLayout());

    	    // Add components to the main panel
    	    gbc.gridx = 0;
    	    gbc.gridy = 0;
    	    gbc.insets = new Insets(10, 0, 0, 0); // Add top margin
    	    mainPanel.add(logoLabel, gbc); // Add the image label

    	    gbc.gridx = 0;
    	    gbc.gridy = 1;
    	    gbc.insets = new Insets(10, 0, 0, 0); // Add top margin
    	    mainPanel.add(searchField, gbc); // Add the search field

    	    gbc.gridy = 2;
    	    mainPanel.add(searchButton, gbc); // Add the search button

    	    // Center the main panel on the screen
    	    setLocationRelativeTo(null);

    	    // Attach action listener to search button
    	    searchButton.addActionListener(e -> {
    	        // Capture user input
    	        String query = searchField.getText();
    	        // Trigger search action
    	        try {
    	            performSearch(query);
    	        } catch (JSONException ex) {
    	            ex.printStackTrace();
    	        }
    	    });

    	    add(mainPanel, BorderLayout.CENTER);
    	}



    // Method to perform the search action
    private void performSearch(String query) throws JSONException {
        try {
            // Construct URL for Google Custom Search JSON API request
            String searchUrl = "https://www.googleapis.com/customsearch/v1?q="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&key=" + API_KEY
                    + "&cx=" + SEARCH_ENGINE_ID;

            // Log the constructed URL for debugging
            System.out.println("Request URL: " + searchUrl);

            // Create HTTP connection
            URL url = new URL(searchUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Check for HTTP response code
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Handle response based on HTTP status code
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response and process JSON data
                // (Your existing code for reading and processing JSON response)
                // Create and display a new window to show search results
                displaySearchResults(conn);
            } else {
                // Handle HTTP error
                JOptionPane.showMessageDialog(this, "HTTP error: " + responseCode, "Error", JOptionPane.ERROR_MESSAGE);
                System.out.println("Wrong output in the input query of the user");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            // Handle IO exception
            JOptionPane.showMessageDialog(this, "Error occurred while performing search.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

 // Method to display search results in a new window
 // Method to display search results in a new window
 // Method to display search results in a new window
    private void displaySearchResults(HttpURLConnection conn) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
                System.out.println(line);
            }
            reader.close();

            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());

            // Check if the response contains an "items" array
            if (jsonResponse.has("items")) {
                JSONArray items = jsonResponse.getJSONArray("items");
                System.out.println(items);

                // Create a new window to display search results
                JFrame resultFrame = new JFrame("Search Results");
                resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JTextPane resultPane = new JTextPane();
                resultPane.setContentType("text/html");
                resultPane.setEditable(false);
                resultPane.setBackground(Color.BLACK); // Set background color to black
                resultPane.setForeground(Color.WHITE); // Set text color to white
                StringBuilder htmlContent = new StringBuilder();
                htmlContent.append("<table style='width:100%; color:white;'>");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String title = "<b>" + item.getString("title") + "</b>"; // Make title bold
                    String snippet = item.getString("snippet");
                    // Append title and snippet to the result area
                    htmlContent.append("<tr><td>").append(title).append("</td></tr>");
                    //htmlContent.append("<tr><td>").append(makeLinksClickable(snippet)).append("</td></tr>");
                }
                htmlContent.append("</table>");
                resultPane.setText(htmlContent.toString());
                JScrollPane scrollPane = new JScrollPane(resultPane);
                resultFrame.add(scrollPane);
                resultFrame.pack();
                resultFrame.setVisible(true);
            } else {
              
                JOptionPane.showMessageDialog(this, "Error: No search results found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException | JSONException e) {
           
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error occurred while displaying search results.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }




    public static void main(String[] args) {
            SearchEngineGUI gui = new SearchEngineGUI();
            gui.setVisible(true);
        };
    }
