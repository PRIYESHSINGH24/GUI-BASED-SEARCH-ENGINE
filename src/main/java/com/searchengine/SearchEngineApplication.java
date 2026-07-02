package com.searchengine;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import com.searchengine.client.SearchEngineGUI;
import javax.swing.SwingUtilities;

@SpringBootApplication
@EnableCaching
public class SearchEngineApplication {
    public static void main(String[] args) {
        // Run Spring Boot backend in non-headless mode to allow Swing/AWT thread initialization
        new SpringApplicationBuilder(SearchEngineApplication.class)
                .headless(false)
                .run(args);

        // Start the Swing Desktop UI client
        SwingUtilities.invokeLater(() -> {
            SearchEngineGUI gui = new SearchEngineGUI();
            gui.setVisible(true);
        });
    }
}
