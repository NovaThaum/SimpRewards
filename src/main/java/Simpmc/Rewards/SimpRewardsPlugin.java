package Simpmc.Rewards;

import Simpmc.Rewards.command.RewardCommand;
import Simpmc.Rewards.data.DataManager;
import Simpmc.Rewards.listener.InventoryListener;
import Simpmc.Rewards.listener.JoinListener;
import Simpmc.Rewards.listener.QuitListener;
import Simpmc.Rewards.task.OnlineTracker;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpRewardsPlugin extends JavaPlugin {

    private static SimpRewardsPlugin instance;

    private DataManager dataManager;
    private OnlineTracker onlineTracker;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        this.dataManager = new DataManager(this);

        dataManager.init();

        /*
         * 注册监听器
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
         * 注册命令
         */

        getCommand("reward").setExecutor(
                new RewardCommand()
        );

        /*
         * 在线统计任务
         */

        this.onlineTracker = new OnlineTracker(this);

        onlineTracker.start();

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
