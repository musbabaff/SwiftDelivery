package com.nettyforge.swiftdelivery.hook.item;

import org.bukkit.inventory.ItemStack;

/**
 * Interface for providing ItemStacks from various custom item engines
 * (Nexo, Oraxen, ItemsAdder) or fallback Vanilla.
 */
public interface ItemProvider {
    
    /**
     * Gets an ItemStack by internal ID from the target engine.
     * @param id The ID (e.g., "ruby_sword", "DIAMOND")
     * @return The ItemStack, or null if not found
     */
    ItemStack getItem(String id);
}
