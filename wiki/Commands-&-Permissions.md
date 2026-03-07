# Commands & Permissions

## Player Commands

| Command | Aliases | Permission | Description |
|---------|---------|-----------|-------------|
| `/delivery` | `/dm`, `/deliverymenu`, `/teslimat`, `/tm` | `swiftdelivery.use` | Opens the delivery GUI menu |
| `/deliver` | `/del`, `/teslim` | `swiftdelivery.deliver` | Quick-deliver items from your inventory |
| `/deliver chest` | — | `swiftdelivery.deliver.chest` | Right-click a chest to deliver its contents |

## Admin Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/swiftdelivery help` | `swiftdelivery.admin` | Shows the admin help menu |
| `/swiftdelivery reload` | `swiftdelivery.admin.reload` | Reloads all configuration files |
| `/swiftdelivery start <name>` | `swiftdelivery.admin.start` | Force-starts a delivery event |
| `/swiftdelivery stop <name>` | `swiftdelivery.admin.stop` | Stops an active delivery event |
| `/swiftdelivery list` | `swiftdelivery.admin.list` | Lists all deliveries and their status |
| `/swiftdelivery status` | `swiftdelivery.admin.status` | Shows plugin status and diagnostics |

**Admin command aliases:** `/sd`, `/dc`

## Permission Nodes

### Root
| Permission | Default | Description |
|-----------|---------|-------------|
| `swiftdelivery.*` | OP | All permissions |

### Admin
| Permission | Default | Description |
|-----------|---------|-------------|
| `swiftdelivery.admin` | OP | All admin permissions |
| `swiftdelivery.admin.reload` | OP | Reload configs |
| `swiftdelivery.admin.start` | OP | Start events |
| `swiftdelivery.admin.stop` | OP | Stop events |
| `swiftdelivery.admin.list` | OP | List deliveries |
| `swiftdelivery.admin.status` | OP | View plugin status |

### Player
| Permission | Default | Description |
|-----------|---------|-------------|
| `swiftdelivery.use` | Everyone | Open the delivery menu |
| `swiftdelivery.deliver` | Everyone | Deliver items |
| `swiftdelivery.deliver.auto` | Everyone | Auto-deliver from inventory |
| `swiftdelivery.deliver.chest` | Everyone | Deliver from chest |

### Bypass
| Permission | Default | Description |
|-----------|---------|-------------|
| `swiftdelivery.bypass` | OP | All bypass permissions |
| `swiftdelivery.bypass.cooldown` | OP | Skip delivery cooldown |
| `swiftdelivery.bypass.limit` | OP | Bypass delivery limits |
