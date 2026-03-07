# SwiftDelivery Setup Guide

Welcome to SwiftDelivery! This guide will walk you through the complete setup process.

## Prerequisites

- Minecraft server running **Spigot**, **Paper**, or **Purpur** (1.16.5 – 1.21.4)
- **Java 17** or higher installed on your server
- Server access via FTP or file manager

## Step 1: Installation

1. Download the latest `SwiftDelivery-x.x.x.jar` from [GitHub Releases](https://github.com/musbabaff/SwiftDelivery/releases)
2. Place the JAR file in your server's `plugins/` directory
3. Restart your server (or run `/reload confirm` — full restart recommended)

## Step 2: First-Time Configuration

After the first startup, the following files are generated in `plugins/SwiftDelivery/`:

```
plugins/SwiftDelivery/
├── config.yml         # General settings
├── deliveries.yml     # Delivery event definitions
├── categories.yml     # Item categories
├── items.yml          # Custom item names
├── data.yml           # Auto-generated data storage
└── locales/           # Language files
    ├── en.yml
    ├── tr.yml
    ├── de.yml
    ├── es.yml
    ├── fr.yml
    ├── pt.yml
    ├── ru.yml
    └── zh.yml
```

## Step 3: Choose Your Language

Edit `config.yml` and set your preferred language:

```yaml
general:
  language: en   # Options: en, tr, de, es, fr, pt, ru, zh
```

## Step 4: Configure Deliveries

Edit `deliveries.yml` to set up your delivery events. Each delivery has:
- **Items**: What players need to deliver
- **Schedule**: When the event starts/stops
- **Duration**: How long the event lasts
- **Rewards**: What winners receive

## Step 5: Set Up Discord Webhooks (Optional)

1. In your Discord server, go to **Channel Settings → Integrations → Webhooks**
2. Click **New Webhook**, copy the URL
3. Paste into `config.yml`:

```yaml
webhook:
  enabled: true
  url: "https://discord.com/api/webhooks/your-webhook-url"
```

## Step 6: Verify

1. Join your server
2. Run `/delivery` to open the delivery menu
3. Check the console for `[SwiftDelivery] Successfully loaded!`

## Getting Help

- Check the other [Wiki pages](.) for detailed documentation
- Open an [Issue on GitHub](https://github.com/musbabaff/SwiftDelivery/issues)
