package org.simpmc.simpRewards.gui;

import org.simpmc.simpRewards.reward.RewardType;

public enum RewardButtonType {
    DAILY_SIGN(RewardType.DAILY_SIGN),
    TOTAL_ONLINE(RewardType.TOTAL_ONLINE),
    DAILY_ONLINE(RewardType.DAILY_ONLINE),
    WEEKLY_ONLINE(RewardType.WEEKLY_ONLINE),
    ACTIVE_STREAK(RewardType.ACTIVE_STREAK),
    CLOSE(null),
    INFO(null),
    DECORATION(null);

    private final RewardType rewardType;

    RewardButtonType(RewardType rewardType) {
        this.rewardType = rewardType;
    }

    public RewardType rewardType() {
        return rewardType;
    }
}
