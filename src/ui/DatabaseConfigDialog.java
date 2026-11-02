package ui;

import db.DBConnection;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Dialog allowing the administrator to inspect, test, and update MySQL connection parameters,
 * as well as automatically initialize the database schema and sample data.
 */
public class DatabaseConfigDialog extends JDialog {

    private JTextField hostField;
    private JTextField portField;
    private JTextField dbNameField;
    private JTextField userField;
    private JPasswordField passField;
    private JLabel statusLabel;

    public DatabaseConfigDialog(Frame owner) {
        super(owner, "Database Connection Settings", true);
        initUI();
    }

    private void initUI() {
        setSize(480, 480);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BLACK);
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.WINE_DARK);
        headerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("⚙ Database Configuration");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.ACCENT);

        JLabel descLabel = new JLabel("Configure MySQL connection settings and initialize schema");
        descLabel.setFont(Theme.FONT_SMALL);
        descLabel.setForeground(Theme.TEXT_MUTED);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(descLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Form Panel
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.CHARCOAL);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        hostField = new JTextField(DBConnection.getHost());
        portField = new JTextField(String.valueOf(DBConnection.getPort()));
        dbNameField = new JTextField(DBConnection.getDbName());
        userField = new JTextField(DBConnection.getUser());
        passField = new JPasswordField(DBConnection.getPassword());

        Theme.styleField(hostField);
        Theme.styleField(portField);
        Theme.styleField(dbNameField);
        Theme.styleField(userField);
        Theme.styleField(passField);

        int row = 0;
        addFormRow(form, gbc, row++, "Host:", hostField);
        addFormRow(form, gbc, row++, "Port:", portField);
        addFormRow(form, gbc, row++, "Database Name:", dbNameField);
        addFormRow(form, gbc, row++, "Username:", userField);
        addFormRow(form, gbc, row++, "Password:", passField);

        // Status Message
        statusLabel = new JLabel("Status: Ready to test");
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_MUTED);
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        form.add(statusLabel, gbc);

        add(form, BorderLayout.CENTER);

        // Button Bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        btnBar.setBackground(Theme.BLACK);

        JButton testBtn = Theme.secondaryButton("Test Connection");
        JButton initBtn = Theme.primaryButton("Initialize DB & Tables");
        JButton saveBtn = Theme.successButton("Save & Close");

        testBtn.addActionListener(e -> onTest());
        initBtn.addActionListener(e -> onInit());
        saveBtn.addActionListener(e -> onSave());

        btnBar.add(testBtn);
        btnBar.add(initBtn);
        btnBar.add(saveBtn);
        add(btnBar, BorderLayout.SOUTH);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        gbc.gridwidth = 1;
        JLabel lbl = Theme.label(labelText);
        lbl.setFont(Theme.FONT_BOLD);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(field, gbc);
    }

    private void syncInputs() {
        int port = 3306;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ignored) {}

        DBConnection.saveConfig(
            hostField.getText().trim(),
            port,
            dbNameField.getText().trim(),
            userField.getText().trim(),
            new String(passField.getPassword()).trim()
        );
    }

    private void onTest() {
        syncInputs();
        statusLabel.setText("Status: Testing connection...");
        statusLabel.setForeground(Theme.ACCENT);

        SwingUtilities.invokeLater(() -> {
            boolean ok = DBConnection.testConnection();
            if (ok) {
                statusLabel.setText("Status: Connection Successful! ✓");
                statusLabel.setForeground(Theme.SUCCESS);
                Theme.showInfo(this, "Connected successfully to MySQL server!", "Connection Test");
            } else {
                statusLabel.setText("Status: Connection Failed! ✗");
                statusLabel.setForeground(Theme.ERROR);
                String err = DBConnection.getLastError();
                Theme.showError(this, "Failed to connect to MySQL:\n" + (err != null ? err : "Unknown error"), "Connection Error");
            }
        });
    }

    private void onInit() {
        syncInputs();
        statusLabel.setText("Status: Initializing database and tables...");
        statusLabel.setForeground(Theme.ACCENT);

        SwingUtilities.invokeLater(() -> {
            boolean ok = DBConnection.initializeDatabase();
            if (ok) {
                statusLabel.setText("Status: Database & tables initialized successfully! ✓");
                statusLabel.setForeground(Theme.SUCCESS);
                Theme.showInfo(this, "Database 'student_db' and tables created successfully with sample data!", "Database Initialized");
            } else {
                statusLabel.setText("Status: Initialization Failed! ✗");
                statusLabel.setForeground(Theme.ERROR);
                String err = DBConnection.getLastError();
                Theme.showError(this, "Failed to initialize database:\n" + (err != null ? err : "Check server connection"), "Initialization Error");
            }
        });
    }

    private void onSave() {
        syncInputs();
        dispose();
    }
}

