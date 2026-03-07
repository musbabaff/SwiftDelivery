package com.nettyforge.swiftdelivery.hook.papi;

import com.nettyforge.swiftdelivery.SwiftDeliveryPlugin;
import com.nettyforge.swiftdelivery.data.DatabaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class SwiftDeliveryExpansion extends PlaceholderExpansion {

    private final SwiftDeliveryPlugin plugin;
    private final DatabaseManager databaseManager;

    public SwiftDeliveryExpansion(SwiftDeliveryPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "NettyForge";
    }

    @Override
    public String getIdentifier() {
        return "swiftdelivery";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) return "";
        UUID uuid = player.getUniqueId();

        try {
            if (identifier.equals("total_sent")) {
                return "0"; // To be implemented with detailed tracked stats if needed
            }
            if (identifier.equals("total_received")) {
                long count = databaseManager.getMail(uuid).get().size();
                return String.valueOf(count);
            }
            if (identifier.equals("unread_mail")) {
                long count = databaseManager.getMail(uuid).get().stream().filter(m -> !m.claimed()).count();
                return String.valueOf(count);
            }
            if (identifier.equals("events_won") || identifier.equals("total_deliveries")) {
                long total = databaseManager.getGlobalLeaderboard(100).get().getOrDefault(uuid, 0);
                return String.valueOf(total);
            }
        } catch (InterruptedException | ExecutionException e) {
            return "0";
        }

        return null;
    }
}
