# SwiftDelivery Release Notes

All notable changes to SwiftDelivery are documented here.

---

## v1.5.0 — The Game-Changer Expansion (March 2026)
*The highly demanding premium feature update!*

### ✨ Custom Items Engines natively supported
- Deep natively supported parsers engineered for `Nexo`, `Oraxen`, and `ItemsAdder`.
- Deliver `NEXO:ruby_sword` or `ITEMSADDER:custom_block` through the config with zero hassle.
- Fallback mechanisms intact for `VANILLA:DIAMOND`.

### ✉️ P2P Mail System & Profiles
- **Mail System:** Secure cross-server asynchronous system sending items offline (`/mail send`).
- **Profile GUI:** Instantly track Courier XP and Level via `/delivery profile`. Claim Mail rewards dynamically!
- **Vault Fees:** Charge configurable amounts via Vault when players send P2P Mail.

### 📈 Courier Progression System
- Gain `Courier XP` every time you securely send Mail or complete successful event deliveries.
- Climb the Courier Leveling System uniquely built into `SwiftDelivery`.

### 🛡️ Bulletproof Anti-Dupe 
- Newly crafted `swift_transaction_logs` schema tracks 100% of Delivery & Mail transfers using Transaction UUIDs.
- Admins can instantly audit ANY player’s deliveries in-game: `/swiftdelivery adminlog <player>`.

### 🌈 Next-Gen Cosmetics & Messaging
- Universal `MiniMessage` parsing implemented natively. Use `<gradient:red:blue>` freely in any local `.yml` file.
- `AdventureHook` backwards-compatibility securely preserves legacy color formats (`&c`, `&#FF0000`).
- Attach premium `Sound` and `Particle` effects immediately upon player deliveries. Fully configurable.

### ⚡ NPC & Hub Native Integrations
- Boundless hooks integrated for `Citizens` and `FancyNpcs`.
- Use the fallback internal command `/swiftdelivery _internal_npc_open` for generic NPC plugins like ZNPCs.

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
