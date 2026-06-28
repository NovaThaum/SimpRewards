package org.simpmc.simpRewards.time;

import java.time.LocalDate;
import org.simpmc.simpRewards.config.RewardConfig;
import org.simpmc.simpRewards.data.PlayerRewardState;

public final class ResetService {
    private final TimeService timeService;
    private RewardConfig rewardConfig;

    public ResetService(TimeService timeService, RewardConfig rewardConfig) {
        this.timeService = timeService;
        this.rewardConfig = rewardConfig;
    }

    public void setRewardConfig(RewardConfig rewardConfig) {
        this.rewardConfig = rewardConfig;
    }

    public void applyResets(PlayerRewardState state) {
        LocalDate today = timeService.today();
        applyDailyReset(state, today);
        applyWeeklyReset(state, today);
        state.setLastSeenDate(today);
    }

    public String currentWeekKey() {
        return WeekKey.from(timeService.today(), rewardConfig.weeklyStart()).toString();
    }

    private void applyDailyReset(PlayerRewardState state, LocalDate today) {
        LocalDate lastReset = state.lastDailyResetDate();
        if (lastReset == null) {
            state.setLastDailyResetDate(today);
            return;
        }
        if (!lastReset.isBefore(today)) {
            return;
        }

        LocalDate yesterday = today.minusDays(1);
        if (state.signedDate() == null || state.signedDate().isBefore(yesterday)) {
            state.setSignStreak(0);
        }
        if (state.lastActiveDate() == null || state.lastActiveDate().isBefore(yesterday)) {
            state.setActiveStreak(0);
        }
        state.setDailyOnlineMinutes(0);
        state.setClaimedDailyDate(null);
        state.setLastDailyResetDate(today);
    }

    private void applyWeeklyReset(PlayerRewardState state, LocalDate today) {
        String currentWeek = WeekKey.from(today, rewardConfig.weeklyStart()).toString();
        if (state.lastWeeklyResetWeek() == null || state.lastWeeklyResetWeek().isBlank()) {
            state.setLastWeeklyResetWeek(currentWeek);
            return;
        }
        WeekKey last = WeekKey.parse(state.lastWeeklyResetWeek());
        WeekKey current = WeekKey.parse(currentWeek);
        if (last == null || current == null || last.compareTo(current) < 0) {
            state.setWeeklyOnlineMinutes(0);
            state.setClaimedWeeklyWeek("");
            state.setLastWeeklyResetWeek(currentWeek);
        }
    }
}
