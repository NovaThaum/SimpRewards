package com.example.rewards;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public class PlayerScheduler {
    
    private final RewardsPlugin plugin;
    private final Player player;
    private final PlayerCache cache;
    private BukkitTask task;
    
    public PlayerScheduler(RewardsPlugin plugin, Player player, PlayerCache cache) {
        this.plugin = plugin;
        this.player = player;
        this.cache = cache;
    }
    
    public void start() {
        // 每60秒更新一次在线时间
        task = player.getScheduler().runAtFixedRate(plugin, executor -> {
            if (!player.isOnline()) {
                executor.cancel();
                plugin.getCacheManager().unloadPlayerData(player);
                return;
            }
            
            updatePlayerTime();
            checkMilestones();
            
        }, null, 60, 60, TimeUnit.SECONDS);
    }
    
    private void updatePlayerTime() {
        PlayerRewardsData data = cache.getData();
        long currentTime = System.currentTimeMillis();
        String currentDate = java.time.LocalDate.now().toString();
        String currentWeek = java.time.LocalDate.now().get(YearWeekField.INSTANCE);
        
        // 更新累计时间
        data.setTotalSeconds(data.getTotalSeconds() + 60);
        
        // 检查是否需要重置每日/每周计数
        if (!currentDate.equals(data.getLastActiveDate())) {
            // 重置每日计数
            if (!currentDate.equals(data.getDailyClaimedDate())) {
                data.setDailySeconds(0);
            }
            data.setDailySeconds(data.getDailySeconds() + 60);
            
            // 重置每周计数
            if (!currentWeek.equals(data.getWeeklyClaimedWeek())) {
                data.setWeeklySeconds(0);
            }
            data.setWeeklySeconds(data.getWeeklySeconds() + 60);
            
            // 更新连续活跃天数
            if (currentDate.equals(getNextDay(data.getLastActiveDate()))) {
                data.setConsecutiveDays(data.getConsecutiveDays() + 1);
            } else {
                data.setConsecutiveDays(1);
            }
            
            data.setLastActiveDate(currentDate);
            cache.markDirty();
        } else {
            // 同一天内
            data.setDailySeconds(data.getDailySeconds() + 60);
            data.setWeeklySeconds(data.getWeeklySeconds() + 60);
            cache.markDirty();
        }
    }
    
    private String getNextDay(String dateStr) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            return date.plusDays(1).toString();
        } catch (Exception e) {
            return dateStr;
        }
    }
    
    private void checkMilestones() {
        PlayerRewardsData data = cache.getData();
        ConfigManager config = plugin.getConfigManager();
        
        // 检查累计里程碑
        for (int milestone : new int[]{3600, 7200, 14400, 28800, 86400}) {
            if (data.getTotalSeconds() >= milestone) {
                String milestoneId = "milestone_" + milestone;
                if (!data.getClaimedMilestones().contains(milestoneId)) {
                    // 触发里程碑奖励
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getRewardManager().grantMilestoneReward(player, milestoneId, milestone);
                    });
                }
            }
        }
        
        // 检查每日任务
        for (int target : new int[]{3600, 7200}) {
            if (data.getDailySeconds() >= target) {
                String taskId = "daily_" + target;
                if (data.getDailyClaimedDate() == null || 
                    !data.getDailyClaimedDate().equals(java.time.LocalDate.now().toString())) {
                    // 触发每日任务完成
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getRewardManager().grantDailyTaskReward(player, taskId, target);
                    });
                }
            }
        }
        
        // 检查每周任务
        for (int target : new int[]{25200, 50400}) { // 7 hours, 14 hours
            if (data.getWeeklySeconds() >= target) {
                String taskId = "weekly_" + target;
                String currentWeek = java.time.LocalDate.now().get(YearWeekField.INSTANCE);
                if (data.getWeeklyClaimedWeek() == null || 
                    !data.getWeeklyClaimedWeek().equals(currentWeek)) {
                    // 触发每周任务完成
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getRewardManager().grantWeeklyTaskReward(player, taskId, target);
                    });
                }
            }
        }
        
        // 检查连续活跃天数奖励
        for (int days : new int[]{7, 14, 30}) {
            if (data.getConsecutiveDays() >= days) {
                String consecutiveId = "consecutive_" + days;
                if (!data.getClaimedConsecutive().contains(consecutiveId)) {
                    // 触发连续活跃奖励
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getRewardManager().grantConsecutiveReward(player, consecutiveId, days);
                    });
                }
            }
        }
    }
    
    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }
}

// 自定义字段用于获取周数
class YearWeekField implements java.time.temporal.TemporalField {
    static final YearWeekField INSTANCE = new YearWeekField();
    
    @Override
    public java.time.temporal.TemporalUnit getBaseUnit() {
        return java.time.temporal.ChronoUnit.WEEKS;
    }
    
    @Override
    public java.time.temporal.TemporalUnit getRangeUnit() {
        return java.time.temporal.ChronoUnit.YEARS;
    }
    
    @Override
    public java.time.temporal.ValueRange range() {
        return java.time.temporal.ValueRange.of(1, 53);
    }
    
    @Override
    public boolean isDateBased() {
        return true;
    }
    
    @Override
    public boolean isTimeBased() {
        return false;
    }
    
    @Override
    public boolean isSupportedBy(java.time.temporal.TemporalAccessor temporal) {
        return temporal.isSupported(java.time.temporal.ChronoField.YEAR);
    }
    
    @Override
    public long getFrom(java.time.temporal.TemporalAccessor temporal) {
        java.time.LocalDate date = java.time.LocalDate.from(temporal);
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault());
        int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
        int year = date.get(weekFields.weekBasedYear());
        return Long.parseLong(year + "" + (weekNumber < 10 ? "0" + weekNumber : weekNumber));
    }
    
    @Override
    public <R extends java.time.temporal.Temporal> R adjustInto(R temporal, long newValue) {
        throw new UnsupportedOperationException();
    }
}
