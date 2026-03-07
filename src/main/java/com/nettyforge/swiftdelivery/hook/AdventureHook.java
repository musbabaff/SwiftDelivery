package com.nettyforge.swiftdelivery.hook;

import com.nettyforge.swiftdelivery.SwiftDeliveryPlugin;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class AdventureHook {

    private BukkitAudiences adventure;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;

    public AdventureHook(SwiftDeliveryPlugin plugin) {
        this.adventure = BukkitAudiences.create(plugin);
        this.miniMessage = MiniMessage.miniMessage();
        
        // Allows converting old & color codes into Adventure Components
        this.legacySerializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexCharacter('#')
                .extractUrls()
                .build();
    }

    public void disable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }
    
    public BukkitAudiences getAdventure() {
        return this.adventure;
    }
    
    /**
     * Parses a string that may contain MiniMessage tags (<red>, <gradient>) OR legacy codes (&c, &#FF0000).
     */
    public Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        
        // Pre-process: if it has MiniMessage tags, use MiniMessage.
        // If it exclusively has legacy (&c), use legacy.
        // The best approach for full backward compatibility + modern tags:
        // Serialize the legacy string to a Component, then back to MiniMessage string, and parse all together?
        // Or simply convert & to section symbol, then use MiniMessage's legacy feature?
        // Actually, MiniMessage can work alongside legacy if we deserialize legacy first, then serialize to MM!
        
        // Let's keep it simple: if it contains '<' and '>', process with MiniMessage.
        // Then serialize to legacy for remaining & codes? No, Component is the final output.
        // We can parse legacy into Component.
        
        if (text.contains("<") && text.contains(">")) {
            // It uses MiniMessage tags. Let's process legacy codes inside it first by replacing &c with <red>, but that's complex.
            // For now, let's treat it as MiniMessage primary if tags are present.
            Component mmComp = miniMessage.deserialize(text);
            return mmComp;
        } else {
            // Treat as pure legacy
            return legacySerializer.deserialize(text);
        }
    }

    public void sendMessage(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        Component component = parse(message);
        adventure.player(player).sendMessage(component);
    }
    
    public void sendActionBar(Player player, String message) {
         if (message == null || message.isEmpty()) return;
         Component component = parse(message);
         adventure.player(player).sendActionBar(component);
    }
}
