package org.simpmc.simpRewards.reward;

public record RewardOutcome(RewardOutcomeType type, int remaining, Integer milestone) {
    public static RewardOutcome success(Integer milestone) {
        return new RewardOutcome(RewardOutcomeType.SUCCESS, 0, milestone);
    }

    public static RewardOutcome alreadyClaimed() {
        return new RewardOutcome(RewardOutcomeType.ALREADY_CLAIMED, 0, null);
    }

    public static RewardOutcome notEnoughProgress(int remaining) {
        return new RewardOutcome(RewardOutcomeType.NOT_ENOUGH_PROGRESS, Math.max(0, remaining), null);
    }

    public static RewardOutcome noClaimableMilestone() {
        return new RewardOutcome(RewardOutcomeType.NO_CLAIMABLE_MILESTONE, 0, null);
    }

    public static RewardOutcome configError() {
        return new RewardOutcome(RewardOutcomeType.CONFIG_ERROR, 0, null);
    }
}
