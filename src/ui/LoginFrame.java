package ui;

import dao.AdminDAO;
import db.DBConnection;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Login Window for Administrator authentication.
 * Follows the Wine Red & Black design system.
 */
public class LoginFrame extends JFrame {

    private final AdminDAO adminDAO = new AdminDAO();

    private JTextField userField;
    private JPasswordField passField;
    private JLabel errorBanner;
    private JButton loginBtn;

    public LoginFrame() {
        setTitle("🎓 Student Management System - Administrator Login");
        setSize(460, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BLACK);

        // Centered Charcoal Card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.CHARCOAL);
        card.setPreferredSize(new Dimension(380, 470));
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(0x33, 0x33, 0x33), 1, true),
            new EmptyBorder(30, 32, 28, 32)
        ));

        // Logo / Icon & Title
        JLabel logoLabel = new JLabel("🎓");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("STUDENT PORTAL");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Theme.ACCENT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Administrator Sign In");
        subLabel.setFont(Theme.FONT_BODY);
        subLabel.setForeground(Theme.TEXT_MUTED);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Error message banner
        errorBanner = new JLabel(" ");
        errorBanner.setFont(Theme.FONT_SMALL);
        errorBanner.setForeground(Theme.ERROR);
        errorBanner.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Username Field
        JLabel userLbl = Theme.label("Username");
        userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        userField = new JTextField(15);
        userField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        Theme.styleField(userField);
        userField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Password Field
        JLabel passLbl = Theme.label("Password");
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        passField = new JPasswordField(15);
        passField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        Theme.styleField(passField);
        passField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Show / Hide password toggle
        JCheckBox showPassCheck = new JCheckBox("Show Password");
        showPassCheck.setBackground(Theme.CHARCOAL);
        showPassCheck.setForeground(Theme.TEXT_MUTED);
        showPassCheck.setFont(Theme.FONT_SMALL);
        showPassCheck.setFocusPainted(false);
        showPassCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        showPassCheck.addActionListener(e -> {
            if (showPassCheck.isSelected()) {
                passField.setEchoChar((char) 0);
            } else {
                passField.setEchoChar('•');
            }
        });

        // Login Button
        loginBtn = Theme.button("LOGIN TO DASHBOARD");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.addActionListener(e -> performLogin());

        // Pressing Enter in user or password field triggers login
        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        userField.addKeyListener(enterKey);
        passField.addKeyListener(enterKey);

        // Hint label & DB settings button
        JLabel hintLabel = new JLabel("Default: admin / admin123");
        hintLabel.setFont(Theme.FONT_SMALL);
        hintLabel.setForeground(Theme.TEXT_MUTED);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton dbSettingsBtn = new JButton("⚙ Database Settings");
        dbSettingsBtn.setFont(Theme.FONT_SMALL);
        dbSettingsBtn.setForeground(Theme.ACCENT);
        dbSettingsBtn.setBackground(Theme.CHARCOAL);
        dbSettingsBtn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        dbSettingsBtn.setFocusPainted(false);
        dbSettingsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        dbSettingsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        dbSettingsBtn.addActionListener(e -> {
            DatabaseConfigDialog dialog = new DatabaseConfigDialog(this);
            dialog.setVisible(true);
        });

        // Assemble Card
        card.add(logoLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(subLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(errorBanner);
        card.add(Box.createVerticalStrut(10));
        card.add(userLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(userField);
        card.add(Box.createVerticalStrut(12));
        card.add(passLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(passField);
        card.add(Box.createVerticalStrut(6));
        card.add(showPassCheck);
        card.add(Box.createVerticalStrut(18));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(hintLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(dbSettingsBtn);

        root.add(card);
        setContentPane(root);
    }

    private void performLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("AUTHENTICATING...");
        errorBanner.setText(" ");

        SwingUtilities.invokeLater(() -> {
            try {
                boolean authenticated = adminDAO.authenticate(username, password);
                if (authenticated) {
                    // Open Dashboard and close LoginFrame
                    DashboardFrame dashboard = new DashboardFrame(username);
                    dashboard.setVisible(true);
                    dispose();
                } else {
                    showError("Invalid username or password.");
                }
            } catch (SQLException ex) {
                // Testing Checklist #11: MySQL stopped -> Friendly error, no crash
                String err = ex.getMessage();
                showError("Database connection failed. Please check MySQL.");
                int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Could not connect to MySQL database.\nError: " + err + "\n\nWould you like to open Database Connection Settings?",
                    "Database Offline / Unreachable",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    DatabaseConfigDialog dialog = new DatabaseConfigDialog(this);
                    dialog.setVisible(true);
                }
            } finally {
                loginBtn.setEnabled(true);
                loginBtn.setText("LOGIN TO DASHBOARD");
            }
        });
    }

    private void showError(String message) {
        errorBanner.setText("⚠ " + message);
    }
}

