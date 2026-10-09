package com.smartsplit.ai;

import com.smartsplit.model.Expense;
import com.smartsplit.model.Group;
import com.smartsplit.model.Settlement;
import com.smartsplit.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages the conversation history and builds context-aware prompts for the LLM.
 *
 * The system prompt describes the assistant's role and passes in live group context
 * (members, expenses, balances, settlements) so the LLM can answer questions like
 * "Who owes the most?" or "Summarise this week's expenses" accurately.
 *
 * The LLM is only used for natural language; place names, ratings and distances
 * always come from PlacesService (real API data), never from the LLM.
 */
public class AIChatService {

    private static final int MAX_HISTORY = 20; // keep last 20 turns to limit token usage

    private final LLMClient         llm;
    private final List<ChatMessage> history = new ArrayList<>();

    // Live context — refreshed before each chat session
    private Group              currentGroup;
    private List<User>         members;
    private List<Expense>      expenses;
    private Map<Integer, ?>    balances;
    private List<Settlement>   settlements;

    public AIChatService(LLMClient llm) {
        this.llm = llm;
    }

    // ── Context injection ─────────────────────────────────────────────────

    public void setContext(Group group, List<User> members,
                           List<Expense> expenses,
                           Map<Integer, java.math.BigDecimal> balances,
                           List<Settlement> settlements) {
        this.currentGroup  = group;
        this.members       = members;
        this.expenses      = expenses;
        this.balances      = balances;
        this.settlements   = settlements;
        history.clear(); // fresh conversation per group session
    }

    // ── Send a message and get a reply ────────────────────────────────────

    public String send(String userMessage) throws AIException {
        history.add(ChatMessage.user(userMessage));

        // Trim history to stay within token budget
        while (history.size() > MAX_HISTORY) history.remove(0);

        List<Map<String, String>> payload = new ArrayList<>();
        payload.add(ChatMessage.system(buildSystemPrompt()).toMap());
        history.forEach(m -> payload.add(m.toMap()));

        String reply = llm.chat(payload);
        history.add(ChatMessage.assistant(reply));
        return reply;
    }

    public void clearHistory() { history.clear(); }

    // ── System prompt ─────────────────────────────────────────────────────

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the SmartSplit assistant — a helpful, concise chatbot built into a shared-expense app. ");
        sb.append("Your job is to help users understand their group expenses, balances and settlements. ");
        sb.append("Keep answers short and friendly. Use ₹ for currency amounts. ");
        sb.append("Do NOT make up place names, addresses, ratings or distances — ");
        sb.append("real location data is provided separately by the app.\n\n");

        if (currentGroup != null) {
            sb.append("=== Current Group: ").append(currentGroup.getGroupName()).append(" ===\n");
        }

        if (members != null && !members.isEmpty()) {
            sb.append("Members: ");
            for (int i = 0; i < members.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(members.get(i).getName());
                if (balances != null) {
                    Object b = balances.get(members.get(i).getUserId());
                    if (b != null) sb.append(" (balance: ₹").append(b).append(")");
                }
            }
            sb.append("\n");
        }

        if (expenses != null && !expenses.isEmpty()) {
            sb.append("Recent expenses (newest first, up to 10):\n");
            expenses.stream().limit(10).forEach(e -> {
                String payerName = members == null ? "?" : members.stream()
                        .filter(u -> u.getUserId() == e.getPaidBy())
                        .map(User::getName).findFirst().orElse("?");
                sb.append("  • ").append(e.getDescription())
                        .append(" ₹").append(e.getAmount().toPlainString())
                        .append(" (paid by ").append(payerName)
                        .append(", ").append(e.getCategory()).append(")\n");
            });
        } else {
            sb.append("No expenses recorded yet.\n");
        }

        if (settlements != null && !settlements.isEmpty()) {
            sb.append("Settlements needed:\n");
            settlements.forEach(s -> {
                String from = memberName(s.fromUserId());
                String to   = memberName(s.toUserId());
                sb.append("  • ").append(from).append(" pays ₹")
                        .append(s.amount().toPlainString())
                        .append(" to ").append(to).append("\n");
            });
        } else {
            sb.append("All settled up — no payments needed.\n");
        }

        return sb.toString();
    }

    private String memberName(int userId) {
        if (members == null) return "User " + userId;
        return members.stream()
                .filter(u -> u.getUserId() == userId)
                .map(User::getName).findFirst()
                .orElse("User " + userId);
    }
}
