package Simpmc.Rewards.util;

import Simpmc.Rewards.SimpRewardsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FoliaUtil {

    public static void runPlayer(Player player, Runnable runnable) {

        SimpRewardsPlugin.getInstance()
                .getServer()
                .getRegionScheduler()
                .execute(
                        SimpRewardsPlugin.getInstance(),
                        player.getLocation(),
                        runnable
                );
    }

    public static void runLocation(Location location, Runnable runnable) {

        SimpRewardsPlugin.getInstance()
                .getServer()
                .getRegionScheduler()
                .execute(
                        SimpRewardsPlugin.getInstance(),
                        location,
                        runnable
                );
    }

    public static void async(Runnable runnable) {

        SimpRewardsPlugin.getInstance()
                .getServer()
                .getAsyncScheduler()
                .runNow(
                        SimpRewardsPlugin.getInstance(),
                        task -> runnable.run()
                );
    }
}
