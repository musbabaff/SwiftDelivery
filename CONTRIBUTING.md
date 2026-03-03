# Contributing to VeloDelivery

Thank you for your interest in contributing to **VeloDelivery**! We appreciate every contribution, whether it's a bug fix, a new feature, a translation, or simply improving documentation.

---

## 📋 Table of Contents

- [Getting Started](#-getting-started)
- [Development Setup](#-development-setup)
- [Project Structure](#-project-structure)
- [Code Style Guide](#-code-style-guide)
- [Adding a New Language](#-adding-a-new-language)
- [Adding New Features](#-adding-new-features)
- [Submitting Changes](#-submitting-changes)
- [Reporting Issues](#-reporting-issues)
- [Feature Requests](#-feature-requests)

---

## 🚀 Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork:
   ```bash
   git clone https://github.com/yourusername/VeloDelivery.git
   cd VeloDelivery
   ```
3. **Create** a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make** your changes and test them
5. **Commit** with a clear message:
   ```bash
   git commit -m "feat: add my awesome feature"
   ```
6. **Push** and create a Pull Request:
   ```bash
   git push origin feature/your-feature-name
   ```

---

## 🛠️ Development Setup

### Requirements

| Tool | Version | Notes |
|------|---------|-------|
| Java | 17+ | Required for compilation |
| Maven | 3.6+ | Build tool |
| Spigot/Paper | 1.16.5+ | Test server |
| IDE | Any | IntelliJ IDEA recommended |

### Building

```bash
# Full build with shaded JAR
mvn clean package

# Quick compile (no JAR)
mvn clean compile

# Run tests only
mvn test
```

The output JAR will be in `target/VeloDelivery-1.0.3.jar`.

### Testing Locally

1. Build the plugin: `mvn clean package`
2. Copy the JAR to your test server's `plugins/` folder
3. Start the server and verify functionality
4. Check console for any errors or warnings

---

## 📁 Project Structure

```
VeloDelivery/
├── src/main/java/com/blockstock/
│   ├── VeloDeliveryPlugin.java      # Main plugin class
│   ├── command/
│   │   ├── CommandHandler.java       # Admin command routing
│   │   └── DeliverCommand.java       # Player deliver commands
│   ├── config/
│   │   └── ConfigManagerImpl.java    # Configuration loading & validation
│   ├── gui/
│   │   └── DeliveryGUI.java          # Inventory GUI menus
│   ├── model/                        # Data models
│   ├── placeholder/
│   │   └── PlaceholderEngineImpl.java # PlaceholderAPI expansion
│   ├── reward/                       # Reward distribution
│   ├── service/                      # Business logic services
│   ├── util/
│   │   └── LoggingService.java       # Centralized logging
│   └── webhook/
│       └── WebhookServiceImpl.java   # Discord webhook integration
├── src/main/resources/
│   ├── plugin.yml                    # Plugin metadata
│   ├── config.yml                    # Main configuration
│   ├── categories.yml                # Item categories
│   ├── deliveries.yml                # Delivery event definitions
│   ├── items.yml                     # 500+ item definitions
│   └── lang/                         # Language files
│       ├── tr.yml                    # Turkish
│       ├── en.yml                    # English
│       ├── de.yml                    # German
│       ├── es.yml                    # Spanish
│       ├── fr.yml                    # French
│       ├── ru.yml                    # Russian
│       ├── zh.yml                    # Chinese (Simplified)
│       └── pt.yml                    # Portuguese
├── pom.xml                           # Maven build configuration
├── README.md
└── CONTRIBUTING.md                   # You are here!
```

---

## 📝 Code Style Guide

### General Rules

- **Indentation:** 4 spaces (no tabs)
- **Line length:** Max 120 characters
- **Encoding:** UTF-8 with LF line endings
- **Language:** Code in English, comments can be in English or Turkish

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `DeliveryGUI`, `ConfigManagerImpl` |
| Methods | camelCase | `handleCommand()`, `getActiveEvents()` |
| Constants | UPPER_SNAKE | `PERM_ADMIN`, `MAX_WINNERS` |
| Variables | camelCase | `playerName`, `activeCount` |
| Packages | lowercase | `com.blockstock.command` |

### Best Practices

- Add **JavaDoc** comments for all public methods
- Keep methods under **50 lines** when possible
- Use **meaningful variable names** — no single-letter names except loop iterators
- Prefer **switch expressions** (Java 14+) over if-else chains
- Use **`final`** for variables that shouldn't be reassigned
- Handle exceptions gracefully — never swallow exceptions silently
- Use the **`LoggingService`** for all console output

### Example

```java
/**
 * Starts a delivery event with the specified parameters.
 *
 * @param name     the delivery name from deliveries.yml
 * @param duration optional duration override
 * @param winners  number of winners (1-10)
 * @return true if the event was started successfully
 */
public boolean startEvent(String name, Duration duration, int winners) {
    // Implementation here
}
```

---

## 🌍 Adding a New Language

Adding a new language is one of the easiest ways to contribute! Follow these steps:

### Step 1: Copy the Template

```bash
cp src/main/resources/lang/en.yml src/main/resources/lang/<code>.yml
```

Where `<code>` is the [ISO 639-1](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) language code (e.g., `ja` for Japanese, `ko` for Korean, `it` for Italian).

### Step 2: Translate

Open the new file and translate **only the message values**. Keep all YAML keys, color codes (`&a`, `&c`, etc.), and placeholders (`{player}`, `{count}`, etc.) exactly as they are.

```yaml
# ❌ WRONG — Don't change keys or placeholders
erfolgreich: "&a✓ {sayac} Konfiguration erfolgreich!"

# ✅ CORRECT — Only change the human-readable text
success: "&a✓ Konfiguration erfolgreich neu geladen!"
```

### Step 3: Update the Header

Change the file header to reflect the new language:

```yaml
# ║                      VeloDelivery - <Language> Language File            ║
```

### Step 4: Register in Java

Add a line in `VeloDeliveryPlugin.java` → `saveDefaultConfigs()`:

```java
saveResourceIfNotExists("lang/<code>.yml");
```

### Step 5: Submit

Create a Pull Request with:
- The new language file
- The Java registration change
- A note about your fluency level in that language

---

## ✨ Adding New Features

### Before You Start

1. **Check existing issues** to avoid duplicating work
2. **Open an issue** to discuss the feature before implementing
3. **Consider scope** — large features may need to be broken into smaller PRs

### Guidelines

- New features should be **configurable** (enable/disable in `config.yml`)
- Add appropriate **language strings** to all 8 language files (at minimum `en.yml` and `tr.yml`)
- Include **permissions** for any new commands
- Update `plugin.yml` if adding new commands or permissions
- Add **tab completion** for new commands

---

## ✅ Submitting Changes

### Pull Request Checklist

- [ ] Code compiles without errors: `mvn clean compile`
- [ ] Follows the code style guide
- [ ] JavaDoc added for new public methods
- [ ] Language strings added to `en.yml` and `tr.yml` (at minimum)
- [ ] `plugin.yml` updated if new commands/permissions were added
- [ ] Changes tested on a local server
- [ ] PR description clearly explains what and why

### Commit Message Format

We use conventional commit messages:

```
feat: add daily streak bonus system
fix: resolve GUI not closing after delivery
docs: update README with new commands
lang: add Italian language file
refactor: simplify event scheduling logic
```

---

## 🐛 Reporting Issues

When reporting bugs, please include:

| Information | Example |
|-------------|---------|
| Minecraft Version | 1.20.4 |
| Server Software | Paper |
| VeloDelivery Version | 1.0.3 |
| Java Version | 17 |
| Steps to Reproduce | 1. Start event... 2. Click GUI... |
| Expected Behavior | Should show leaderboard |
| Actual Behavior | GUI closes immediately |
| Error Logs | Paste from `latest.log` |

> **Tip:** Enable `debug: true` in `config.yml` for more detailed error information.

---

## 💡 Feature Requests

We love hearing ideas! When suggesting a feature:

1. **Search** existing issues to avoid duplicates
2. **Describe** the use case — what problem does it solve?
3. **Explain** why it would be useful to other server owners
4. **Consider** if it fits the plugin's scope
5. **Suggest** how it might work from a user's perspective

---

## 📜 License

By contributing to VeloDelivery, you agree that your contributions will be licensed under the [MIT License](LICENSE).

---

<p align="center">
  <b>Thank you for contributing! 🎉</b><br>
  <sub>Every contribution makes VeloDelivery better for the entire Minecraft community.</sub>
</p>