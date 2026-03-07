# SwiftDelivery Release Notes

All notable changes to SwiftDelivery are documented here.

---

## v1.0.0 — The Rebirth (March 2026)

### 🎉 Complete Rebranding
- **VeloDelivery** → **SwiftDelivery** — a premium, international-ready plugin
- **blockstock** → **NettyForge** — new studio identity
- Package structure: `com.nettyforge.swiftdelivery.*`

### 🌍 Full Internationalization
- 100% English codebase — zero hardcoded player-facing strings
- 8 built-in locales: English, Turkish, German, Spanish, French, Portuguese, Russian, Chinese
- All messages fetched from locale files — change language with one config option

### 🎨 GUI Improvements
- Compact 36-slot main menu with dynamic centering
- Custom head textures with 1.16.5 – 1.21.4 compatibility
- Decorative glass borders and polished layout

### 🔔 Discord Webhooks
- Rich embed notifications for event start and end
- Customizable embed templates with placeholders
- Winner announcements with medal emojis
- Debug logging for webhook troubleshooting

### 📊 Auto Update Checker
- Checks GitHub releases on plugin startup
- In-game notification for OP players when a new version is available

### 🏆 Delivery System
- Scheduled delivery events with cron-like timing
- Real-time leaderboard with player rankings
- Inventory and chest delivery modes
- Flexible reward system (commands, items, economy, XP)
- Persistent data with auto-save and crash recovery

### 🔧 Technical
- Java 17+ required
- Compatible with Spigot, Paper, Purpur (1.16.5 – 1.21.4)
- SnakeYAML 2.5 shaded for configuration
- Clean logger output (`[SwiftDelivery]` prefix only)

---

*For older VeloDelivery changelog, see the git history.*
