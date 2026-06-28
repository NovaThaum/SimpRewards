package org.simpmc.simpRewards.config;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.simpmc.simpRewards.reward.RewardAction;

public record RewardConfig(
        ZoneId zoneId,
        boolean autoClaimSignOnFirstJoin,
        int signStreakCycle,
        boolean allowMakeupSign,
        List<RewardAction> signBaseRewards,
        Map<Integer, List<RewardAction>> signStreakRewards,
        int onlineTickMinutes,
        int dailyTargetMinutes,
        List<RewardAction> dailyRewards,
        DayOfWeek weeklyStart,
        int weeklyTargetMinutes,
        List<RewardAction> weeklyRewards,
        Map<Integer, List<RewardAction>> totalOnlineMilestones,
        int activeThresholdMinutes,
        Map<Integer, List<RewardAction>> activeStreakMilestones,
        int autosaveMinutes
) {
}
