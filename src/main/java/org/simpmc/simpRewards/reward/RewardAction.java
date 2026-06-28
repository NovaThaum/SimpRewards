package org.simpmc.simpRewards.reward;

import java.util.List;

public record RewardAction(
        RewardActionType type,
        String material,
        double amount,
        String name,
        List<String> lore,
        String command
) {
    public int intAmount() {
        return Math.max(1, (int) Math.round(amount));
    }
}
