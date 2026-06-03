package Simpmc.Rewards;

import Simpmc.Rewards.command.ReloadCommand;
import Simpmc.Rewards.command.RewardCommand;
import Simpmc.Rewards.config.ConfigManager;
import Simpmc.Rewards.data.DataManager;
import Simpmc.Rewards.listener.InventoryListener;
import Simpmc.Rewards.listener.JoinListener;
import Simpmc.Rewards.listener.QuitListener;
import Simpmc.Rewards.task.DailyResetTask;
import Simpmc.Rewards.task.OnlineTracker;
import Simpmc.Rewards.task.WeeklyResetTask;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpRewardsPlugin extends JavaPlugin {

    private static SimpRewardsPlugin instance;

    private DataManager dataManager;

    private OnlineTracker onlineTracker;
    private DailyResetTask dailyResetTask;
    private WeeklyResetTask weeklyResetTask;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        ConfigManager.load();

        this.dataManager = new DataManager(this);

        dataManager.init();

        /*
         * 监听器
         */

        getServer().getPluginManager().registerEvents(
                new JoinListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new QuitListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new InventoryListener(this),
                this
        );

        /*
         * 命令
         */

        getCommand("reward").setExecutor(
                new RewardCommand()
        );

        getCommand("simprewardsreload").setExecutor(
                new ReloadCommand()
        );

        /*
         * 任务
         */

        this.onlineTracker = new OnlineTracker(this);

        onlineTracker.start();

        this.dailyResetTask = new DailyResetTask(this);

        dailyResetTask.start();

        this.weeklyResetTask = new WeeklyResetTask(this);

        weeklyResetTask.start();

        getLogger().info("SimpRewards enabled.");
    }

    @Override
    public void onDisable() {

        dataManager.saveAll();

        getLogger().info("SimpRewards disabled.");
    }

    public static SimpRewardsPlugin getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }
}
