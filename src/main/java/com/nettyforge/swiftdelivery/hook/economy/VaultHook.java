package com.nettyforge.swiftdelivery.hook.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private Economy econ = null;

    public VaultHook() {
        if (!setupEconomy()) {
            Bukkit.getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", Bukkit.getPluginManager().getPlugin("SwiftDelivery").getDescription().getName()));
        }
    }

    private boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean isEnabled() {
        return econ != null;
    }

    public boolean hasAccount(Player player) {
        if (econ == null) return false;
        return econ.hasAccount(player);
    }

    public double getBalance(Player player) {
        if (econ == null) return 0.0;
        return econ.getBalance(player);
    }

    public boolean withdraw(Player player, double amount) {
        if (econ == null) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }
}
