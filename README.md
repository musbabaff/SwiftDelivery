<div align="center">
  <h1>⚡ SwiftDelivery</h1>
  <p><strong>Premium Delivery Event Automation Plugin for Minecraft Servers</strong></p>
  <p>
    <a href="https://github.com/musbabaff/SwiftDelivery/releases"><img src="https://img.shields.io/github/v/release/musbabaff/SwiftDelivery?style=for-the-badge&color=brightgreen&label=Latest%20Release" alt="Release"></a>
    <a href="https://github.com/musbabaff/SwiftDelivery/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=for-the-badge" alt="License"></a>
    <img src="https://img.shields.io/badge/MC-1.16.5%20–%201.21.4-blue?style=for-the-badge&logo=minecraft" alt="Minecraft Version">
    <img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk" alt="Java 17+">
  </p>
  <p>
    <a href="#-features">Features</a> •
    <a href="#-installation">Installation</a> •
    <a href="#-commands">Commands</a> •
    <a href="#-configuration">Configuration</a> •
    <a href="wiki/">Wiki</a> •
    <a href="release_notes.md">Changelog</a>
  </p>
  <br>
  <em>Built by <strong>NettyForge</strong> — crafting premium server tools.</em>
</div>

---

## 🚀 Features

| Feature | Description |
|---------|-------------|
| **📦 Scheduled Deliveries** | Fully automated delivery events with cron-like scheduling |
| **🏆 Leaderboard System** | Real-time rankings with customizable rewards for top deliverers |
| **🎨 Custom GUI Menus** | Beautiful, compact inventory menus with custom head textures |
| **🌍 Multi-Language** | 8 built-in locales: English, Turkish, German, Spanish, French, Portuguese, Russian, Chinese |
| **🔔 Discord Webhooks** | Automated Discord notifications with rich embeds for event start/end |
| **⚡ Chest Delivery** | Right-click a chest to bulk-deliver items — no manual clicking! |
| **🎁 Flexible Rewards** | Commands, items, economy (Vault), and experience rewards |
| **📊 Auto Update Checker** | Automatic GitHub release detection with in-game admin notifications |
| **🔧 Hot Reload** | Reload all configs without restarting the server |
| **💾 Persistent Data** | Auto-saving with crash recovery for active events |

## 📋 Requirements

- **Java:** 17 or higher
- **Server:** Spigot, Paper, Purpur, or any fork (1.16.5 – 1.21.4)
- **Optional:** Vault (for economy rewards), PlaceholderAPI

## 📥 Installation

1. **Download** the latest `SwiftDelivery-x.x.x.jar` from [Releases](https://github.com/musbabaff/SwiftDelivery/releases)
2. **Place** the JAR in your server's `plugins/` folder
3. **Start** your server — default configs will be generated
4. **Configure** `plugins/SwiftDelivery/config.yml` to your liking
5. **Set up deliveries** in `plugins/SwiftDelivery/deliveries.yml`
6. **Reload** with `/swiftdelivery reload` or restart

## 💻 Commands

### Player Commands
| Command | Permission | Description |
|---------|-----------|-------------|
| `/delivery` | `swiftdelivery.use` | Opens the delivery menu |
| `/deliver` | `swiftdelivery.deliver` | Quick-deliver items from inventory |
| `/deliver chest` | `swiftdelivery.deliver.chest` | Deliver items from a chest |

### Admin Commands
| Command | Permission | Description |
|---------|-----------|-------------|
| `/swiftdelivery reload` | `swiftdelivery.admin.reload` | Reload all configuration files |
| `/swiftdelivery start <name>` | `swiftdelivery.admin.start` | Force-start a delivery event |
| `/swiftdelivery stop <name>` | `swiftdelivery.admin.stop` | Stop an active delivery event |
| `/swiftdelivery list` | `swiftdelivery.admin.list` | List all deliveries and their status |
| `/swiftdelivery status` | `swiftdelivery.admin.status` | View plugin status and diagnostics |

## ⚙️ Configuration

All configuration files are generated in `plugins/SwiftDelivery/`:

| File | Purpose |
|------|---------|
| `config.yml` | General settings, display names, webhooks, events |
| `deliveries.yml` | Delivery definitions, schedules, rewards |
| `categories.yml` | Item categories with weights |
| `items.yml` | Custom item display names per locale |
| `locales/*.yml` | Language files (8 built-in locales) |

> 📖 **Full documentation** is available in the [Wiki](wiki/).

## 🔔 Discord Webhook

SwiftDelivery can send beautiful embed notifications to your Discord server:

1. Create a webhook in your Discord channel settings
2. Paste the URL into `config.yml` under `webhook.url`
3. Set `webhook.enabled: true`
4. Customize the embed templates in `config.yml`

## 🏗️ Building from Source

```bash
git clone https://github.com/musbabaff/SwiftDelivery.git
cd SwiftDelivery
mvn clean package
```

The compiled JAR will be at `target/SwiftDelivery-1.0.0.jar`.

> See [BUILDING.md](BUILDING.md) for detailed build instructions.

## 📄 License

This project is **All Rights Reserved**. See [LICENSE](LICENSE) for details.

## 🤝 Support

- **Issues:** [GitHub Issues](https://github.com/musbabaff/SwiftDelivery/issues)
- **Wiki:** [Documentation](wiki/)

---

<div align="center">
  <strong>⚡ SwiftDelivery</strong> — Premium Delivery Events for Minecraft<br>
  <em>Made with ❤️ by <strong>NettyForge</strong></em>
</div>
