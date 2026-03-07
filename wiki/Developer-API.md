# Developer API

SwiftDelivery provides a lightweight API for other plugins to interact with the delivery event system.

## Getting the Plugin Instance

```java
SwiftDeliveryPlugin plugin = (SwiftDeliveryPlugin) Bukkit.getPluginManager().getPlugin("SwiftDelivery");
```

## Custom Events

SwiftDelivery fires custom Bukkit events that you can listen to.

### DeliveryDeliverEvent

Fired when a player successfully delivers an item.

```java
import com.nettyforge.swiftdelivery.event.DeliveryDeliverEvent;

@EventHandler
public void onDelivery(DeliveryDeliverEvent event) {
    Player player = event.getPlayer();
    String deliveryName = event.getDeliveryName();
    int amount = event.getAmount();
    
    // Your custom logic here
    player.sendMessage("You delivered " + amount + " items!");
}
```

## Service Architecture

SwiftDelivery uses a modular service architecture:

| Service | Interface | Description |
|---------|-----------|-------------|
| `DeliveryService` | Manages active delivery events | Start, stop, query events |
| `CategoryService` | Manages item categories | Category lookups and validation |
| `RewardService` | Handles reward distribution | Pending rewards, delivery |
| `WebhookService` | Discord webhook integration | Send embed notifications |
| `MessageService` | Localized messaging | Player-facing messages |
| `SchedulerService` | Event scheduling | Cron-like event automation |

## Data Models

### ActiveEvent
Represents a currently running delivery event with player delivery counts and rankings.

### DeliveryDefinition
A record defining a delivery event's configuration (items, schedule, rewards, duration).

### Winner
A record representing a delivery event winner with player name, UUID, rank, and delivery count.

## Building with SwiftDelivery as a Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.nettyforge</groupId>
    <artifactId>SwiftDelivery</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

And add `SwiftDelivery` to your `plugin.yml` dependencies:

```yaml
depend:
  - SwiftDelivery
```
