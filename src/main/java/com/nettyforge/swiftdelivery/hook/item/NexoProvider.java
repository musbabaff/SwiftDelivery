package com.nettyforge.swiftdelivery.hook.item;

import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class NexoProvider implements ItemProvider {
    private final Logger logger;
    private Method itemFromIdMethod;
    private Method buildMethod;

    public NexoProvider(Logger logger) {
        this.logger = logger;
        try {
            Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            this.itemFromIdMethod = nexoItemsClass.getMethod("itemFromId", String.class);
        } catch (Exception e) {
            logger.warning("Nexo hook failed to initialize: " + e.getMessage());
        }
    }

    @Override
    public ItemStack getItem(String id) {
        if (itemFromIdMethod == null) return null;
        try {
            Object builder = itemFromIdMethod.invoke(null, id);
            if (builder != null) {
                if (buildMethod == null) {
                    buildMethod = builder.getClass().getMethod("build");
                }
                return (ItemStack) buildMethod.invoke(builder);
            }
        } catch (Exception e) {
            logger.warning("Error getting Nexo item (" + id + "): " + e.getMessage());
        }
        return null;
    }
}
