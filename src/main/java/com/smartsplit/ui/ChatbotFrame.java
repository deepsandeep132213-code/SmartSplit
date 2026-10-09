package com.smartsplit.ui;

import com.smartsplit.ai.AIException;
import com.smartsplit.ai.AIChatService;
import com.smartsplit.ai.LLMClient;
import com.smartsplit.ai.PlaceResult;
import com.smartsplit.ai.PlacesService;
import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.model.Expense;
import com.smartsplit.model.Group;
import com.smartsplit.model.Settlement;
import com.smartsplit.model.User;
import com.smartsplit.util.EnvConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI chatbot panel — shown as a side dialog from GroupFrame.
 *
 * Features:
 *   • Natural-language chat about the group's expenses / balances / settlements
 *   • "Find nearby" tab: fetches real place data from Google Places API
 *     (no LLM-invented names/ratings/distances — spec requirement)
 *
 * All network calls run on a single-thread ExecutorService so the UI never blocks.
 */
public class ChatbotFrame extends JDialog {

    private final GroupFrame    groupFrame;
    private final AppContext    ctx;
    private final Group         group;

    private AIChatService  chatService;
    private PlacesService  placesService;

    // ExecutorService for AI and Places network calls
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "smartsplit-ai");
        t.setDaemon(true);
        return t;
    });

    // ── Chat tab ──────────────────────────────────────────────────────────
    private final JTextPane    chatPane  = new JTextPane();
    private final JTextField   inputField = UITheme.textField();
    private final JButton      sendBtn    = UITheme.primaryButton("Send");
    private final JButton      clearBtn   = UITheme.linkButton("Clear");

    // ── Places tab ────────────────────────────────────────────────────────
    private final JTextField   latField  = UITheme.textField();
    private final JTextField   lngField  = UITheme.textField();
    private final JTextField   typeField = UITheme.textField();
    private final JTextField   kwField   = UITheme.textField();
    private final JButton      searchBtn = UITheme.primaryButton("Search Nearby");
    private final JTextArea    placesArea = new JTextArea();

    // Chat styles
    private Style userStyle, botStyle, infoStyle;

    public ChatbotFrame(GroupFrame groupFrame, AppContext ctx, Group group) {
        super(groupFrame, "AI Assistant — " + group.getGroupName(), false); // non-modal
        this.groupFrame = groupFrame;
        this.ctx        = ctx;
        this.group      = group;
        buildUI();
        setSize(480, 580);
        setLocationRelativeTo(groupFrame);
        initServices();
    }

    // ── UI ────────────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("💬 Chat",      buildChatTab());
        tabs.addTab("📍 Find Nearby", buildPlacesTab());

        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);

        // Stop executor on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                executor.shutdownNow();
            }
        });
    }

    private JPanel buildChatTab() {
        JPanel p = new JPanel(new BorderLayout(0, UITheme.PAD_S));
        p.setBackground(UITheme.BG);
        p.setBorder(new EmptyBorder(UITheme.PAD_S, UITheme.PAD_S, UITheme.PAD_S, UITheme.PAD_S));

        // Chat transcript
        chatPane.setEditable(false);
        chatPane.setFont(UITheme.FONT_BODY);
        chatPane.setBackground(UITheme.SURFACE);

        // Define styles
        StyledDocument doc = chatPane.getStyledDocument();
        userStyle = doc.addStyle("user", null);
        StyleConstants.setForeground(userStyle, UITheme.PRIMARY_DARK);
        StyleConstants.setBold(userStyle, true);

        botStyle = doc.addStyle("bot", null);
        StyleConstants.setForeground(botStyle, UITheme.TEXT_PRIMARY);

        infoStyle = doc.addStyle("info", null);
        StyleConstants.setForeground(infoStyle, UITheme.TEXT_MUTED);
        StyleConstants.setItalic(infoStyle, true);

        JScrollPane scroll = new JScrollPane(chatPane);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        // Input row
        JPanel inputRow = new JPanel(new BorderLayout(UITheme.PAD_S, 0));
        inputRow.setOpaque(false);
        inputField.setPreferredSize(new Dimension(300, 32));
        inputRow.add(inputField, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btnRow.setOpaque(false);
        btnRow.add(sendBtn);
        btnRow.add(clearBtn);
        inputRow.add(btnRow, BorderLayout.EAST);

        p.add(scroll, BorderLayout.CENTER);
        p.add(inputRow, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> onSend());
        inputField.addActionListener(e -> onSend());
        clearBtn.addActionListener(e -> onClear());

        return p;
    }

    private JPanel buildPlacesTab() {
        JPanel p = new JPanel(new BorderLayout(0, UITheme.PAD_S));
        p.setBackground(UITheme.BG);
        p.setBorder(new EmptyBorder(UITheme.PAD_S, UITheme.PAD, UITheme.PAD_S, UITheme.PAD));

        JPanel form = UITheme.card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill   = GridBagConstraints.HORIZONTAL;

        typeField.setText("restaurant");
        latField.setPreferredSize(new Dimension(120, 32));
        lngField.setPreferredSize(new Dimension(120, 32));

        addRow(form, c, 0, "Place type",    typeField);
        addRow(form, c, 1, "Keyword",       kwField);
        addRow(form, c, 2, "Latitude",      latField);
        addRow(form, c, 3, "Longitude",     lngField);

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2; c.weightx = 1;
        form.add(UITheme.muted("Tip: find your latitude/longitude from Google Maps."), c);

        c.gridy = 5;
        form.add(searchBtn, c);

        placesArea.setEditable(false);
        placesArea.setFont(UITheme.FONT_BODY);
        placesArea.setWrapStyleWord(true);
        placesArea.setLineWrap(true);
        placesArea.setText("Results from Google Places will appear here.\n(Real data only — nothing is invented.)");

        JScrollPane scroll = new JScrollPane(placesArea);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        p.add(form,   BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);

        searchBtn.addActionListener(e -> onSearchPlaces());
        return p;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        p.add(new JLabel(label + ":"), c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
    }

    // ── Service init ──────────────────────────────────────────────────────

    private void initServices() {
        appendInfo("Loading AI assistant…");
        executor.submit(() -> {
            try {
                EnvConfig config = EnvConfig.load();
                LLMClient llm = new LLMClient(config);
                chatService = new AIChatService(llm);
                loadGroupContext();

                // PlacesService is optional — swallow if key not set
                try { placesService = new PlacesService(config); }
                catch (AIException e) {
                    SwingUtilities.invokeLater(() ->
                        appendInfo("Places feature disabled: " + e.getMessage()));
                }

                SwingUtilities.invokeLater(() -> {
                    clearChat();
                    appendInfo("Ready! Ask me anything about \"" + group.getGroupName() + "\".");
                    inputField.setEnabled(true);
                    sendBtn.setEnabled(true);
                });
            } catch (AIException e) {
                SwingUtilities.invokeLater(() -> {
                    appendInfo("AI unavailable: " + e.getMessage());
                    inputField.setEnabled(false);
                    sendBtn.setEnabled(false);
                });
            }
        });
        inputField.setEnabled(false);
        sendBtn.setEnabled(false);
    }

    private void loadGroupContext() {
        try {
            List<User>    members     = ctx.groups.getMembers(group.getGroupId());
            List<Expense> expenses    = ctx.expenses.getGroupExpenses(group.getGroupId());
            Map<Integer, BigDecimal> balances = ctx.expenses.getBalances(group.getGroupId());
            List<Settlement> settlements      = ctx.expenses.getSettlements(group.getGroupId());
            chatService.setContext(group, members, expenses, balances, settlements);
        } catch (AuthenticationException | DatabaseException e) {
            SwingUtilities.invokeLater(() ->
                appendInfo("Could not load group data: " + e.getMessage()));
        }
    }

    // ── Chat actions ──────────────────────────────────────────────────────

    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isBlank() || chatService == null) return;
        inputField.setText("");
        appendUser(text);
        sendBtn.setEnabled(false);

        executor.submit(() -> {
            try {
                String reply = chatService.send(text);
                SwingUtilities.invokeLater(() -> { appendBot(reply); sendBtn.setEnabled(true); });
            } catch (AIException e) {
                SwingUtilities.invokeLater(() -> {
                    appendInfo("Error: " + e.getMessage());
                    sendBtn.setEnabled(true);
                });
            }
        });
    }

    private void onClear() {
        if (chatService != null) chatService.clearHistory();
        clearChat();
        appendInfo("Conversation cleared.");
    }

    // ── Places actions ────────────────────────────────────────────────────

    private void onSearchPlaces() {
        if (placesService == null) {
            UITheme.showError(this, "Google Places API key is not configured.\nAdd GOOGLE_PLACES_API_KEY to your .env file.");
            return;
        }
        String typeStr = typeField.getText().trim();
        String kwStr   = kwField.getText().trim();
        String latStr  = latField.getText().trim();
        String lngStr  = lngField.getText().trim();

        if (latStr.isBlank() || lngStr.isBlank()) {
            UITheme.showError(this, "Please enter your latitude and longitude.");
            return;
        }
        double lat, lng;
        try {
            lat = Double.parseDouble(latStr);
            lng = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            UITheme.showError(this, "Latitude and longitude must be numbers.");
            return;
        }

        searchBtn.setEnabled(false);
        placesArea.setText("Searching…");

        executor.submit(() -> {
            try {
                List<PlaceResult> results = placesService.search(lat, lng, typeStr, kwStr, 10);
                SwingUtilities.invokeLater(() -> {
                    searchBtn.setEnabled(true);
                    displayPlaces(results, typeStr.isBlank() ? "places" : typeStr);
                });
            } catch (AIException e) {
                SwingUtilities.invokeLater(() -> {
                    searchBtn.setEnabled(true);
                    placesArea.setText("Error: " + e.getMessage());
                });
            }
        });
    }

    private void displayPlaces(List<PlaceResult> results, String type) {
        if (results.isEmpty()) {
            placesArea.setText("No " + type + "s found within 2 km.");
            return;
        }
        StringBuilder sb = new StringBuilder("Nearby " + type + "s (source: Google Places):\n\n");
        for (int i = 0; i < results.size(); i++) {
            PlaceResult r = results.get(i);
            sb.append(i + 1).append(". ").append(r.name()).append("\n");
            sb.append("   ").append(r.address()).append("\n");
            sb.append("   Rating: ").append(r.rating());
            if (!"N/A".equals(r.rating())) sb.append(" (").append(r.userRatingsTotal()).append(" reviews)");
            sb.append("   ").append(r.openNow()).append("\n\n");
        }
        placesArea.setText(sb.toString());
        placesArea.setCaretPosition(0);
    }

    // ── Chat rendering ────────────────────────────────────────────────────

    private void appendUser(String text) {
        append("You: " + text + "\n", userStyle);
    }

    private void appendBot(String text) {
        append("Assistant: " + text + "\n\n", botStyle);
    }

    private void appendInfo(String text) {
        append(text + "\n", infoStyle);
    }

    private void append(String text, Style style) {
        StyledDocument doc = chatPane.getStyledDocument();
        try { doc.insertString(doc.getLength(), text, style); }
        catch (BadLocationException ignored) {}
        chatPane.setCaretPosition(doc.getLength());
    }

    private void clearChat() {
        try { chatPane.getStyledDocument().remove(0, chatPane.getDocument().getLength()); }
        catch (BadLocationException ignored) {}
    }
}
