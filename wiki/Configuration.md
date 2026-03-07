# Configuration Guide

## config.yml

The main configuration file controls general settings, display names, event behavior, and webhooks.

### General Settings

```yaml
general:
  language: en           # Locale: en, tr, de, es, fr, pt, ru, zh
  timezone: Europe/Istanbul
  debug: false           # Enable verbose console logging
```

### Category Display Names

Custom display names for item categories in the GUI:

```yaml
category-display-names:
  farm: "Farm"
  ore: "Ore"
  block: "Block"
  food: "Food"
  # ... add more as needed
```

### Delivery Display Names

Custom names for each delivery event:

```yaml
delivery-display-names:
  daily: "Daily Delivery"
  mining: "Mining Delivery"
  grand-event: "Grand Event"
```

### Event Settings

```yaml
events:
  max-concurrent: 5      # Max simultaneous active events

  announcements:
    start: true           # Broadcast event start
    end: true             # Broadcast event end

  sounds:
    enabled: true
    start: "ENTITY_PLAYER_LEVELUP"
    end: "UI_TOAST_CHALLENGE_COMPLETE"

  title:
    enabled: true
    fade-in: 10
    stay: 70
    fade-out: 20
```

### Webhook Configuration

```yaml
webhook:
  enabled: true
  url: "https://discord.com/api/webhooks/..."
  mention-everyone: false

  start:
    title: "📦 {delivery} Started!"
    description: "A new event has started!..."
    color: "#00FF00"
    footer: "SwiftDelivery | Delivery Event System"

  end:
    title: "🏆 {delivery} Ended!"
    description: "Event complete!..."
    color: "#FFD700"
```

#### Available Placeholders

| Placeholder | Context | Description |
|------------|---------|-------------|
| `{delivery}` | Start / End | Delivery event name |
| `{item}` | Start / End | Required item name |
| `{category}` | Start / End | Category name |
| `{duration}` | Start | Event duration |
| `{winners}` | Start / End | Winner count or list |
| `{end_time}` | Start | Event end time |
| `{total}` | End | Total deliveries |
| `{participants}` | End | Participant count |

## deliveries.yml

Define your delivery events, schedules, rewards, and item pools here. See the default file for a complete example.

## categories.yml

Define item categories with associated items and selection weights.

## items.yml

Custom display names for Minecraft items, organized by locale.

## locales/*.yml

Full message localization files. Every player-facing string is customizable.
