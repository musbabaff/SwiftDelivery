package com.nettyforge.swiftdelivery.hook.item;

import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class OraxenProvider implements ItemProvider {
    private final Logger logger;
    private Method getItemByIdMethod;
    private Method buildMethod;

    public OraxenProvider(Logger logger) {
        this.logger = logger;
        try {
            Class<?> oraxenItemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            this.getItemByIdMethod = oraxenItemsClass.getMethod("getItemById", String.class);
        } catch (Exception e) {
            logger.warning("Oraxen hook failed to initialize: " + e.getMessage());
        }
    }

    @Override
    public ItemStack getItem(String id) {
        if (getItemByIdMethod == null) return null;
        try {
            Object builder = getItemByIdMethod.invoke(null, id);
            if (builder != null) {
                if (buildMethod == null) {
                    buildMethod = builder.getClass().getMethod("build");
                }
                return (ItemStack) buildMethod.invoke(builder);
            }
        } catch (Exception e) {
            logger.warning("Error getting Oraxen item (" + id + "): " + e.getMessage());
        }
        return null;
    }
}
