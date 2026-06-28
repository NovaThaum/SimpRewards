package org.simpmc.simpRewards.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultEconomyService {
    private final JavaPlugin plugin;
    private Economy economy;

    public VaultEconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void hook() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            plugin.getLogger().info("Vault not found. Money rewards are disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> provider = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);
        if (provider == null) {
            economy = null;
            plugin.getLogger().warning("Vault is installed, but no economy provider is registered.");
            return;
        }
        economy = provider.getProvider();
        plugin.getLogger().info("Hooked Vault economy provider: " + economy.getName());
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null || amount <= 0) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }
}
