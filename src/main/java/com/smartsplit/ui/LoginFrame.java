package com.smartsplit.ui;

import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Login screen — first window the user sees.
 */
public class LoginFrame extends JFrame {

    private final AppContext ctx;

    private final JTextField     emailField    = UITheme.textField();
    private final JPasswordField passwordField = UITheme.passwordField();
    private final JButton        loginBtn      = UITheme.primaryButton("Log In");
    private final JButton        registerLink  = UITheme.linkButton("Create an account");
    private final JLabel         errorLabel    = UITheme.muted(" ");

    public LoginFrame(AppContext ctx) {
        super("SmartSplit — Log In");
        this.ctx = ctx;
        buildUI();
        wire();
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(40, 60, 40, 60));

        // ── Header ────────────────────────────────────────────────────────
        JLabel title = new JLabel("SmartSplit");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.PRIMARY);
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel sub = UITheme.muted("Split expenses. Settle smartly. Explore nearby.");
        sub.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);
        header.add(title);
        header.add(sub);
        root.add(header, BorderLayout.NORTH);

        // ── Form ──────────────────────────────────────────────────────────
        JPanel form = UITheme.card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        addRow(form, c, 0, "Email",    emailField);
        addRow(form, c, 1, "Password", passwordField);

        errorLabel.setForeground(UITheme.DANGER);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        form.add(errorLabel, c);

        c.gridy = 3;
        loginBtn.setPreferredSize(new Dimension(240, 36));
        form.add(loginBtn, c);

        c.gridy = 4;
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkRow.setOpaque(false);
        linkRow.add(UITheme.muted("New here?"));
        linkRow.add(registerLink);
        form.add(linkRow, c);

        JPanel formWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        formWrap.setOpaque(false);
        formWrap.setBorder(new EmptyBorder(20, 0, 0, 0));
        formWrap.add(form);
        root.add(formWrap, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        p.add(new JLabel(label + ":"), c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
    }

    private void wire() {
        loginBtn.addActionListener(this::onLogin);
        passwordField.addActionListener(this::onLogin);
        emailField.addActionListener(e -> passwordField.requestFocus());
        registerLink.addActionListener(e -> new RegisterFrame(this, ctx).setVisible(true));
    }

    private void onLogin(ActionEvent e) {
        errorLabel.setText(" ");
        String email    = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        loginBtn.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String errorMsg = null;
            @Override protected Void doInBackground() {
                try { ctx.auth.login(email, password); }
                catch (AuthenticationException | DatabaseException ex) { errorMsg = ex.getMessage(); }
                return null;
            }
            @Override protected void done() {
                loginBtn.setEnabled(true);
                if (errorMsg != null) {
                    errorLabel.setText(errorMsg);
                    passwordField.setText("");
                } else {
                    dispose();
                    new DashboardFrame(ctx).setVisible(true);
                }
            }
        };
        worker.execute();
    }

    // Called from RegisterFrame after successful registration
    void onRegistered() {
        dispose();
        new DashboardFrame(ctx).setVisible(true);
    }

    // ── Entry point helper ────────────────────────────────────────────────
    public static void show(AppContext ctx) {
        SwingUtilities.invokeLater(() -> new LoginFrame(ctx).setVisible(true));
    }
}
