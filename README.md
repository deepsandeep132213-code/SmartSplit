# SmartSplit

**Split expenses. Settle smartly. Explore nearby.**

SmartSplit is a Java 17 desktop application for managing group expenses. Create groups, add expenses, split them equally or by custom amounts, see exactly who owes whom, and ask an AI assistant about restaurants or cafes nearby.

## Features

- **User accounts** — register and log in; PBKDF2-HMAC-SHA256 password hashing (100 k iterations, random salt per user)
- **Groups** — create groups, invite members by email, remove members, archive or delete groups
- **Expenses** — add expenses with a description, category, date, and optional notes; pay on behalf of the group
- **Split modes** — equal split across all members, equal split across a subset, or custom amounts per person
- **Settlements** — greedy algorithm minimises the number of payments (at most *n − 1* for *n* members); colour-coded balance table
- **AI chatbot** — conversational assistant with live group context (balances, members, recent expenses); backed by the OpenAI-compatible API
- **Find Nearby** — real Google Places results for restaurants, cafes, and activities near a location; the LLM is explicitly prohibited from inventing place data

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| GUI | Swing (SwingWorker for all DB/network calls) |
| Database | MySQL 8+ via JDBC |
| Build | Maven |
| HTTP | JDK `HttpClient` (no external HTTP library) |
| JSON | Hand-rolled builder/parser (no external JSON library) |
| LLM | OpenAI-compatible chat-completion API |
| Places | Google Places Nearby Search API |
| Tests | Hand-rolled `@Test` runner (no JUnit dependency) |

No third-party runtime dependencies — every library used ships with the JDK.

## Architecture

```
UI (Swing frames)
      │
AppContext  ──  AuthenticationService, GroupService, ExpenseService, SettlementService
      │
DAO layer  ──  UserDAO, GroupDAO, ExpenseDAO, ExpenseSplitDAO
      │
JDBC / DBConnection
      │
MySQL 8
      │          AI / Places (daemon ExecutorService)
      └──────── LLMClient, PlacesService, AIChatService
```

## Project Structure

```
SmartSplit/
  src/main/java/com/smartsplit/
    Main.java
    ai/          LLMClient, AIChatService, PlacesService, ChatMessage, PlaceResult
    dao/         UserDAO, GroupDAO, ExpenseDAO, ExpenseSplitDAO
    database/    DBConnection
    exception/   AuthenticationException, DatabaseException, InvalidExpenseException,
                 InvalidSplitException, MemberNotFoundException
    model/       User, Group, Expense, ExpenseSplit, Settlement, ExpenseCategory, SplitType
    service/     AuthenticationService, GroupService, ExpenseService, SettlementService
    strategy/    SplitStrategy, EqualSplitStrategy, CustomSplitStrategy
    ui/          UITheme, AppContext, LoginFrame, RegisterFrame, DashboardFrame,
                 GroupFrame, ExpenseFrame, SettlementFrame, ChatbotFrame
    util/        EnvConfig, Money
  src/test/java/com/smartsplit/
    testrunner/  @Test, Assertions, Runner
    ai/          LLMClientJsonTest
    service/     AuthenticationServiceTest, SettlementServiceTest
    strategy/    EqualSplitStrategyTest, CustomSplitStrategyTest
    util/        EnvConfigTest, MoneyTest
  database/
    smartsplit.sql
  .env.example
  pom.xml
```

## Requirements

- JDK 17 or newer
- Maven 3.8+
- MySQL 8.0.16 or newer
- OpenAI-compatible API key (for the AI chatbot)
- Google Places API key (for Find Nearby — optional; the tab is hidden if the key is absent)

## Database Setup

1. Ensure MySQL 8.0.16+ is running.
2. Create all tables by running the schema script:

   ```bash
   mysql -u <your_user> -p < database/smartsplit.sql
   ```

   The script creates the `smartsplit_db` database and all five tables. Re-running it **wipes existing data**.

3. Add your credentials to `.env` (see Configuration).

Tables: `users`, `groups`, `group_members`, `expenses`, `expense_splits`.

> **Note:** `groups` is a reserved word in MySQL 8 — the SQL always quotes it as `` `groups` ``.

## Configuration

Copy `.env.example` to `.env` and fill in your values. The app checks real environment variables first, then `.env` — either works.

| Variable | Required | Purpose |
|----------|----------|---------|
| `DB_URL` | Yes | JDBC URL (default `jdbc:mysql://localhost:3306/smartsplit_db`) |
| `DB_USERNAME` | Yes | MySQL user |
| `DB_PASSWORD` | No | MySQL password |
| `OPENAI_API_KEY` | Yes* | LLM API key — chat tab disabled without it |
| `OPENAI_MODEL` | No | Model name (default `gpt-4o-mini`) |
| `OPENAI_API_URL` | No | Override for Azure / Ollama / other providers |
| `GOOGLE_PLACES_API_KEY` | No | Places API key — Find Nearby tab hidden without it |

\* The app starts without it; only the AI chatbot is unavailable.

**MySQL troubleshooting:** if MySQL 8 reports `Public Key Retrieval is not allowed`, add `?allowPublicKeyRetrieval=true&useSSL=false` to `DB_URL`. Use that only for a local development database.

## Build and Run

```bash
# Compile and run
mvn compile
mvn exec:java

# Or package and run as a fat-jar
mvn package
java -jar target/smartsplit-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Tests

The project ships a hand-rolled test runner (no JUnit dependency — Maven Central is not required).

```bash
# Compile everything
find src -name "*.java" | sort > /tmp/sources.txt
javac -d /tmp/classes --release 17 @/tmp/sources.txt

# Run all 66 tests
java -cp /tmp/classes com.smartsplit.testrunner.Runner \
  com.smartsplit.util.MoneyTest \
  com.smartsplit.util.EnvConfigTest \
  com.smartsplit.strategy.EqualSplitStrategyTest \
  com.smartsplit.strategy.CustomSplitStrategyTest \
  com.smartsplit.service.SettlementServiceTest \
  com.smartsplit.service.AuthenticationServiceTest \
  com.smartsplit.ai.LLMClientJsonTest
```

Expected output: `Results: 66 passed, 0 failed`

Tests cover: `Money` rounding, `EnvConfig` parsing, `EqualSplitStrategy` (including paise-level remainder), `CustomSplitStrategy` validation, `SettlementService` balance and settlement algorithm, `AuthenticationService` (stub DAO, password hashing, register/login/logout), and `LLMClient` JSON parsing.

## Security

- Passwords hashed with PBKDF2-HMAC-SHA256 (100 000 iterations, 32-byte random salt, 256-bit key)
- Constant-time password comparison (no timing attacks)
- All DB queries use `PreparedStatement` — no string concatenation into SQL
- API keys and credentials are read from environment variables only; never from source code
- `.env` is in `.gitignore` and must never be committed
- The AI is instructed not to invent place names, ratings, or distances

## Future Improvements

Receipt photos, multi-currency support, export to CSV/PDF, recurring expenses, push notifications for new expenses.
