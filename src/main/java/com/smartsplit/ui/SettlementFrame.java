package com.smartsplit.ui;

import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.model.Group;
import com.smartsplit.model.Settlement;
import com.smartsplit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Shows each member's current balance and the minimal set of payments to settle the group.
 */
public class SettlementFrame extends JDialog {

    private final GroupFrame groupFrame;
    private final AppContext  ctx;
    private final Group       group;

    private final DefaultTableModel balanceModel;
    private final DefaultTableModel settlementModel;

    public SettlementFrame(GroupFrame groupFrame, AppContext ctx, Group group) {
        super(groupFrame, "Settlements — " + group.getGroupName(), true);
        this.groupFrame = groupFrame;
        this.ctx        = ctx;
        this.group      = group;

        balanceModel = new DefaultTableModel(new String[]{"Member", "Balance (₹)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        settlementModel = new DefaultTableModel(new String[]{"From", "→", "To", "Amount (₹)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        buildUI();
        setSize(520, 460);
        setLocationRelativeTo(groupFrame);
        loadData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, UITheme.PAD));
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(UITheme.PAD, UITheme.PAD, UITheme.PAD, UITheme.PAD));

        // ── Balances ──────────────────────────────────────────────────────
        JTable balanceTable = styledTable(balanceModel);
        colorizeBalance(balanceTable);

        JPanel balanceCard = UITheme.card();
        balanceCard.setLayout(new BorderLayout(0, UITheme.PAD_S));
        balanceCard.add(UITheme.heading("Balances"), BorderLayout.NORTH);
        balanceCard.add(new JScrollPane(balanceTable), BorderLayout.CENTER);
        balanceCard.add(UITheme.muted("Positive = owed money  ·  Negative = owes money"),
                BorderLayout.SOUTH);

        // ── Settlements ───────────────────────────────────────────────────
        JTable settlementTable = styledTable(settlementModel);

        JPanel settlementCard = UITheme.card();
        settlementCard.setLayout(new BorderLayout(0, UITheme.PAD_S));
        settlementCard.add(UITheme.heading("Who pays whom"), BorderLayout.NORTH);
        settlementCard.add(new JScrollPane(settlementTable), BorderLayout.CENTER);
        settlementCard.add(UITheme.muted("Minimum payments to settle everyone."),
                BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, balanceCard, settlementCard);
        split.setDividerLocation(200);
        split.setBorder(null);
        root.add(split, BorderLayout.CENTER);

        JButton closeBtn = UITheme.primaryButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(closeBtn);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(UITheme.FONT_BODY);
        t.getTableHeader().setFont(UITheme.FONT_SMALL);
        t.setRowHeight(28);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        return t;
    }

    private void colorizeBalance(JTable table) {
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                String s = String.valueOf(value);
                try {
                    BigDecimal v = new BigDecimal(s);
                    c.setForeground(v.signum() >= 0 ? UITheme.ACCENT : UITheme.DANGER);
                } catch (NumberFormatException ignored) {}
                setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        });
    }

    private void loadData() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            Map<Integer, BigDecimal> balances;
            List<Settlement>         settlements;
            List<User>               members;
            String                   err = null;

            @Override protected Void doInBackground() {
                try {
                    members     = ctx.groups.getMembers(group.getGroupId());
                    balances    = ctx.expenses.getBalances(group.getGroupId());
                    settlements = ctx.expenses.getSettlements(group.getGroupId());
                } catch (AuthenticationException | DatabaseException ex) {
                    err = ex.getMessage();
                }
                return null;
            }

            @Override protected void done() {
                if (err != null) { UITheme.showError(SettlementFrame.this, err); return; }
                populateBalances();
                populateSettlements();
            }

            private void populateBalances() {
                balanceModel.setRowCount(0);
                for (User u : members) {
                    BigDecimal b = balances.getOrDefault(u.getUserId(), BigDecimal.ZERO);
                    balanceModel.addRow(new Object[]{u.getName(), b.toPlainString()});
                }
            }

            private void populateSettlements() {
                settlementModel.setRowCount(0);
                if (settlements.isEmpty()) {
                    settlementModel.addRow(new Object[]{"✓ All settled up!", "", "", ""});
                    return;
                }
                for (Settlement s : settlements) {
                    String from = members.stream()
                            .filter(u -> u.getUserId() == s.fromUserId())
                            .map(User::getName).findFirst().orElse("?");
                    String to = members.stream()
                            .filter(u -> u.getUserId() == s.toUserId())
                            .map(User::getName).findFirst().orElse("?");
                    settlementModel.addRow(new Object[]{from, "→", to, s.amount().toPlainString()});
                }
            }
        };
        w.execute();
    }
}
