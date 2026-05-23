package com.example.rewards;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    
    private final RewardsPlugin plugin;
    private final ConfigManager configManager;
    private final RewardManager rewardManager;
    
    public ReloadCommand(RewardsPlugin plugin, ConfigManager configManager, RewardManager rewardManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.rewardManager = rewardManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rewards.reload")) {
            sender.sendMessage("§c你没有权限使用此命令！");
            return true;
        }
        
        configManager.reload();
        rewardManager.reload();
        
        sender.sendMessage("§a§l✓ §a插件配置已重新加载！");
        return true;
    }
}
