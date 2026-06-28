package org.simpmc.simpRewards.data;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlPlayerRewardRepository implements PlayerRewardRepository {
    private final JavaPlugin plugin;
    private final File playerDirectory;
    private final Map<UUID, PlayerRewardState> loadedStates = new HashMap<>();

    public YamlPlayerRewardRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerDirectory = new File(plugin.getDataFolder(), "data/players");
        if (!playerDirectory.exists() && !playerDirectory.mkdirs()) {
            plugin.getLogger().warning("Unable to create player data directory: " + playerDirectory.getAbsolutePath());
        }
    }

    @Override
    public PlayerRewardState load(UUID uuid, String name) {
        PlayerRewardState loaded = loadedStates.get(uuid);
        if (loaded != null) {
            loaded.setName(name);
            return loaded;
        }

        File file = file(uuid);
        PlayerRewardState state = new PlayerRewardState(uuid, name);
        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            state.setName(config.getString("name", name));
            state.setTotalOnlineMinutes(config.getLong("total-online-minutes", 0));
            state.setDailyOnlineMinutes(config.getInt("daily-online-minutes", 0));
            state.setWeeklyOnlineMinutes(config.getInt("weekly-online-minutes", 0));
            state.setLastSeenDate(parseDate(config.getString("last-seen-date")));
            state.setLastDailyResetDate(parseDate(config.getString("last-daily-reset-date")));
            state.setLastWeeklyResetWeek(config.getString("last-weekly-reset-week", ""));
            state.setSignedDate(parseDate(config.getString("signed-date")));
            config.getStringList("signed-dates").stream()
                    .map(this::parseDate)
                    .forEach(state::addSignedDate);
            state.setSignStreak(config.getInt("sign-streak", 0));
            state.setActiveStreak(config.getInt("active-streak", 0));
            state.setLastActiveDate(parseDate(config.getString("last-active-date")));
            state.claimedTotalMilestones().addAll(config.getIntegerList("claimed-total-milestones"));
            state.setClaimedDailyDate(parseDate(config.getString("claimed-daily-date")));
            state.setClaimedWeeklyWeek(config.getString("claimed-weekly-week", ""));
            state.claimedActiveStreakMilestones().addAll(config.getIntegerList("claimed-active-streak-milestones"));
        }

        loadedStates.put(uuid, state);
        return state;
    }

    @Override
    public Optional<PlayerRewardState> findLoaded(UUID uuid) {
        return Optional.ofNullable(loadedStates.get(uuid));
    }

    @Override
    public Collection<PlayerRewardState> loadedStates() {
        return loadedStates.values();
    }

    @Override
    public void save(PlayerRewardState state) {
        File file = file(state.uuid());
        FileConfiguration config = new YamlConfiguration();
        config.set("uuid", state.uuid().toString());
        config.set("name", state.name());
        config.set("total-online-minutes", state.totalOnlineMinutes());
        config.set("daily-online-minutes", state.dailyOnlineMinutes());
        config.set("weekly-online-minutes", state.weeklyOnlineMinutes());
        config.set("last-seen-date", formatDate(state.lastSeenDate()));
        config.set("last-daily-reset-date", formatDate(state.lastDailyResetDate()));
        config.set("last-weekly-reset-week", state.lastWeeklyResetWeek());
        config.set("signed-date", formatDate(state.signedDate()));
        config.set("signed-dates", state.signedDates().stream().sorted().map(this::formatDate).toList());
        config.set("sign-streak", state.signStreak());
        config.set("active-streak", state.activeStreak());
        config.set("last-active-date", formatDate(state.lastActiveDate()));
        config.set("claimed-total-milestones", state.claimedTotalMilestones().stream().sorted().toList());
        config.set("claimed-daily-date", formatDate(state.claimedDailyDate()));
        config.set("claimed-weekly-week", state.claimedWeeklyWeek());
        config.set("claimed-active-streak-milestones", state.claimedActiveStreakMilestones().stream().sorted().toList());
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Unable to save player data for " + state.uuid() + ": " + exception.getMessage());
        }
    }

    @Override
    public void saveAll() {
        for (PlayerRewardState state : loadedStates.values()) {
            save(state);
        }
    }

    @Override
    public void unload(UUID uuid) {
        PlayerRewardState state = loadedStates.remove(uuid);
        if (state != null) {
            save(state);
        }
    }

    private File file(UUID uuid) {
        return new File(playerDirectory, uuid + ".yml");
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception exception) {
            return null;
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }
}
