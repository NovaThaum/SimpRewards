package Simpmc.Rewards.task;

import Simpmc.Rewards.SimpRewardsPlugin;
import Simpmc.Rewards.data.PlayerData;

public class ActiveDayTask {

    private final SimpRewardsPlugin plugin;

    public ActiveDayTask(SimpRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    public void check(PlayerData data) {

        int required = plugin.getConfig()
                .getInt("settings.active-minutes-required");

        if (data.getDailyMinutes() >= required) {

            data.setStreakDays(
                    data.getStreakDays() + 1
            );

        } else {

            data.setStreakDays(0);
        }
    }
}
