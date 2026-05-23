package com.example.rewards;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

public class RewardManager {
    
    private final RewardsPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerCacheManager cacheManager;
    
    public RewardManager(RewardsPlugin plugin, ConfigManager configManager, PlayerCacheManager cacheManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cacheManager = cacheManager;
    }
    
    public void grantMilestoneReward(Player player, String milestoneId, int seconds) {
        PlayerCache cache = cacheManager.getPlayerCache(player.getUniqueId());
        if (cache == null) return;
        
        PlayerRewardsData data = cache.getData();
        if (data.getClaimedMilestones().contains(milestoneId)) {
            player.sendMessage("§c你已经领取过这个里程碑奖励了！");
            return;
        }
        
        // 数据库原子操作防止重复领取
        try {
            if (plugin.getRewardsDAO().claimMilestone(player.getUniqueId().toString(), milestoneId)) {
                List<String> rewards = configManager.getMilestoneRewards(seconds);
                giveRewards(player, rewards, "累计在线 " + formatTime(seconds) + " 里程碑奖励");
                data.getClaimedMilestones().add(milestoneId);
                cache.markDirty();
                player.sendMessage("§a§l✓ §a成功领取累计在线 " + formatTime(seconds) + " 里程碑奖励！");
            } else {
                player.sendMessage("§c这个奖励已经被其他服务器领取了，下次请更快一点！");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to claim milestone: " + e.getMessage());
            player.sendMessage("§c领取奖励时发生错误，请重试。");
        }
    }
    
    public void grantDailyTaskReward(Player player, String taskId, int seconds) {
        PlayerCache cache = cacheManager.getPlayerCache(player.getUniqueId());
        if (cache == null) return;
        
        PlayerRewardsData data = cache.getData();
        String today = java.time.LocalDate.now().toString();
        
        if (data.getDailyClaimedDate() != null && data.getDailyClaimedDate().equals(today)) {
            player.sendMessage("§c你今天已经领取过每日任务奖励了！");
            return;
        }
        
        // 检查是否达到目标
        if (data.getDailySeconds() < seconds) {
            player.sendMessage("§c你还没有完成这个每日任务！");
            return;
        }
        
        // 原子更新
        try {
            List<String> rewards = configManager.getDailyTaskRewards(seconds);
            giveRewards(player, rewards, "每日在线 " + formatTime(seconds) + " 任务奖励");
            
            data.setDailyClaimedDate(today);
            cache.markDirty();
            player.sendMessage("§a§l✓ §a成功领取每日在线 " + formatTime(seconds) + " 任务奖励！");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to grant daily task: " + e.getMessage());
            player.sendMessage("§c领取奖励时发生错误，请重试。");
        }
    }
    
    public void grantWeeklyTaskReward(Player player, String taskId, int seconds) {
        PlayerCache cache = cacheManager.getPlayerCache(player.getUniqueId());
        if (cache == null) return;
        
        PlayerRewardsData data = cache.getData();
        String currentWeek = java.time.LocalDate.now().get(YearWeekField.INSTANCE);
        
        if (data.getWeeklyClaimedWeek() != null && data.getWeeklyClaimedWeek().equals(currentWeek)) {
            player.sendMessage("§c你本周已经领取过每周任务奖励了！");
            return;
        }
        
        if (data.getWeeklySeconds() < seconds) {
            player.sendMessage("§c你还没有完成这个每周任务！");
            return;
        }
        
        try {
            List<String> rewards = configManager.getWeeklyTaskRewards(seconds);
            giveRewards(player, rewards, "每周在线 " + formatTime(seconds) + " 任务奖励");
            
            data.setWeeklyClaimedWeek(currentWeek);
            cache.markDirty();
            player.sendMessage("§a§l✓ §a成功领取每周在线 " + formatTime(seconds) + " 任务奖励！");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to grant weekly task: " + e.getMessage());
            player.sendMessage("§c领取奖励时发生错误，请重试。");
        }
    }
    
    public void grantConsecutiveReward(Player player, String consecutiveId, int days) {
        PlayerCache cache = cacheManager.getPlayerCache(player.getUniqueId());
        if (cache == null) return;
        
        PlayerRewardsData data = cache.getData();
        if (data.getClaimedConsecutive().contains(consecutiveId)) {
            player.sendMessage("§c你已经领取过这个连续活跃奖励了！");
            return;
        }
        
        try {
            if (plugin.getRewardsDAO().claimConsecutive(player.getUniqueId().toString(), consecutiveId)) {
                List<String> rewards = configManager.getConsecutiveDayRewards(days);
                giveRewards(player, rewards, "连续活跃 " + days + " 天奖励");
                data.getClaimedConsecutive().add(consecutiveId);
                cache.markDirty();
                player.sendMessage("§a§l✓ §a成功领取连续活跃 " + days + " 天奖励！");
            } else {
                player.sendMessage("§c这个奖励已经被其他服务器领取了，下次请更快一点！");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to claim consecutive reward: " + e.getMessage());
            player.sendMessage("§c领取奖励时发生错误，请重试。");
        }
    }
    
    public void grantSignInReward(Player player) {
        PlayerCache cache = cacheManager.getPlayerCache(player.getUniqueId());
        if (cache == null) return;
        
        PlayerRewardsData data = cache.getData();
        String today = java.time.LocalDate.now().toString();
        String yesterday = java.time.LocalDate.now().minusDays(1).toString();
        
        // 检查是否今天已经签到
        if (data.getLastSignTime() != 0) {
            java.time.LocalDateTime lastSign = java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(data.getLastSignTime()), 
                java.time.ZoneId.systemDefault()
            );
            if (lastSign.toLocalDate().equals(java.time.LocalDate.now())) {
                player.sendMessage("§c你今天已经签到过了！");
                return;
            }
        }
        
        // 更新连签天数
        if (data.getLastSignTime() != 0) {
            java.time.LocalDateTime lastSign = java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(data.getLastSignTime()), 
                java.time.ZoneId.systemDefault()
            );
            if (lastSign.toLocalDate().equals(java.time.LocalDate.parse(yesterday))) {
                data.setSignStreak(data.getSignStreak() + 1);
            } else {
                data.setSignStreak(1);
            }
        } else {
            data.setSignStreak(1);
        }
        
        // 发放基础签到奖励
        List<String> baseRewards = configManager.getDailySignInRewards();
        giveRewards(player, baseRewards, "每日签到奖励");
        
        // 检查连签奖励
        for (int streak : new int[]{3, 7, 14, 30}) {
            if (data.getSignStreak() >= streak) {
                List<String> streakRewards = configManager.getStreakRewards(streak);
                if (!streakRewards.isEmpty()) {
                    giveRewards(player, streakRewards, "连续签到 " + streak + " 天奖励");
                }
            }
        }
        
        data.setLastSignTime(System.currentTimeMillis());
        cache.markDirty();
        
        player.sendMessage("§a§l✓ §a签到成功！当前连签天数: §e" + data.getSignStreak() + "§a天");
    }
    
    private void giveRewards(Player player, List<String> rewards, String reason) {
        if (rewards == null || rewards.isEmpty()) return;
        
        for (String reward : rewards) {
            String[] parts = reward.split(":");
            if (parts.length < 2) continue;
            
            String type = parts[0].toLowerCase();
            try {
                if (type.equals("money")) {
                    int amount = Integer.parseInt(parts[1]);
                    // 这里假设使用 Vault 经济 API
                    if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = 
                            plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                        if (rsp != null) {
                            net.milkbowl.vault.economy.Economy economy = rsp.getProvider();
                            economy.depositPlayer(player, amount);
                            player.sendMessage("§a+ §6$" + amount + " §7(" + reason + ")");
                        }
                    }
                } else if (type.equals("item")) {
                    if (parts.length >= 3) {
                        org.bukkit.Material material = org.bukkit.Material.valueOf(parts[1]);
                        int amount = parts.length >= 4 ? Integer.parseInt(parts[3]) : 1;
                        ItemStack item = new ItemStack(material, amount);
                        
                        // 尝试直接给玩家
                        if (!player.getInventory().addItem(item).isEmpty()) {
                            // 背包满了，掉落物品
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                            player.sendMessage("§e你的背包满了，奖励已掉落！");
                        }
                        player.sendMessage("§a+ §e" + amount + "x " + material.name().replace("_", " ") + " §7(" + reason + ")");
                    }
                } else if (type.equals("command")) {
                    String command = String.join(":", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                        command.replace("%player%", player.getName()));
                    player.sendMessage("§a已执行命令奖励: §e" + command);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to give reward '" + reward + "': " + e.getMessage());
            }
        }
    }
    
    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        }
        return minutes + "分钟";
    }
}
