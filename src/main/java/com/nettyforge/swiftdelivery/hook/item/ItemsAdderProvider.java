package com.nettyforge.swiftdelivery.hook.item;

import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class ItemsAdderProvider implements ItemProvider {
    private final Logger logger;
    private Method getInstanceMethod;
    private Method getItemStackMethod;

    public ItemsAdderProvider(Logger logger) {
        this.logger = logger;
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            this.getInstanceMethod = customStackClass.getMethod("getInstance", String.class);
            this.getItemStackMethod = customStackClass.getMethod("getItemStack");
        } catch (Exception e) {
            logger.warning("ItemsAdder hook failed to initialize: " + e.getMessage());
        }
    }

    @Override
    public ItemStack getItem(String id) {
        if (getInstanceMethod == null || getItemStackMethod == null) return null;
        try {
            Object customStack = getInstanceMethod.invoke(null, id);
            if (customStack != null) {
                return (ItemStack) getItemStackMethod.invoke(customStack);
            }
        } catch (Exception e) {
            logger.warning("Error getting ItemsAdder item (" + id + "): " + e.getMessage());
        }
        return null;
    }
}
