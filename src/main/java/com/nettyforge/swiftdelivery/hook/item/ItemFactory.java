package com.nettyforge.swiftdelivery.hook.item;

import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ItemFactory {

    private final Map<String, ItemProvider> providers = new HashMap<>();
    private final ItemProvider fallbackProvider;
    private final Logger logger;

    public ItemFactory(Logger logger) {
        this.logger = logger;
        this.fallbackProvider = new VanillaProvider();
    }

    public void registerProvider(String prefix, ItemProvider provider) {
        providers.put(prefix.toUpperCase(), provider);
        logger.info("[ItemFactory] Registered custom item provider for prefix: " + prefix.toUpperCase());
    }

    /**
     * Parses a string like "NEXO:ruby_sword", "ORAXEN:amber", "ITEMSADDER:cool_hat",
     * or defaults to "DIAMOND" using the Vanilla provider.
     * 
     * @param rawInput The raw string from config
     * @return The constructed ItemStack
     */
    public ItemStack createItem(String rawInput) {
        if (rawInput == null || rawInput.isEmpty()) {
            return fallbackProvider.getItem("PAPER");
        }

        if (rawInput.contains(":")) {
            String[] parts = rawInput.split(":", 2);
            String prefix = parts[0].toUpperCase();
            String id = parts[1];

            ItemProvider provider = providers.get(prefix);
            if (provider != null) {
                ItemStack item = provider.getItem(id);
                if (item != null) {
                    return item;
                }
            } else {
                // If the user specified minecraft:diamond
                if (prefix.equals("MINECRAFT")) {
                    return fallbackProvider.getItem(id);
                }
                logger.warning("[ItemFactory] Unknown prefix or plugin not loaded: " + prefix);
            }
        }

        return fallbackProvider.getItem(rawInput); // Fallback to Vanilla
    }
}
