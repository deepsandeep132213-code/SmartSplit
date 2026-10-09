package com.smartsplit.ui;

import com.smartsplit.ai.AIException;
import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.exception.MemberNotFoundException;
import com.smartsplit.model.Expense;
import com.smartsplit.model.Group;
import com.smartsplit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Shows a single group: member list, expense list, and action buttons.
 * Opens ExpenseFrame and SettlementFrame as child dialogs.
 */
public class GroupFrame extends JDialog {

    private final DashboardFrame  dashboard;
    private final AppContext       ctx;
    private final Group            group;

    private final DefaultListModel<String>      memberModel  = new DefaultListModel<>();
    private final JList<String>                 memberList   = new JList<>(memberModel);
    private final DefaultTableModel             expenseModel;
    private final JTable                        expenseTable;

    private final JButton addExpenseBtn   = UITheme.primaryButton("+ Add Expense");
    private final JButton settleBtn       = UITheme.primaryButton("View Settlements");
    private final JButton chatBtn         = UITheme.primaryButton("💬 AI Assistant");
    private final JButton addMemberBtn    = UITheme.linkButton("+ Add member");
    private final JButton removeMemberBtn = UITheme.linkButton("Remove member");
    private final JButton deleteGroupBtn  = UITheme.dangerButton("Delete Group");

    public GroupFrame(DashboardFrame dashboard, AppContext ctx, Group group) {
        super(dashboard, group.getGroupName(), true);
        this.dashboard = dashboard;
        this.ctx       = ctx;
        this.group     = group;

        String[] cols = {"Description", "Amount (₹)", "Paid by", "Category", "Date"};
        expenseModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        expenseTable = new JTable(expenseModel);
        expenseTable.setFont(UITheme.FONT_BODY);
        expenseTable.getTableHeader().setFont(UITheme.FONT_SMALL);
        expenseTable.setRowHeight(28);
        expenseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        buildUI();
        wire();
        setSize(700, 540);
        setLocationRelativeTo(dashboard);
        refresh();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG);

        // ── Header ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PRIMARY);
        header.setBorder(new EmptyBorder(12, UITheme.PAD, 12, UITheme.PAD));

        JLabel title = new JLabel(group.getGroupName());
        title.setFont(UITheme.FONT_HEADING);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // ── Split view: members left, expenses right ───────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildMemberPanel(), buildExpensePanel());
        split.setDividerLocation(180);
        split.setBorder(null);
        root.add(split, BorderLayout.CENTER);

        // ── Bottom bar ────────────────────────────────────────────────────
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.PAD_S, UITheme.PAD_S));
        bottom.setBackground(UITheme.BG);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));
        bottom.add(deleteGroupBtn);
        bottom.add(chatBtn);
        bottom.add(settleBtn);
        bottom.add(addExpenseBtn);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildMemberPanel() {
        JPanel p = new JPanel(new BorderLayout(0, UITheme.PAD_S));
        p.setBackground(UITheme.SURFACE);
        p.setBorder(new EmptyBorder(UITheme.PAD, UITheme.PAD_S, UITheme.PAD, UITheme.PAD_S));

        JLabel lbl = UITheme.heading("Members");
        memberList.setFont(UITheme.FONT_BODY);
        memberList.setBackground(UITheme.SURFACE);

        JPanel btnRow = new JPanel(new GridLayout(2, 1, 0, 4));
        btnRow.setOpaque(false);
        btnRow.add(addMemberBtn);
        btnRow.add(removeMemberBtn);

        p.add(lbl, BorderLayout.NORTH);
        p.add(new JScrollPane(memberList), BorderLayout.CENTER);
        p.add(btnRow, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildExpensePanel() {
        JPanel p = new JPanel(new BorderLayout(0, UITheme.PAD_S));
        p.setBackground(UITheme.SURFACE);
        p.setBorder(new EmptyBorder(UITheme.PAD, UITheme.PAD_S, UITheme.PAD, UITheme.PAD_S));

        JLabel lbl = UITheme.heading("Expenses");
        p.add(lbl, BorderLayout.NORTH);
        p.add(new JScrollPane(expenseTable), BorderLayout.CENTER);
        return p;
    }

    private void wire() {
        chatBtn.addActionListener(e ->
                new ChatbotFrame(this, ctx, group).setVisible(true));
        addExpenseBtn.addActionListener(e -> {
            new ExpenseFrame(this, ctx, group).setVisible(true);
            refresh();
        });
        settleBtn.addActionListener(e ->
                new SettlementFrame(this, ctx, group).setVisible(true));
        addMemberBtn.addActionListener(e -> onAddMember());
        removeMemberBtn.addActionListener(e -> onRemoveMember());
        deleteGroupBtn.addActionListener(e -> onDeleteGroup());

        // Right-click context menu on expense table
        JPopupMenu menu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete expense");
        deleteItem.addActionListener(e -> onDeleteExpense());
        menu.add(deleteItem);
        expenseTable.setComponentPopupMenu(menu);
        expenseTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                int row = expenseTable.rowAtPoint(e.getPoint());
                if (row >= 0) expenseTable.setRowSelectionInterval(row, row);
            }
        });
    }

    // ── Data refresh ──────────────────────────────────────────────────────

    void refresh() {
        loadMembers();
        loadExpenses();
    }

    private void loadMembers() {
        SwingWorker<List<User>, Void> w = new SwingWorker<>() {
            @Override protected List<User> doInBackground() throws Exception {
                return ctx.groups.getMembers(group.getGroupId());
            }
            @Override protected void done() {
                try {
                    memberModel.clear();
                    get().forEach(u -> memberModel.addElement(u.getName()));
                } catch (Exception ex) { UITheme.showError(GroupFrame.this, ex.getMessage()); }
            }
        };
        w.execute();
    }

    private List<User> cachedMembers = List.of();

    private void loadExpenses() {
        SwingWorker<List<Expense>, Void> w = new SwingWorker<>() {
            List<User> members;
            @Override protected List<Expense> doInBackground() throws Exception {
                members = ctx.groups.getMembers(group.getGroupId());
                return ctx.expenses.getGroupExpenses(group.getGroupId());
            }
            @Override protected void done() {
                try {
                    cachedMembers = members;
                    List<Expense> expenses = get();
                    expenseModel.setRowCount(0);
                    for (Expense e : expenses) {
                        String payer = members.stream()
                                .filter(u -> u.getUserId() == e.getPaidBy())
                                .map(User::getName).findFirst().orElse("?");
                        expenseModel.addRow(new Object[]{
                                e.getDescription(),
                                e.getAmount().toPlainString(),
                                payer,
                                e.getCategory().toString(),
                                e.getExpenseDate() != null ? e.getExpenseDate().toString() : ""
                        });
                    }
                } catch (Exception ex) { UITheme.showError(GroupFrame.this, ex.getMessage()); }
            }
        };
        w.execute();
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void onAddMember() {
        String email = JOptionPane.showInputDialog(this,
                "Enter their email address:", "Add Member", JOptionPane.PLAIN_MESSAGE);
        if (email == null || email.isBlank()) return;
        try {
            ctx.groups.addMember(group.getGroupId(), email);
            refresh();
        } catch (AuthenticationException | DatabaseException |
                 MemberNotFoundException | IllegalStateException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void onRemoveMember() {
        String selected = memberList.getSelectedValue();
        if (selected == null) { UITheme.showError(this, "Select a member first."); return; }
        if (!UITheme.confirm(this, "Remove " + selected + " from the group?")) return;
        try {
            List<User> members = ctx.groups.getMembers(group.getGroupId());
            User toRemove = members.stream()
                    .filter(u -> u.getName().equals(selected)).findFirst().orElse(null);
            if (toRemove == null) return;
            ctx.groups.removeMember(group.getGroupId(), toRemove.getUserId());
            refresh();
        } catch (AuthenticationException | DatabaseException |
                 MemberNotFoundException | IllegalStateException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void onDeleteExpense() {
        int row = expenseTable.getSelectedRow();
        if (row < 0) return;
        String desc = (String) expenseModel.getValueAt(row, 0);
        if (!UITheme.confirm(this, "Delete expense \"" + desc + "\"?")) return;
        // We need the expense id — store them in a parallel list
        UITheme.showInfo(this, "Select the expense and use the right-click menu. (Feature: store id in table model)");
    }

    private void onDeleteGroup() {
        if (!UITheme.confirm(this, "Permanently delete \"" + group.getGroupName() + "\" and all its data?")) return;
        try {
            ctx.groups.deleteGroup(group.getGroupId());
            dispose();
            dashboard.loadGroups();
        } catch (AuthenticationException | DatabaseException |
                 MemberNotFoundException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    /** Exposes cached member list for child frames (e.g. ExpenseFrame). */
    List<User> getCachedMembers() { return cachedMembers; }
}
