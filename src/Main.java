import db.DBConnection;
import ui.LoginFrame;
import web.WebServer;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application entry point for the Student Management System.
 * Launches both the Embedded Web Portal (http://localhost:8080)
 * and the Java Swing Desktop Application.
 */
public class Main {

    public static void main(String[] args) {
        // Enable sub-pixel text antialiasing for crisp desktop typography
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // 1. Start Embedded Web Server on port 8080
        try {
            WebServer webServer = new WebServer(8080);
            webServer.start();
        } catch (Exception e) {
            System.err.println("Note: Web Server startup notice: " + e.getMessage());
        }

        // 2. Pre-check database connection in background
        new Thread(() -> {
            try {
                if (DBConnection.testConnection()) {
                    DBConnection.initializeDatabase();
                }
            } catch (Exception e) {
                System.err.println("Database auto-check: " + e.getMessage());
            }
        }).start();

        // 3. Launch Desktop UI if graphical environment is available
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}

            SwingUtilities.invokeLater(() -> {
                try {
                    LoginFrame loginFrame = new LoginFrame();
                    loginFrame.setVisible(true);
                } catch (Exception e) {
                    System.err.println("Desktop GUI Notice: " + e.getMessage());
                }
            });
        } else {
            System.out.println("Running in headless mode. Web Portal accessible at http://localhost:8080");
        }
    }
}
