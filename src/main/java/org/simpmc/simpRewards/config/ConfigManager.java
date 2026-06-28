package org.simpmc.simpRewards.config;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.simpmc.simpRewards.reward.RewardAction;
import org.simpmc.simpRewards.reward.RewardActionType;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private RewardConfig rewardConfig;
    private GuiConfig guiConfig;
    private MessageConfig messageConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        saveResourceIfAbsent("gui.yml");
        saveResourceIfAbsent("messages.yml");
        plugin.reloadConfig();

        this.rewardConfig = loadRewardConfig(plugin.getConfig());
        this.guiConfig = loadGuiConfig(YamlConfiguration.loadConfiguration(plugin.getDataFolder().toPath().resolve("gui.yml").toFile()));
        this.messageConfig = loadMessageConfig(YamlConfiguration.loadConfiguration(plugin.getDataFolder().toPath().resolve("messages.yml").toFile()));
    }

    public RewardConfig getRewardConfig() {
        return rewardConfig;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    private void saveResourceIfAbsent(String resourcePath) {
        File file = plugin.getDataFolder().toPath().resolve(resourcePath).toFile();
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private RewardConfig loadRewardConfig(FileConfiguration config) {
        ZoneId zoneId = parseZoneId(config.getString("timezone", "server"));
        boolean autoClaimSign = config.getBoolean("sign.auto-claim-on-first-join", false);
        int signStreakCycle = Math.max(0, config.getInt("sign.streak-cycle", 7));
        boolean allowMakeupSign = config.getBoolean("sign.allow-makeup", false);
        List<RewardAction> signBaseRewards = loadRewardActions(config.getMapList("sign.rewards.base"));
        Map<Integer, List<RewardAction>> signStreakRewards = loadMilestoneRewards(config.getConfigurationSection("sign.rewards.streaks"));

        int onlineTickMinutes = Math.max(1, config.getInt("online.tick-minutes", 1));
        int dailyTargetMinutes = Math.max(1, config.getInt("daily-task.target-minutes", 30));
        List<RewardAction> dailyRewards = loadRewardActions(config.getMapList("daily-task.rewards"));

        DayOfWeek weeklyStart = parseDayOfWeek(config.getString("weekly-task.week-start", "MONDAY"));
        int weeklyTargetMinutes = Math.max(1, config.getInt("weekly-task.target-minutes", 300));
        List<RewardAction> weeklyRewards = loadRewardActions(config.getMapList("weekly-task.rewards"));

        Map<Integer, List<RewardAction>> totalOnlineMilestones = loadNestedMilestoneRewards(config.getConfigurationSection("total-online.milestones"));
        int activeThresholdMinutes = Math.max(1, config.getInt("active-streak.active-threshold-minutes", 30));
        Map<Integer, List<RewardAction>> activeStreakMilestones = loadNestedMilestoneRewards(config.getConfigurationSection("active-streak.milestones"));

        int autosaveMinutes = Math.max(1, config.getInt("storage.autosave-minutes", 5));

        return new RewardConfig(
                zoneId,
                autoClaimSign,
                signStreakCycle,
                allowMakeupSign,
                signBaseRewards,
                signStreakRewards,
                onlineTickMinutes,
                dailyTargetMinutes,
                dailyRewards,
                weeklyStart,
                weeklyTargetMinutes,
                weeklyRewards,
                totalOnlineMilestones,
                activeThresholdMinutes,
                activeStreakMilestones,
                autosaveMinutes
        );
    }

    private GuiConfig loadGuiConfig(FileConfiguration config) {
        String title = config.getString("menu.title", "&6奖励中心");
        int size = Math.max(9, Math.min(54, config.getInt("menu.size", 36)));
        if (size % 9 != 0) {
            size = 36;
        }

        Map<String, GuiConfig.GuiItemConfig> items = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection itemSection = section.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                Map<String, GuiConfig.GuiItemStateConfig> states = new HashMap<>();
                for (String state : List.of("locked", "claimable", "claimed")) {
                    ConfigurationSection stateSection = itemSection.getConfigurationSection(state);
                    if (stateSection != null) {
                        states.put(state, new GuiConfig.GuiItemStateConfig(
                                stateSection.getString("material", "STONE"),
                                stateSection.getString("name", ""),
                                stateSection.getStringList("lore")
                        ));
                    }
                }
                items.put(key, new GuiConfig.GuiItemConfig(
                        itemSection.getInt("slot", -1),
                        itemSection.getString("material", "STONE"),
                        itemSection.getString("name", ""),
                        itemSection.getStringList("lore"),
                        states,
                        itemSection.getIntegerList("slots")
                ));
            }
        }
        return new GuiConfig(title, size, items);
    }

    private MessageConfig loadMessageConfig(FileConfiguration config) {
        String prefix = config.getString("prefix", "");
        Map<String, String> messages = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (!"prefix".equals(key) && config.isString(key)) {
                messages.put(key, config.getString(key, key));
            }
        }
        return new MessageConfig(prefix, messages);
    }

    private ZoneId parseZoneId(String value) {
        if (value == null || value.equalsIgnoreCase("server")) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(value);
        } catch (Exception exception) {
            plugin.getLogger().warning("Invalid timezone '" + value + "', using server timezone.");
            return ZoneId.systemDefault();
        }
    }

    private DayOfWeek parseDayOfWeek(String value) {
        try {
            return DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            plugin.getLogger().warning("Invalid weekly-task.week-start '" + value + "', using MONDAY.");
            return DayOfWeek.MONDAY;
        }
    }

    private Map<Integer, List<RewardAction>> loadMilestoneRewards(ConfigurationSection section) {
        Map<Integer, List<RewardAction>> rewards = new LinkedHashMap<>();
        if (section == null) {
            return rewards;
        }
        for (String key : section.getKeys(false)) {
            Integer milestone = parsePositiveInteger(key);
            if (milestone == null) {
                plugin.getLogger().warning("Invalid milestone key: " + key);
                continue;
            }
            rewards.put(milestone, loadRewardActions(section.getMapList(key)));
        }
        return rewards;
    }

    private Map<Integer, List<RewardAction>> loadNestedMilestoneRewards(ConfigurationSection section) {
        Map<Integer, List<RewardAction>> rewards = new LinkedHashMap<>();
        if (section == null) {
            return rewards;
        }
        for (String key : section.getKeys(false)) {
            Integer milestone = parsePositiveInteger(key);
            if (milestone == null) {
                plugin.getLogger().warning("Invalid milestone key: " + key);
                continue;
            }
            rewards.put(milestone, loadRewardActions(section.getMapList(key + ".rewards")));
        }
        return rewards;
    }

    private List<RewardAction> loadRewardActions(List<Map<?, ?>> maps) {
        List<RewardAction> actions = new ArrayList<>();
        for (Map<?, ?> raw : maps) {
            String typeName = stringValue(raw.get("type"), "item").toUpperCase(Locale.ROOT);
            RewardActionType type;
            try {
                type = RewardActionType.valueOf(typeName);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Unknown reward action type: " + typeName);
                continue;
            }
            String material = stringValue(raw.get("material"), "STONE");
            double amount = doubleValue(raw.get("amount"), 1);
            String name = stringValue(raw.get("name"), "");
            List<String> lore = stringListValue(raw.get("lore"));
            String command = stringValue(raw.get("command"), "");
            actions.add(new RewardAction(type, material, Math.max(0, amount), name, lore, command));
        }
        return actions;
    }

    private Integer parsePositiveInteger(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception exception) {
            return fallback;
        }
    }

    private double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception exception) {
            return fallback;
        }
    }

    private List<String> stringListValue(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> strings = new ArrayList<>();
        for (Object item : list) {
            strings.add(String.valueOf(item));
        }
        return strings;
    }
}
