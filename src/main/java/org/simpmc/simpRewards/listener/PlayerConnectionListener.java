package org.simpmc.simpRewards.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.simpmc.simpRewards.config.ConfigManager;
import org.simpmc.simpRewards.data.PlayerRewardRepository;
import org.simpmc.simpRewards.data.PlayerRewardState;
import org.simpmc.simpRewards.reward.RewardService;
import org.simpmc.simpRewards.reward.RewardType;
import org.simpmc.simpRewards.service.OnlineTimeService;
import org.simpmc.simpRewards.time.ResetService;

public final class PlayerConnectionListener implements Listener {
    private final ConfigManager configManager;
    private final PlayerRewardRepository repository;
    private final ResetService resetService;
    private final OnlineTimeService onlineTimeService;
    private final RewardService rewardService;

    public PlayerConnectionListener(
            ConfigManager configManager,
            PlayerRewardRepository repository,
            ResetService resetService,
            OnlineTimeService onlineTimeService,
            RewardService rewardService
    ) {
        this.configManager = configManager;
        this.repository = repository;
        this.resetService = resetService;
        this.onlineTimeService = onlineTimeService;
        this.rewardService = rewardService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerRewardState state = repository.load(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        resetService.applyResets(state);
        onlineTimeService.startSession(event.getPlayer());
        if (configManager.getRewardConfig().autoClaimSignOnFirstJoin() && !rewardService.isDailySignClaimed(state)) {
            rewardService.claim(event.getPlayer(), RewardType.DAILY_SIGN);
        } else {
            repository.save(state);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        onlineTimeService.stopSession(event.getPlayer());
    }
}
