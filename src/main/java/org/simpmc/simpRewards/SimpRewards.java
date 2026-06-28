package org.simpmc.simpRewards;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.simpmc.simpRewards.command.DailyCommand;
import org.simpmc.simpRewards.command.RewardCommand;
import org.simpmc.simpRewards.config.ConfigManager;
import org.simpmc.simpRewards.data.PlayerRewardRepository;
import org.simpmc.simpRewards.data.YamlPlayerRewardRepository;
import org.simpmc.simpRewards.economy.VaultEconomyService;
import org.simpmc.simpRewards.gui.DailyCalendarMenuRenderer;
import org.simpmc.simpRewards.gui.RewardMenuRenderer;
import org.simpmc.simpRewards.listener.InventoryClickListener;
import org.simpmc.simpRewards.listener.PlayerConnectionListener;
import org.simpmc.simpRewards.reward.RewardService;
import org.simpmc.simpRewards.service.OnlineTimeService;
import org.simpmc.simpRewards.time.ResetService;
import org.simpmc.simpRewards.time.TimeService;

public final class SimpRewards extends JavaPlugin {
    private ConfigManager configManager;
    private PlayerRewardRepository playerRepository;
    private TimeService timeService;
    private ResetService resetService;
    private RewardService rewardService;
    private OnlineTimeService onlineTimeService;
    private RewardMenuRenderer rewardMenuRenderer;
    private DailyCalendarMenuRenderer dailyCalendarMenuRenderer;
    private VaultEconomyService economyService;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.timeService = new TimeService(configManager.getRewardConfig().zoneId());
        this.resetService = new ResetService(timeService, configManager.getRewardConfig());
        this.playerRepository = new YamlPlayerRewardRepository(this);
        this.economyService = new VaultEconomyService(this);
        this.economyService.hook();
        this.rewardService = new RewardService(this, configManager, playerRepository, resetService, timeService, economyService);
        this.onlineTimeService = new OnlineTimeService(this, playerRepository, resetService, timeService, configManager);
        this.rewardMenuRenderer = new RewardMenuRenderer(configManager, playerRepository, resetService, rewardService, timeService);
        this.dailyCalendarMenuRenderer = new DailyCalendarMenuRenderer(playerRepository, resetService, rewardService, timeService);

        RewardCommand rewardCommand = new RewardCommand(this, configManager, playerRepository, resetService, rewardMenuRenderer);
        PluginCommand command = getCommand("reward");
        if (command != null) {
            command.setExecutor(rewardCommand);
            command.setTabCompleter(rewardCommand);
        } else {
            getLogger().severe("Command 'reward' is missing from plugin.yml.");
        }
        PluginCommand dailyCommand = getCommand("daily");
        if (dailyCommand != null) {
            dailyCommand.setExecutor(new DailyCommand(configManager, dailyCalendarMenuRenderer));
        } else {
            getLogger().severe("Command 'daily' is missing from plugin.yml.");
        }

        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(configManager, playerRepository, resetService, onlineTimeService, rewardService),
                this
        );
        getServer().getPluginManager().registerEvents(new InventoryClickListener(rewardMenuRenderer), this);

        onlineTimeService.start();
        getServer().getOnlinePlayers().forEach(player -> {
            playerRepository.load(player.getUniqueId(), player.getName());
            onlineTimeService.startSession(player);
        });
    }

    @Override
    public void onDisable() {
        if (onlineTimeService != null) {
            onlineTimeService.stop();
        }
        if (playerRepository != null) {
            playerRepository.saveAll();
        }
    }

    public void reloadPlugin() {
        configManager.load();
        timeService.setZoneId(configManager.getRewardConfig().zoneId());
        resetService.setRewardConfig(configManager.getRewardConfig());
        economyService.hook();
        onlineTimeService.restart();
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public PlayerRewardRepository playerRepository() {
        return playerRepository;
    }

    public RewardMenuRenderer rewardMenuRenderer() {
        return rewardMenuRenderer;
    }
}
