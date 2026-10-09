package com.smartsplit.ui;

import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/** Registration dialog — opened from LoginFrame. */
public class RegisterFrame extends JDialog {

    private final AppContext ctx;
    private final LoginFrame parent;

    private final JTextField     nameField     = UITheme.textField();
    private final JTextField     emailField    = UITheme.textField();
    private final JPasswordField passwordField = UITheme.passwordField();
    private final JPasswordField confirmField  = UITheme.passwordField();
    private final JButton        registerBtn   = UITheme.primaryButton("Create Account");
    private final JLabel         errorLabel    = UITheme.muted(" ");

    public RegisterFrame(LoginFrame parent, AppContext ctx) {
        super(parent, "SmartSplit — Create Account", true);
        this.parent = parent;
        this.ctx    = ctx;
        buildUI();
        wire();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(30, 50, 30, 50));

        JLabel title = UITheme.heading("Create your account");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(title, BorderLayout.NORTH);

        JPanel form = UITheme.card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        addRow(form, c, 0, "Name",             nameField);
        addRow(form, c, 1, "Email",             emailField);
        addRow(form, c, 2, "Password",          passwordField);
        addRow(form, c, 3, "Confirm password",  confirmField);

        errorLabel.setForeground(UITheme.DANGER);
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        form.add(errorLabel, c);

        c.gridy = 5;
        registerBtn.setPreferredSize(new Dimension(240, 36));
        form.add(registerBtn, c);

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(16, 0, 0, 0));
        wrap.add(form);
        root.add(wrap, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        p.add(new JLabel(label + ":"), c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
    }

    private void wire() {
        registerBtn.addActionListener(this::onRegister);
        confirmField.addActionListener(this::onRegister);
    }

    private void onRegister(ActionEvent e) {
        errorLabel.setText(" ");
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm  = new String(confirmField.getPassword());

        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match.");
            confirmField.setText("");
            return;
        }

        registerBtn.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String errorMsg = null;
            @Override protected Void doInBackground() {
                try { ctx.auth.register(name, email, password); }
                catch (AuthenticationException | DatabaseException ex) { errorMsg = ex.getMessage(); }
                return null;
            }
            @Override protected void done() {
                registerBtn.setEnabled(true);
                if (errorMsg != null) {
                    errorLabel.setText(errorMsg);
                    passwordField.setText("");
                    confirmField.setText("");
                } else {
                    dispose();
                    parent.onRegistered();
                }
            }
        };
        worker.execute();
    }
}
