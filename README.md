<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.16.5--1.21.4-brightgreen?style=for-the-badge&logo=minecraft" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk" alt="Java Version">
  <img src="https://img.shields.io/badge/API-Spigot%20%7C%20Paper%20%7C%20Purpur-blue?style=for-the-badge" alt="API Support">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="License">
</p>

<h1 align="center">📦 VeloDelivery</h1>
<p align="center"><b>Professional Delivery Event System for Minecraft Servers</b></p>
<p align="center">
  Transform your server with engaging delivery events!<br>
  Players collect and deliver items to earn rewards, compete on leaderboards,<br>
  and participate in scheduled or manual events.
</p>

---

## 🚀 Why VeloDelivery?

Most delivery/quest plugins are either too simple or overly complex. **VeloDelivery** strikes the perfect balance:

- ⚡ **Zero dependencies** — Drop the JAR in and go. Vault & PlaceholderAPI are optional.
- 🎯 **Event-driven** — Not just tasks — real competitive events with winners, leaderboards, and rewards.
- 🎨 **Beautiful GUI** — Custom head textures, SmallCaps typography, real-time updates.
- 🌍 **8 languages out of the box** — Turkish, English, German, Spanish, French, Russian, Chinese, Portuguese.
- 📡 **Discord integration** — Beautiful webhook embeds announce events automatically.
- ⚙️ **Fully configurable** — 500+ items, 17 categories, unlimited delivery types.

---

## ✨ Features

<details>
<summary><b>📦 Delivery Events</b></summary>

| Feature | Description |
|---------|-------------|
| Scheduled Events | Natural language scheduling: `every day at 18:00`, `every monday at 20:00` |
| Manual Events | Start events instantly with `/dc start <name>` |
| Concurrent Events | Run up to **5 events** simultaneously |
| Random Selection | Items randomly picked from configurable category pools |
| Winner System | 1-10 configurable winners per event |
| Visible Before Start | Optionally show upcoming events in the GUI |

</details>

<details>
<summary><b>🎨 Beautiful GUI</b></summary>

| Feature | Description |
|---------|-------------|
| Custom Heads | Unique textures for every menu element |
| SmallCaps Text | Modern, stylish typography |
| Live Updates | Real-time leaderboard and delivery counters |
| Intuitive Navigation | Clean, easy-to-use menu system |
| Delivery Detail View | Per-event stats, rankings, and quick-deliver buttons |

</details>

<details>
<summary><b>🏆 Reward System</b></summary>

| Feature | Description |
|---------|-------------|
| Item Rewards | Give items directly to winners |
| Command Rewards | Execute any command (economy, crates, ranks, etc.) |
| Combined Rewards | Items + commands together |
| Range-based Rewards | Different rewards for 1st, 2nd, 3rd, 4th-10th, etc. |
| Economy Integration | Sell delivered items for money (Vault) |
| Pending Rewards | Offline players receive rewards on next login |
| End Commands | Execute commands when events naturally end |

</details>

<details>
<summary><b>📡 Discord Webhook</b></summary>

| Feature | Description |
|---------|-------------|
| Event Start | Rich embed with item, category, duration, and winner count |
| Event End | Winner leaderboard with medal emojis 🥇🥈🥉 |
| Customizable | Full control over colors, titles, descriptions, thumbnails |
| Per-Event URLs | Different webhook URLs for different events |
| @everyone Mention | Optional ping for high-priority events |

</details>

<details>
<summary><b>🌍 Multi-Language (8 Languages)</b></summary>

| Flag | Language | Code |
|------|----------|------|
| 🇹🇷 | Türkçe | `tr` |
| 🇬🇧 | English | `en` |
| 🇩🇪 | Deutsch | `de` |
| 🇪🇸 | Español | `es` |
| 🇫🇷 | Français | `fr` |
| 🇷🇺 | Русский | `ru` |
| 🇨🇳 | 简体中文 | `zh` |
| 🇧🇷 | Português | `pt` |

> **Adding your own:** Copy any `lang/*.yml`, translate the values, set `language:` in `config.yml`. Done!

</details>

<details>
<summary><b>🔧 Advanced Features</b></summary>

| Feature | Description |
|---------|-------------|
| PlaceholderAPI | 12+ placeholders for scoreboards, holograms, TAB, etc. |
| Chest Delivery | Right-click a chest to deliver items directly from it |
| Inventory Delivery | One-click deliver all matching items from inventory |
| Data Persistence | Active events and player data survive server restarts |
| Sellable Items | Players can sell delivered items with configurable tax rates |
| Debug Mode | Detailed console logging for troubleshooting |
| Auto-Save | Configurable auto-save interval for player data |

</details>

---

## 📥 Installation

```
1. Download VeloDelivery.jar
2. Drop it into your server's /plugins/ folder
3. Restart the server
4. Edit config files in /plugins/VeloDelivery/
5. Use /dc reload to apply changes — no restart needed!
```

---

## 💻 Commands

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/dc help` | Help menu | `velodelivery.use` |
| `/dc version` | Plugin version info | `velodelivery.use` |
| `/dc reload` | Reload all configurations | `velodelivery.admin.reload` |
| `/dc info` | System information | `velodelivery.admin.info` |
| `/dc list` | List all deliveries | `velodelivery.admin.list` |
| `/dc start <name> [duration] [winners]` | Start event manually | `velodelivery.admin.start` |
| `/dc stop <name>` | Stop an active event | `velodelivery.admin.stop` |
| `/dc status [name]` | View active event status | `velodelivery.admin.status` |
| `/dc top [name]` | Show leaderboard | `velodelivery.use` |
| `/dc categories` | List all categories | `velodelivery.admin` |

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/teslimat` | Open delivery GUI menu | `velodelivery.use` |
| `/teslim` | Deliver items from inventory | `velodelivery.deliver` |

> **Aliases:** `/dc` is the main admin command. Player commands work with `/teslimat` and `/teslim`.

---

## 🔐 Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `velodelivery.*` | All permissions | `op` |
| `velodelivery.use` | Access GUI & basic commands | `true` |
| `velodelivery.admin` | General admin access | `op` |
| `velodelivery.admin.reload` | Reload configurations | `op` |
| `velodelivery.admin.start` | Start delivery events | `op` |
| `velodelivery.admin.stop` | Stop delivery events | `op` |
| `velodelivery.admin.list` | List deliveries | `op` |
| `velodelivery.admin.status` | Check event status | `op` |
| `velodelivery.admin.info` | View system info | `op` |
| `velodelivery.admin.event` | Event management | `op` |
| `velodelivery.deliver` | Deliver items | `true` |
| `velodelivery.deliver.auto` | Auto-deliver items | `true` |
| `velodelivery.deliver.chest` | Deliver from chests | `true` |
| `velodelivery.bypass.cooldown` | Bypass delivery cooldown | `op` |
| `velodelivery.bypass.limit` | Bypass delivery limits | `op` |
| `velodelivery.bypass.protection` | Bypass chest protection | `op` |
| `velodelivery.participate` | Participate in events | `true` |

---

## 📁 Configuration Files

| File | Description |
|------|-------------|
| `config.yml` | General settings, webhook config, display names, data saving |
| `categories.yml` | Item categories with weighted random selection |
| `deliveries.yml` | Delivery event definitions with scheduling & rewards |
| `items.yml` | 500+ Minecraft items with display names and prices |
| `lang/*.yml` | Translatable message files (8 languages included) |

### Quick Config Examples

<details>
<summary><b>📋 Creating a new delivery event</b></summary>

```yaml
# In deliveries.yml
deliveries:
  my-event:
    enabled: true
    display-name: "My Custom Event"
    visible-before-start: true
    category:
      mode: random          # or "fixed" with value: "ore"
    item:
      mode: random          # or "fixed" with value: "DIAMOND"
    timezone: Europe/Istanbul
    schedule:
      start: "every day 20:00"
      end: "every day 21:00"
    rewards:
      1:
        type: both
        item: DIAMOND
        amount: 10
        commands:
          - "eco give {player} 5000"
      2:
        type: command
        commands:
          - "eco give {player} 2500"
      default:
        type: command
        commands:
          - "eco give {player} 100"
    webhook:
      enabled: true
      url: ""               # Empty = uses global URL from config.yml
```

</details>

<details>
<summary><b>🔔 Setting up Discord webhooks</b></summary>

```yaml
# In config.yml
webhook:
  enabled: true
  url: "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL"
  mention-everyone: false
  start:
    title: "📦 {delivery} Started!"
    description: "A new delivery event has started!\\n..."
    color: "#00FF00"
    footer: "VeloDelivery • Delivery Event System"
  end:
    title: "🏆 {delivery} Ended!"
    color: "#FFD700"
```

</details>

---

## 📊 PlaceholderAPI

> Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (optional)

| Placeholder | Description |
|-------------|-------------|
| `%velodelivery_active_count%` | Number of active events |
| `%velodelivery_active_names%` | Names of active events |
| `%velodelivery_player_total%` | Player's total deliveries |
| `%velodelivery_player_rank%` | Player's current rank |
| `%velodelivery_event_item%` | Current event's required item |
| `%velodelivery_event_category%` | Current event's category |
| `%velodelivery_event_remaining%` | Time remaining in event |
| `%velodelivery_leaderboard%` | Top players leaderboard |
| `%velodelivery_top_1_name%` | #1 player name |
| `%velodelivery_top_1_count%` | #1 player delivery count |
| `%velodelivery_top_2_name%` | #2 player name |
| `%velodelivery_top_3_name%` | #3 player name |

---

## 🔄 Soft Dependencies

| Plugin | Purpose | Required? |
|--------|---------|-----------|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Economy integration (sell items) | Optional |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Placeholders for other plugins | Optional |

---

## ❓ FAQ

<details>
<summary><b>How do I change the language?</b></summary>

Edit `config.yml` and change `language: tr` to your preferred language code (`en`, `de`, `es`, `fr`, `ru`, `zh`, `pt`). Then run `/dc reload`.

</details>

<details>
<summary><b>Can I run multiple events at the same time?</b></summary>

Yes! VeloDelivery supports up to 5 concurrent events by default. You can change this in `config.yml` → `events.max-concurrent`.

</details>

<details>
<summary><b>How do I add custom items?</b></summary>

Edit `items.yml` and add your items with a display name and price. Then add them to a category in `categories.yml`.

</details>

<details>
<summary><b>Does it work with 1.21.4?</b></summary>

Yes! VeloDelivery supports Minecraft **1.16.5 through 1.21.4** on Spigot, Paper, and Purpur.

</details>

<details>
<summary><b>Where is data stored?</b></summary>

All active event data and player statistics are stored in `data.yml` inside the plugin folder. Auto-save interval is configurable.

</details>

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).

---

<p align="center">
  <b>Made with ❤️ for the Minecraft community</b><br>
  <sub>VeloDelivery — Professional Delivery Event System</sub>
</p>
