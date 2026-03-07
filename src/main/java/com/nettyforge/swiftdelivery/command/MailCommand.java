package com.nettyforge.swiftdelivery.command;

import com.nettyforge.swiftdelivery.SwiftDeliveryPlugin;
import com.nettyforge.swiftdelivery.data.DatabaseManager;
import com.nettyforge.swiftdelivery.hook.economy.VaultHook;
import com.nettyforge.swiftdelivery.model.Mail;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MailCommand implements CommandExecutor {

    private final SwiftDeliveryPlugin plugin;
    private final DatabaseManager databaseManager;
    private final VaultHook vaultHook;

    public MailCommand(SwiftDeliveryPlugin plugin, DatabaseManager databaseManager, VaultHook vaultHook) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.vaultHook = vaultHook;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("swiftdelivery.mail")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this.");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("send")) {
            player.sendMessage(ChatColor.RED + "Usage: /swiftmail send <player>");
            return true;
        }

        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        UUID targetUuid;
        
        if (targetPlayer != null) {
            targetUuid = targetPlayer.getUniqueId();
        } else {
            // Offline player support
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            targetUuid = offlineTarget.getUniqueId();
            if (targetUuid == null || (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline())) {
                player.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
        }
        
        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot send mail to yourself.");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must hold an item to send!");
            return true;
        }

        double fee = plugin.getConfig().getDouble("mail.fee", 50.0);
        if (fee > 0 && vaultHook != null && vaultHook.isEnabled()) {
            if (!vaultHook.hasAccount(player) || vaultHook.getBalance(player) < fee) {
                player.sendMessage(ChatColor.RED + "You need $" + fee + " to send a mail.");
                return true;
            }
            vaultHook.withdraw(player, fee);
            player.sendMessage(ChatColor.GREEN + "Charged $" + fee + " for delivery.");
        }

        ItemStack itemToSend = itemInHand.clone();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        Mail mail = new Mail(0, player.getUniqueId(), targetUuid, itemToSend, System.currentTimeMillis() / 1000L, false);
        
        databaseManager.saveMail(mail).thenRun(() -> {
            databaseManager.addCourierXp(player.getUniqueId(), 5); // 5 XP per mail
            
            String summary = itemToSend.getAmount() + "x " + itemToSend.getType().name();
            databaseManager.logTransaction(UUID.randomUUID().toString(), player.getUniqueId(), targetUuid, summary, "P2P_MAIL");

            // Cosmetics
            org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
            String soundName = config.getString("events.cosmetics.mail-send-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            String particleName = config.getString("events.cosmetics.mail-send-particle", "HEART");

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f);
                } catch (Exception ignored) {}

                try {
                    player.spawnParticle(org.bukkit.Particle.valueOf(particleName), player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
                } catch (Exception ignored) {}
            });

            player.sendMessage(ChatColor.GREEN + "You sent an item to " + targetName + "!");
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(ChatColor.GOLD + "[SwiftDelivery] " + ChatColor.GREEN + player.getName() + " sent you a mail! Check your inbox.");
            }
        }).exceptionally(ex -> {
            player.sendMessage(ChatColor.RED + "Failed to send mail. Contact an administrator.");
            plugin.getLogger().severe("Failed to send mail from " + player.getName() + ": " + ex.getMessage());
            Bukkit.getScheduler().runTask(plugin, () -> player.getInventory().addItem(itemToSend));
            return null;
        });

        return true;
    }
}
