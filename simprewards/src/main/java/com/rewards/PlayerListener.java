package com.example.rewards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    
    private final RewardsPlugin plugin;
    private final PlayerCacheManager cacheManager;
    private final RewardGUI rewardGUI;
    private final RewardManager rewardManager;
    
    public PlayerListener(RewardsPlugin plugin, PlayerCacheManager cacheManager, RewardGUI rewardGUI, RewardManager rewardManager) {
        this.plugin = plugin;
        this.cacheManager = cacheManager;
        this.rewardGUI = rewardGUI;
        this.rewardManager = rewardManager;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        cacheManager.loadPlayerData(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cacheManager.unloadPlayerData(event.getPlayer());
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player player) {
            Inventory inventory = event.getInventory();
            if (inventory.getTitle().equals("§6§l奖励中心")) {
                event.setCancelled(true); // 防止玩家移动物品
                
                int slot = event.getSlot();
                if (slot >= 0 && slot < 54) {
                    rewardGUI.handleGUIClick(player, slot);
                }
            }
        }
    }
}
