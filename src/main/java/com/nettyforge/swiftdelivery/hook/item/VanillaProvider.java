package com.nettyforge.swiftdelivery.hook.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class VanillaProvider implements ItemProvider {

    @Override
    public ItemStack getItem(String id) {
        if (id == null || id.isEmpty()) return new ItemStack(Material.PAPER);
        try {
            Material mat = Material.matchMaterial(id.toUpperCase());
            if (mat != null) {
                return new ItemStack(mat);
            }
        } catch (Exception ignored) {}
        
        return new ItemStack(Material.PAPER);
    }
}
