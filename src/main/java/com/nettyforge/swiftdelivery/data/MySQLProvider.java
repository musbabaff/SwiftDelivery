package com.nettyforge.swiftdelivery.data;

import com.nettyforge.swiftdelivery.SwiftDeliveryPlugin;
import com.nettyforge.swiftdelivery.model.Winner;
import com.nettyforge.swiftdelivery.model.Mail;
import com.nettyforge.swiftdelivery.service.ActiveEvent;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MySQLProvider implements DatabaseManager {

    private final SwiftDeliveryPlugin plugin;
    private HikariDataSource dataSource;
    
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;
    private final int poolSize;

    public MySQLProvider(SwiftDeliveryPlugin plugin, String host, int port, String database, String username, String password, boolean useSSL, int poolSize) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
        this.poolSize = poolSize;
    }

    @Override
    public boolean connect() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setPoolName("SwiftDelivery-MySQL");
            config.setMaximumPoolSize(poolSize);
            config.setConnectionTimeout(10000);
            
            // HikariCP Optimizations for MySQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);

            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            return false;
        }
    }

    private void createTables() {
        String createEventsTable = "CREATE TABLE IF NOT EXISTS swift_active_events (" +
                "delivery_name VARCHAR(64) PRIMARY KEY, " +
                "category VARCHAR(64) NOT NULL, " +
                "item VARCHAR(128) NOT NULL, " +
                "end_time BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createDeliveriesTable = "CREATE TABLE IF NOT EXISTS swift_player_deliveries (" +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "delivery_name VARCHAR(64) NOT NULL, " +
                "amount INT NOT NULL, " +
                "PRIMARY KEY (player_uuid, delivery_name), " +
                "FOREIGN KEY (delivery_name) REFERENCES swift_active_events(delivery_name) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createHistoryTable = "CREATE TABLE IF NOT EXISTS swift_event_history (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "delivery_name VARCHAR(64) NOT NULL, " +
                "total_participants INT NOT NULL, " +
                "total_items INT NOT NULL, " +
                "completed_at BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createLeaderboardTable = "CREATE TABLE IF NOT EXISTS swift_leaderboard (" +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "delivery_name VARCHAR(64) NOT NULL, " +
                "total_amount INT NOT NULL, " +
                "PRIMARY KEY (player_uuid, delivery_name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createMailTable = "CREATE TABLE IF NOT EXISTS swift_mail (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "sender VARCHAR(36) NOT NULL, " +
                "receiver VARCHAR(36) NOT NULL, " +
                "item_base64 TEXT NOT NULL, " +
                "sent_at BIGINT NOT NULL, " +
                "claimed TINYINT(1) DEFAULT 0" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createTransactionLogsTable = "CREATE TABLE IF NOT EXISTS swift_transaction_logs (" +
                "transaction_id VARCHAR(36) PRIMARY KEY, " +
                "sender VARCHAR(36) NOT NULL, " +
                "receiver VARCHAR(36) NOT NULL, " +
                "item_summary VARCHAR(128) NOT NULL, " +
                "action VARCHAR(32) NOT NULL, " +
                "timestamp BIGINT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createPlayerProfilesTable = "CREATE TABLE IF NOT EXISTS swift_player_profiles (" +
                "player_uuid VARCHAR(36) PRIMARY KEY, " +
                "courier_xp INT DEFAULT 0, " +
                "courier_level INT DEFAULT 1" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(createEventsTable);
             PreparedStatement stmt2 = conn.prepareStatement(createDeliveriesTable);
             PreparedStatement stmt3 = conn.prepareStatement(createHistoryTable);
             PreparedStatement stmt4 = conn.prepareStatement(createLeaderboardTable);
             PreparedStatement stmt5 = conn.prepareStatement(createMailTable);
             PreparedStatement stmt6 = conn.prepareStatement(createTransactionLogsTable);
             PreparedStatement stmt7 = conn.prepareStatement(createPlayerProfilesTable)) {

            stmt1.execute();
            stmt2.execute();
            stmt3.execute();
            stmt4.execute();
            stmt5.execute();
            stmt6.execute();
            stmt7.execute();

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create MySQL tables: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<Void> saveActiveEvent(ActiveEvent event) {
        return CompletableFuture.runAsync(() -> {
            String insertEvent = "INSERT INTO swift_active_events (delivery_name, category, item, end_time) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE category=VALUES(category), item=VALUES(item), end_time=VALUES(end_time)";
            
            String insertDelivery = "INSERT INTO swift_player_deliveries (player_uuid, delivery_name, amount) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE amount=VALUES(amount)";

            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                // Save event
                try (PreparedStatement stmt = conn.prepareStatement(insertEvent)) {
                    stmt.setString(1, event.getDeliveryName());
                    stmt.setString(2, event.getResolvedCategory());
                    stmt.setString(3, event.getResolvedItem());
                    stmt.setLong(4, event.getEndTime().toEpochSecond());
                    stmt.executeUpdate();
                }

                // Unlike SQLite's blind INSERT OR REPLACE, MySQL's ON DUPLICATE KEY allows us to skip the DELETE step
                // for player deliveries, just updating values instead.
                
                // Save new player deliveries
                try (PreparedStatement stmt = conn.prepareStatement(insertDelivery)) {
                    for (Map.Entry<UUID, Integer> entry : event.getPlayerDeliveries().entrySet()) {
                        stmt.setString(1, entry.getKey().toString());
                        stmt.setString(2, event.getDeliveryName());
                        stmt.setInt(3, entry.getValue());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save active event " + event.getDeliveryName() + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteActiveEvent(String deliveryName) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt1 = conn.prepareStatement("DELETE FROM swift_player_deliveries WHERE delivery_name = ?");
                 PreparedStatement stmt2 = conn.prepareStatement("DELETE FROM swift_active_events WHERE delivery_name = ?")) {

                conn.setAutoCommit(false);
                stmt1.setString(1, deliveryName);
                stmt1.executeUpdate();
                stmt2.setString(1, deliveryName);
                stmt2.executeUpdate();
                conn.commit();

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete active event " + deliveryName + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<ActiveEvent>> loadAllActiveEvents() {
        return CompletableFuture.supplyAsync(() -> {
            List<ActiveEvent> events = new ArrayList<>();
            Map<String, ActiveEvent> eventMap = new HashMap<>();

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM swift_active_events");
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String name = rs.getString("delivery_name");
                    String cat = rs.getString("category");
                    String item = rs.getString("item");
                    long endTime = rs.getLong("end_time");

                    ActiveEvent event = new ActiveEvent(name, cat, item, java.time.ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(endTime), java.time.ZoneId.systemDefault()));
                    events.add(event);
                    eventMap.put(name, event);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load active events: " + e.getMessage());
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM swift_player_deliveries");
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String uuidStr = rs.getString("player_uuid");
                    String eventName = rs.getString("delivery_name");
                    int amount = rs.getInt("amount");

                    ActiveEvent event = eventMap.get(eventName);
                    if (event != null) {
                        try {
                            event.addDelivery(UUID.fromString(uuidStr), amount);
                        } catch (Exception ignored) {}
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player deliveries: " + e.getMessage());
            }

            return events;
        });
    }

    @Override
    public CompletableFuture<Void> saveEventHistory(String deliveryName, List<Winner> winners, int totalParticipants, int totalItems) {
        return CompletableFuture.runAsync(() -> {
            String insertHistory = "INSERT INTO swift_event_history (delivery_name, total_participants, total_items, completed_at) VALUES (?, ?, ?, ?)";
            String updateLeaderboard = "INSERT INTO swift_leaderboard (player_uuid, delivery_name, total_amount) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE total_amount = total_amount + VALUES(total_amount)";

            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                // Insert into history
                try (PreparedStatement stmt = conn.prepareStatement(insertHistory)) {
                    stmt.setString(1, deliveryName);
                    stmt.setInt(2, totalParticipants);
                    stmt.setInt(3, totalItems);
                    stmt.setLong(4, System.currentTimeMillis() / 1000L);
                    stmt.executeUpdate();
                }

                // Update permanent leaderboard for all players who delivered
                if (winners != null && !winners.isEmpty()) {
                    try (PreparedStatement stmt = conn.prepareStatement(updateLeaderboard)) {
                        for (Winner winner : winners) {
                            stmt.setString(1, winner.playerUuid().toString());
                            stmt.setString(2, deliveryName);
                            stmt.setInt(3, winner.deliveryCount());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save event history for " + deliveryName + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<java.util.Map<UUID, Integer>> getLeaderboard(String deliveryName, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Integer> leaderboard = new LinkedHashMap<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT player_uuid, total_amount FROM swift_leaderboard WHERE delivery_name = ? ORDER BY total_amount DESC LIMIT ?")) {
                
                stmt.setString(1, deliveryName);
                stmt.setInt(2, Math.min(limit, 100));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                            leaderboard.put(uuid, rs.getInt("total_amount"));
                        } catch (Exception ignored) {}
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to fetch leaderboard: " + e.getMessage());
            }
            return leaderboard;
        });
    }

    @Override
    public CompletableFuture<java.util.Map<UUID, Integer>> getGlobalLeaderboard(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Integer> leaderboard = new LinkedHashMap<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT player_uuid, SUM(total_amount) as grand_total FROM swift_leaderboard GROUP BY player_uuid ORDER BY grand_total DESC LIMIT ?")) {
                
                stmt.setInt(1, Math.min(limit, 100));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                            leaderboard.put(uuid, rs.getInt("grand_total"));
                        } catch (Exception ignored) {}
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to fetch global leaderboard: " + e.getMessage());
            }
            return leaderboard;
        });
    }

    // --- P2P Mail Operations ---

    @Override
    public CompletableFuture<Void> saveMail(Mail mail) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO swift_mail (sender, receiver, item_base64, sent_at, claimed) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, mail.sender().toString());
                stmt.setString(2, mail.receiver().toString());
                stmt.setString(3, com.nettyforge.swiftdelivery.util.ItemSerializationUtil.itemStackToBase64(mail.item()));
                stmt.setLong(4, mail.sentAt());
                stmt.setBoolean(5, mail.claimed());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save mail for " + mail.receiver() + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<Mail>> getMail(UUID receiver) {
        return CompletableFuture.supplyAsync(() -> {
            List<Mail> inbox = new ArrayList<>();
            String sql = "SELECT * FROM swift_mail WHERE receiver = ? AND claimed = 0 ORDER BY sent_at DESC LIMIT 50";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, receiver.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        inbox.add(new Mail(
                            rs.getInt("id"),
                            UUID.fromString(rs.getString("sender")),
                            receiver,
                            com.nettyforge.swiftdelivery.util.ItemSerializationUtil.itemStackFromBase64(rs.getString("item_base64")),
                            rs.getLong("sent_at"),
                            rs.getBoolean("claimed")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get mail for " + receiver + ": " + e.getMessage());
            }
            return inbox;
        });
    }

    @Override
    public CompletableFuture<Void> markMailClaimed(int mailId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE swift_mail SET claimed = 1 WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, mailId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to mark mail as claimed: " + e.getMessage());
            }
        });
    }

    // --- Tier 1 Security & RPG Operations ---

    @Override
    public CompletableFuture<Void> logTransaction(String transactionId, UUID sender, UUID receiver, String itemSummary, String action) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO swift_transaction_logs (transaction_id, sender, receiver, item_summary, action, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, transactionId);
                stmt.setString(2, sender.toString());
                stmt.setString(3, receiver.toString());
                stmt.setString(4, itemSummary);
                stmt.setString(5, action);
                stmt.setLong(6, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to log transaction " + transactionId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> getPlayerTransactions(UUID playerUuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> logs = new ArrayList<>();
            String sql = "SELECT * FROM swift_transaction_logs WHERE sender = ? OR receiver = ? ORDER BY timestamp DESC LIMIT ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, playerUuid.toString());
                stmt.setInt(3, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        long ts = rs.getLong("timestamp");
                        String action = rs.getString("action");
                        String item = rs.getString("item_summary");
                        String tx = rs.getString("transaction_id");
                        logs.add(String.format("[%tF %tT] %s - %s (ID: %s)", ts, ts, action, item, tx));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to fetch transactions for " + playerUuid + ": " + e.getMessage());
            }
            return logs;
        });
    }

    @Override
    public CompletableFuture<Void> addCourierXp(UUID playerUuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO swift_player_profiles (player_uuid, courier_xp, courier_level) " +
                         "VALUES (?, ?, 1) " +
                         "ON DUPLICATE KEY UPDATE " +
                         "courier_xp = courier_xp + ?, " +
                         "courier_level = (courier_xp + ?) / 100 + 1"; // Fast basic formula: level = xp/100 + 1
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setInt(2, amount);
                stmt.setInt(3, amount);
                stmt.setInt(4, amount);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to add Courier XP for " + playerUuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getCourierXp(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT courier_xp FROM swift_player_profiles WHERE player_uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getInt("courier_xp");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get Courier XP for " + playerUuid + ": " + e.getMessage());
            }
            return 0;
        });
    }

    @Override
    public CompletableFuture<Integer> getCourierLevel(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT courier_level FROM swift_player_profiles WHERE player_uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getInt("courier_level");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get Courier Level for " + playerUuid + ": " + e.getMessage());
            }
            return 1;
        });
    }
}
