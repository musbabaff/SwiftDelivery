package com.nettyforge.swiftdelivery.gui;

import com.nettyforge.swiftdelivery.data.DatabaseManager;
import com.nettyforge.swiftdelivery.model.Mail;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProfileGUI implements Listener {

    private final DatabaseManager databaseManager;
    private final String GUI_TITLE = "§8Your Profile & Inbox";
    
    // Cache for tracking
    private final Map<UUID, List<Mail>> viewingMails = new HashMap<>();

    public ProfileGUI(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void openProfile(Player player) {
        databaseManager.getMail(player.getUniqueId()).thenAccept(mails -> {
            databaseManager.getCourierXp(player.getUniqueId()).thenAccept(xp -> {
                databaseManager.getCourierLevel(player.getUniqueId()).thenAccept(level -> {
                    Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("SwiftDelivery"), () -> {
                        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
                        
                        ItemStack statsItem = new ItemStack(Material.PLAYER_HEAD);
                        ItemMeta statsMeta = statsItem.getItemMeta();
                        if (statsMeta != null) {
                            statsMeta.setDisplayName("§6§l" + player.getName() + "'s Profile");
                            List<String> statsLore = new ArrayList<>();
                            long unread = mails.stream().filter(m -> !m.claimed()).count();
                            long totalReceived = mails.size();
                            statsLore.add("§7Unread Mail: §e" + unread);
                            statsLore.add("§7Total Received: §f" + totalReceived);
                            statsLore.add("");
                            statsLore.add("§b⚔ Courier Level: §3" + level);
                            int nextLevelXp = level * 100;
                            statsLore.add("§b⭐ Courier XP: §3" + xp + " §8/ §b" + nextLevelXp);
                            statsMeta.setLore(statsLore);
                            statsItem.setItemMeta(statsMeta);
                        }
                        inv.setItem(4, statsItem);
                        
                        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                        ItemMeta glassMeta = glass.getItemMeta();
                        if (glassMeta != null) {
                            glassMeta.setDisplayName(" ");
                            glass.setItemMeta(glassMeta);
                        }
                        for (int i = 0; i < 9; i++) {
                            if (i != 4) inv.setItem(i, glass);
                        }
                        
                        List<Mail> unreadMails = mails.stream().filter(m -> !m.claimed()).toList();
                        viewingMails.put(player.getUniqueId(), unreadMails);
                        
                        int slot = 9;
                        for (Mail mail : unreadMails) {
                            if (slot >= 54) break;
                            ItemStack mailItem = mail.item().clone();
                            ItemMeta meta = mailItem.getItemMeta();
                            if (meta != null) {
                                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                                lore.add("");
                                lore.add("§aClick to claim this item!");
                                lore.add("§8Sender: " + Bukkit.getOfflinePlayer(mail.sender()).getName());
                                lore.add("§8ID: " + mail.id());
                                meta.setLore(lore);
                                mailItem.setItemMeta(meta);
                            }
                            inv.setItem(slot++, mailItem);
                        }
                        
                        player.openInventory(inv);
                    });
                });
            });
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE || clickedItem.getType() == Material.PLAYER_HEAD) return;
        
        List<Mail> mails = viewingMails.get(player.getUniqueId());
        if (mails == null) return;
        
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        
        List<String> lore = meta.getLore();
        String idLine = lore.get(lore.size() - 1);
        
        if (idLine.startsWith("§8ID: ")) {
            try {
                int mailId = Integer.parseInt(idLine.substring(6));
                
                Mail targetMail = mails.stream().filter(m -> m.id() == mailId).findFirst().orElse(null);
                
                if (targetMail != null) {
                    if (player.getInventory().firstEmpty() == -1) {
                        player.sendMessage("§cYour inventory is full!");
                        return;
                    }
                    
                    databaseManager.markMailClaimed(mailId).thenAccept(v -> {
                        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("SwiftDelivery"), () -> {
                            player.getInventory().addItem(targetMail.item());
                            player.sendMessage("§aYou claimed an item from your mail!");
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                            openProfile(player);
                        });
                    }).exceptionally(ex -> {
                        player.sendMessage("§cFailed to claim mail.");
                        return null;
                    });
                }
            } catch (NumberFormatException ignored) {}
        }
    }
}
