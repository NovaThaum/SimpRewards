package Simpmc.Rewards.config;

import Simpmc.Rewards.SimpRewardsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private static File rewardsFile;
    private static YamlConfiguration rewards;

    private static File menuFile;
    private static YamlConfiguration menu;

    public static void load() {

        rewardsFile = new File(
                SimpRewardsPlugin.getInstance().getDataFolder(),
                "rewards.yml"
        );

        menuFile = new File(
                SimpRewardsPlugin.getInstance().getDataFolder(),
                "menu.yml"
        );

        if (!rewardsFile.exists()) {
            SimpRewardsPlugin.getInstance()
                    .saveResource("rewards.yml", false);
        }

        if (!menuFile.exists()) {
            SimpRewardsPlugin.getInstance()
                    .saveResource("menu.yml", false);
        }

        rewards = YamlConfiguration.loadConfiguration(rewardsFile);

        menu = YamlConfiguration.loadConfiguration(menuFile);
    }

    public static YamlConfiguration rewards() {
        return rewards;
    }

    public static YamlConfiguration menu() {
        return menu;
    }
}
