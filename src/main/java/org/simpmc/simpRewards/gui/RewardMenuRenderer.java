package org.simpmc.simpRewards.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.simpmc.simpRewards.config.ConfigManager;
import org.simpmc.simpRewards.config.GuiConfig;
import org.simpmc.simpRewards.data.PlayerRewardRepository;
import org.simpmc.simpRewards.data.PlayerRewardState;
import org.simpmc.simpRewards.reward.RewardService;
import org.simpmc.simpRewards.reward.RewardType;
import org.simpmc.simpRewards.time.ResetService;
import org.simpmc.simpRewards.time.TimeService;
import org.simpmc.simpRewards.util.Texts;

public final class RewardMenuRenderer {
    private final ConfigManager configManager;
    private final PlayerRewardRepository repository;
    private final ResetService resetService;
    private final RewardService rewardService;
    private final TimeService timeService;

    public RewardMenuRenderer(
            ConfigManager configManager,
            PlayerRewardRepository repository,
            ResetService resetService,
            RewardService rewardService,
            TimeService timeService
    ) {
        this.configManager = configManager;
        this.repository = repository;
        this.resetService = resetService;
        this.rewardService = rewardService;
        this.timeService = timeService;
    }

    public void open(Player player) {
        player.openInventory(createInventory(player));
    }

    public void refresh(Player player) {
        player.openInventory(createInventory(player));
    }

    public void handleClick(Player player, RewardButtonType buttonType) {
        if (buttonType == RewardButtonType.CLOSE) {
            player.closeInventory();
            return;
        }
        if (buttonType.rewardType() == null) {
            return;
        }
        rewardService.claim(player, buttonType.rewardType());
        refresh(player);
    }

    private Inventory createInventory(Player player) {
        PlayerRewardState state = repository.load(player.getUniqueId(), player.getName());
        resetService.applyResets(state);

        GuiConfig guiConfig = configManager.getGuiConfig();
        RewardMenuHolder holder = new RewardMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, guiConfig.size(), Texts.color(guiConfig.title()));
        holder.setInventory(inventory);

        Map<String, String> placeholders = placeholders(state);
        addOverview(inventory, holder, "overview-sign", placeholders);
        addOverview(inventory, holder, "overview-total", placeholders);
        addOverview(inventory, holder, "overview-daily", placeholders);
        addOverview(inventory, holder, "overview-weekly", placeholders);
        addOverview(inventory, holder, "overview-active", placeholders);
        addRewardButton(inventory, holder, "daily-sign", RewardButtonType.DAILY_SIGN, signState(state), placeholders);
        addRewardButton(inventory, holder, "total-online", RewardButtonType.TOTAL_ONLINE, totalState(state), placeholders);
        addRewardButton(inventory, holder, "daily-task", RewardButtonType.DAILY_ONLINE, dailyTaskState(state), placeholders);
        addRewardButton(inventory, holder, "weekly-task", RewardButtonType.WEEKLY_ONLINE, weeklyTaskState(state), placeholders);
        addRewardButton(inventory, holder, "active-streak", RewardButtonType.ACTIVE_STREAK, activeState(state), placeholders);
        addOverview(inventory, holder, "help", placeholders);
        addOverview(inventory, holder, "reset", placeholders);
        addClose(inventory, holder, placeholders);
        addFiller(inventory, holder, placeholders);

        repository.save(state);
        return inventory;
    }

    private void addOverview(Inventory inventory, RewardMenuHolder holder, String key, Map<String, String> placeholders) {
        GuiConfig.GuiItemConfig item = configManager.getGuiConfig().item(key);
        if (item == null || item.slot() < 0 || item.slot() >= inventory.getSize()) {
            return;
        }
        inventory.setItem(item.slot(), ItemBuilder.build(item.material(), item.name(), item.lore(), placeholders));
        holder.setButton(item.slot(), RewardButtonType.INFO);
    }

    private void addClose(Inventory inventory, RewardMenuHolder holder, Map<String, String> placeholders) {
        GuiConfig.GuiItemConfig item = configManager.getGuiConfig().item("close");
        if (item == null || item.slot() < 0 || item.slot() >= inventory.getSize()) {
            return;
        }
        inventory.setItem(item.slot(), ItemBuilder.build(item.material(), item.name(), item.lore(), placeholders));
        holder.setButton(item.slot(), RewardButtonType.CLOSE);
    }

    private void addFiller(Inventory inventory, RewardMenuHolder holder, Map<String, String> placeholders) {
        GuiConfig.GuiItemConfig item = configManager.getGuiConfig().item("filler");
        if (item == null || item.slots() == null) {
            return;
        }
        for (Integer slot : item.slots()) {
            if (slot == null || slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            inventory.setItem(slot, ItemBuilder.build(item.material(), item.name(), item.lore(), placeholders));
            holder.setButton(slot, RewardButtonType.DECORATION);
        }
    }

    private void addRewardButton(
            Inventory inventory,
            RewardMenuHolder holder,
            String key,
            RewardButtonType buttonType,
            String stateName,
            Map<String, String> placeholders
    ) {
        GuiConfig.GuiItemConfig item = configManager.getGuiConfig().item(key);
        if (item == null || item.slot() < 0 || item.slot() >= inventory.getSize()) {
            return;
        }
        GuiConfig.GuiItemStateConfig state = item.state(stateName);
        if (state == null) {
            return;
        }
        inventory.setItem(item.slot(), ItemBuilder.build(state.material(), state.name(), state.lore(), placeholders));
        holder.setButton(item.slot(), buttonType);
    }

    private String signState(PlayerRewardState state) {
        return rewardService.isDailySignClaimed(state) ? "claimed" : "claimable";
    }

    private String totalState(PlayerRewardState state) {
        if (rewardService.claimableTotalMilestone(state).isPresent()) {
            return "claimable";
        }
        return rewardService.nextTotalMilestone(state).isPresent() ? "locked" : "claimed";
    }

    private String dailyTaskState(PlayerRewardState state) {
        if (rewardService.isDailyTaskClaimed(state)) {
            return "claimed";
        }
        return state.dailyOnlineMinutes() >= configManager.getRewardConfig().dailyTargetMinutes() ? "claimable" : "locked";
    }

    private String weeklyTaskState(PlayerRewardState state) {
        if (rewardService.isWeeklyTaskClaimed(state)) {
            return "claimed";
        }
        return state.weeklyOnlineMinutes() >= configManager.getRewardConfig().weeklyTargetMinutes() ? "claimable" : "locked";
    }

    private String activeState(PlayerRewardState state) {
        if (rewardService.claimableActiveMilestone(state).isPresent()) {
            return "claimable";
        }
        return rewardService.nextActiveMilestone(state).isPresent() ? "locked" : "claimed";
    }

    private Map<String, String> placeholders(PlayerRewardState state) {
        Map<String, String> values = new HashMap<>();
        Optional<Integer> nextTotal = rewardService.nextTotalMilestone(state);
        Optional<Integer> claimableTotal = rewardService.claimableTotalMilestone(state);
        Optional<Integer> nextActive = rewardService.nextActiveMilestone(state);
        Optional<Integer> claimableActive = rewardService.claimableActiveMilestone(state);
        int dailyTarget = configManager.getRewardConfig().dailyTargetMinutes();
        int weeklyTarget = configManager.getRewardConfig().weeklyTargetMinutes();

        values.put("%player%", state.name());
        values.put("%total_minutes%", String.valueOf(state.totalOnlineMinutes()));
        values.put("%daily_minutes%", String.valueOf(state.dailyOnlineMinutes()));
        values.put("%weekly_minutes%", String.valueOf(state.weeklyOnlineMinutes()));
        values.put("%sign_streak%", String.valueOf(state.signStreak()));
        values.put("%active_streak%", String.valueOf(state.activeStreak()));
        values.put("%daily_target%", String.valueOf(dailyTarget));
        values.put("%weekly_target%", String.valueOf(weeklyTarget));
        values.put("%daily_remaining%", String.valueOf(Math.max(0, dailyTarget - state.dailyOnlineMinutes())));
        values.put("%weekly_remaining%", String.valueOf(Math.max(0, weeklyTarget - state.weeklyOnlineMinutes())));
        values.put("%sign_status%", rewardService.isDailySignClaimed(state) ? "已签到" : "未签到");
        values.put("%next_total_milestone%", nextTotal.map(String::valueOf).orElse("无"));
        values.put("%claimable_total_milestone%", claimableTotal.map(String::valueOf).orElse("无"));
        values.put("%total_remaining%", nextTotal
                .map(milestone -> String.valueOf(Math.max(0, milestone - state.totalOnlineMinutes())))
                .orElse("0"));
        values.put("%next_active_milestone%", nextActive.map(String::valueOf).orElse("无"));
        values.put("%claimable_active_milestone%", claimableActive.map(String::valueOf).orElse("无"));
        values.put("%active_remaining%", nextActive
                .map(milestone -> String.valueOf(Math.max(0, milestone - state.activeStreak())))
                .orElse("0"));
        values.put("%today%", timeService.today().toString());
        return values;
    }
}
