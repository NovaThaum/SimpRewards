package org.simpmc.simpRewards.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.simpmc.simpRewards.config.ConfigManager;
import org.simpmc.simpRewards.config.RewardConfig;
import org.simpmc.simpRewards.data.PlayerRewardRepository;
import org.simpmc.simpRewards.data.PlayerRewardState;
import org.simpmc.simpRewards.time.ResetService;
import org.simpmc.simpRewards.time.TimeService;

public final class OnlineTimeService {
    private final JavaPlugin plugin;
    private final PlayerRewardRepository repository;
    private final ResetService resetService;
    private final TimeService timeService;
    private final ConfigManager configManager;
    private final Map<UUID, Instant> lastTickInstants = new HashMap<>();
    private BukkitTask tickerTask;
    private BukkitTask autosaveTask;

    public OnlineTimeService(
            JavaPlugin plugin,
            PlayerRewardRepository repository,
            ResetService resetService,
            TimeService timeService,
            ConfigManager configManager
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.resetService = resetService;
        this.timeService = timeService;
        this.configManager = configManager;
    }

    public void start() {
        stopTasksOnly();
        long period = Math.max(1, config().onlineTickMinutes()) * 60L * 20L;
        this.tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickOnlinePlayers, period, period);
        long autosavePeriod = Math.max(1, config().autosaveMinutes()) * 60L * 20L;
        this.autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, repository::saveAll, autosavePeriod, autosavePeriod);
    }

    public void restart() {
        start();
    }

    public void stop() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopSession(player);
        }
        stopTasksOnly();
        repository.saveAll();
    }

    public void startSession(Player player) {
        PlayerRewardState state = repository.load(player.getUniqueId(), player.getName());
        resetService.applyResets(state);
        lastTickInstants.put(player.getUniqueId(), Instant.now());
        repository.save(state);
    }

    public void stopSession(Player player) {
        PlayerRewardState state = repository.load(player.getUniqueId(), player.getName());
        settleElapsed(player, state);
        repository.save(state);
        repository.unload(player.getUniqueId());
    }

    public void tickOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerRewardState state = repository.load(player.getUniqueId(), player.getName());
            resetService.applyResets(state);
            addConfiguredMinutes(state);
            lastTickInstants.put(player.getUniqueId(), Instant.now());
            repository.save(state);
        }
    }

    private void addConfiguredMinutes(PlayerRewardState state) {
        state.addOnlineMinutes(config().onlineTickMinutes());
        updateActiveStreak(state);
    }

    private void settleElapsed(Player player, PlayerRewardState state) {
        Instant now = Instant.now();
        Instant lastTick = lastTickInstants.remove(player.getUniqueId());
        if (lastTick == null) {
            return;
        }
        long elapsedSeconds = Math.max(0, now.getEpochSecond() - lastTick.getEpochSecond());
        int minutes = (int) (elapsedSeconds / 60L);
        if (minutes <= 0) {
            return;
        }
        resetService.applyResets(state);
        state.addOnlineMinutes(minutes);
        updateActiveStreak(state);
    }

    private void updateActiveStreak(PlayerRewardState state) {
        LocalDate today = timeService.today();
        if (state.dailyOnlineMinutes() < config().activeThresholdMinutes()) {
            return;
        }
        if (today.equals(state.lastActiveDate())) {
            return;
        }
        LocalDate yesterday = today.minusDays(1);
        if (yesterday.equals(state.lastActiveDate())) {
            state.setActiveStreak(state.activeStreak() + 1);
        } else {
            state.setActiveStreak(1);
        }
        state.setLastActiveDate(today);
    }

    private void stopTasksOnly() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    private RewardConfig config() {
        return configManager.getRewardConfig();
    }
}
