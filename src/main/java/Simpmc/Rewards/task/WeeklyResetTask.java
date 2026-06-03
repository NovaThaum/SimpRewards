package Simpmc.Rewards.task;

import Simpmc.Rewards.SimpRewardsPlugin;
import Simpmc.Rewards.data.PlayerData;

public class WeeklyResetTask {

    private final SimpRewardsPlugin plugin;

    public WeeklyResetTask(SimpRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {

        plugin.getServer().getAsyncScheduler()
                .runAtFixedRate(
                        plugin,
                        task -> {

                            for (PlayerData data :
                                    plugin.getDataManager()
                                            .getCache()
                                            .values()) {

                                data.setWeeklyClaimed(false);
                            }

                        },
                        1,
                        604800000,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                );
    }
}
