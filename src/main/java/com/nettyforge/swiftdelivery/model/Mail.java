package com.nettyforge.swiftdelivery.model;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public record Mail(
    int id,
    UUID sender,
    UUID receiver,
    ItemStack item,
    long sentAt,
    boolean claimed
) {}
