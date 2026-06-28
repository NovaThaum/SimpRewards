package org.simpmc.simpRewards.reward;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.simpmc.simpRewards.config.ConfigManager;
import org.simpmc.simpRewards.config.MessageConfig;
import org.simpmc.simpRewards.config.RewardConfig;
import org.simpmc.simpRewards.data.PlayerRewardRepository;
import org.simpmc.simpRewards.data.PlayerRewardState;
import org.simpmc.simpRewards.economy.VaultEconomyService;
import org.simpmc.simpRewards.time.ResetService;
import org.simpmc.simpRewards.time.TimeService;
import org.simpmc.simpRewards.util.Materials;
import org.simpmc.simpRewards.util.Texts;

public final class RewardService {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerRewardRepository repository;
    private final ResetService resetService;
    private final TimeService timeService;
    private final VaultEconomyService economyService;

    public RewardService(
            JavaPlugin plugin,
            ConfigManager configManager,
            PlayerRewardRepository repository,
            ResetService resetService,
            TimeService timeService,
            VaultEconomyService economyService
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.repository = repository;
        this.resetService = resetService;
        this.timeService = timeService;
        this.economyService = economyService;
    }

    public RewardOutcome claim(Player player, RewardType type) {
        PlayerRewardState state = repository.load(player.getUniqueId(), player.getName());
        resetService.applyResets(state);
        RewardOutcome outcome = switch (type) {
            case DAILY_SIGN -> claimDailySign(player, state);
            case TOTAL_ONLINE -> claimTotalOnline(player, state);
            case DAILY_ONLINE -> claimDailyOnline(player, state);
            case WEEKLY_ONLINE -> claimWeeklyOnline(player, state);
            case ACTIVE_STREAK -> claimActiveStreak(player, state);
        };
        repository.save(state);
        sendOutcome(player, outcome);
        return outcome;
    }

    public boolean isDailySignClaimed(PlayerRewardState state) {
        LocalDate today = timeService.today();
        return today.equals(state.signedDate());
    }

    public boolean isDailyTaskClaimed(PlayerRewardState state) {
        return timeService.today().equals(state.claimedDailyDate());
    }

    public boolean isWeeklyTaskClaimed(PlayerRewardState state) {
        return resetService.currentWeekKey().equals(state.claimedWeeklyWeek());
    }

    public Optional<Integer> nextTotalMilestone(PlayerRewardState state) {
        return config().totalOnlineMilestones().keySet().stream()
                .filter(milestone -> !state.claimedTotalMilestones().contains(milestone))
                .sorted()
                .findFirst();
    }

    public Optional<Integer> claimableTotalMilestone(PlayerRewardState state) {
        return config().totalOnlineMilestones().keySet().stream()
                .filter(milestone -> state.totalOnlineMinutes() >= milestone)
                .filter(milestone -> !state.claimedTotalMilestones().contains(milestone))
                .min(Comparator.naturalOrder());
    }

    public Optional<Integer> nextActiveMilestone(PlayerRewardState state) {
        return config().activeStreakMilestones().keySet().stream()
                .filter(milestone -> !state.claimedActiveStreakMilestones().contains(milestone))
                .sorted()
                .findFirst();
    }

    public Optional<Integer> claimableActiveMilestone(PlayerRewardState state) {
        return config().activeStreakMilestones().keySet().stream()
                .filter(milestone -> state.activeStreak() >= milestone)
                .filter(milestone -> !state.claimedActiveStreakMilestones().contains(milestone))
                .min(Comparator.naturalOrder());
    }

    private RewardOutcome claimDailySign(Player player, PlayerRewardState state) {
        LocalDate today = timeService.today();
        if (today.equals(state.signedDate())) {
            return RewardOutcome.alreadyClaimed();
        }

        int nextStreak = calculateNextSignStreak(state, today);
        List<RewardAction> rewards = new ArrayList<>(config().signBaseRewards());
        int effectiveStreak = effectiveStreak(nextStreak);
        if (config().signStreakRewards().containsKey(effectiveStreak)) {
            rewards.addAll(config().signStreakRewards().get(effectiveStreak));
        }

        if (!grantRewards(player, rewards, Map.of(
                "%player%", player.getName(),
                "%uuid%", player.getUniqueId().toString(),
                "%sign_streak%", String.valueOf(nextStreak)
        ))) {
            return RewardOutcome.configError();
        }

        state.setSignedDate(today);
        state.addSignedDate(today);
        state.setSignStreak(nextStreak);
        return RewardOutcome.success(effectiveStreak);
    }

    private RewardOutcome claimTotalOnline(Player player, PlayerRewardState state) {
        Optional<Integer> milestone = claimableTotalMilestone(state);
        if (milestone.isEmpty()) {
            Optional<Integer> next = nextTotalMilestone(state);
            if (next.isPresent()) {
                return RewardOutcome.notEnoughProgress((int) Math.max(0, next.get() - state.totalOnlineMinutes()));
            }
            return RewardOutcome.noClaimableMilestone();
        }

        List<RewardAction> rewards = config().totalOnlineMilestones().getOrDefault(milestone.get(), List.of());
        if (!grantRewards(player, rewards, Map.of(
                "%player%", player.getName(),
                "%uuid%", player.getUniqueId().toString(),
                "%total_minutes%", String.valueOf(state.totalOnlineMinutes()),
                "%milestone%", String.valueOf(milestone.get())
        ))) {
            return RewardOutcome.configError();
        }
        state.claimedTotalMilestones().add(milestone.get());
        return RewardOutcome.success(milestone.get());
    }

    private RewardOutcome claimDailyOnline(Player player, PlayerRewardState state) {
        if (isDailyTaskClaimed(state)) {
            return RewardOutcome.alreadyClaimed();
        }
        int remaining = config().dailyTargetMinutes() - state.dailyOnlineMinutes();
        if (remaining > 0) {
            return RewardOutcome.notEnoughProgress(remaining);
        }

        if (!grantRewards(player, config().dailyRewards(), Map.of(
                "%player%", player.getName(),
                "%uuid%", player.getUniqueId().toString(),
                "%daily_minutes%", String.valueOf(state.dailyOnlineMinutes())
        ))) {
            return RewardOutcome.configError();
        }
        state.setClaimedDailyDate(timeService.today());
        return RewardOutcome.success(null);
    }

    private RewardOutcome claimWeeklyOnline(Player player, PlayerRewardState state) {
        String currentWeek = resetService.currentWeekKey();
        if (currentWeek.equals(state.claimedWeeklyWeek())) {
            return RewardOutcome.alreadyClaimed();
        }
        int remaining = config().weeklyTargetMinutes() - state.weeklyOnlineMinutes();
        if (remaining > 0) {
            return RewardOutcome.notEnoughProgress(remaining);
        }

        if (!grantRewards(player, config().weeklyRewards(), Map.of(
                "%player%", player.getName(),
                "%uuid%", player.getUniqueId().toString(),
                "%weekly_minutes%", String.valueOf(state.weeklyOnlineMinutes())
        ))) {
            return RewardOutcome.configError();
        }
        state.setClaimedWeeklyWeek(currentWeek);
        return RewardOutcome.success(null);
    }

    private RewardOutcome claimActiveStreak(Player player, PlayerRewardState state) {
        Optional<Integer> milestone = claimableActiveMilestone(state);
        if (milestone.isEmpty()) {
            Optional<Integer> next = nextActiveMilestone(state);
            if (next.isPresent()) {
                return RewardOutcome.notEnoughProgress(Math.max(0, next.get() - state.activeStreak()));
            }
            return RewardOutcome.noClaimableMilestone();
        }

        List<RewardAction> rewards = config().activeStreakMilestones().getOrDefault(milestone.get(), List.of());
        if (!grantRewards(player, rewards, Map.of(
                "%player%", player.getName(),
                "%uuid%", player.getUniqueId().toString(),
                "%active_streak%", String.valueOf(state.activeStreak()),
                "%milestone%", String.valueOf(milestone.get())
        ))) {
            return RewardOutcome.configError();
        }
        state.claimedActiveStreakMilestones().add(milestone.get());
        return RewardOutcome.success(milestone.get());
    }

    private int calculateNextSignStreak(PlayerRewardState state, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        if (yesterday.equals(state.signedDate())) {
            return state.signStreak() + 1;
        }
        return 1;
    }

    private int effectiveStreak(int streak) {
        int cycle = config().signStreakCycle();
        if (cycle <= 0) {
            return streak;
        }
        int remainder = streak % cycle;
        return remainder == 0 ? cycle : remainder;
    }

    private boolean grantRewards(Player player, List<RewardAction> rewards, Map<String, String> placeholders) {
        for (RewardAction reward : rewards) {
            switch (reward.type()) {
                case ITEM -> grantItem(player, reward, placeholders);
                case EXP -> player.giveExp(reward.intAmount());
                case COMMAND -> dispatchCommand(player, reward, placeholders);
                case MONEY -> {
                    if (!economyService.deposit(player, reward.amount())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void grantItem(Player player, RewardAction reward, Map<String, String> placeholders) {
        Material material = Materials.parse(reward.material(), Material.STONE);
        ItemStack itemStack = new ItemStack(material, reward.intAmount());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (reward.name() != null && !reward.name().isBlank()) {
                meta.setDisplayName(Texts.color(Texts.placeholders(reward.name(), placeholders)));
            }
            if (reward.lore() != null && !reward.lore().isEmpty()) {
                meta.setLore(reward.lore().stream()
                        .map(line -> Texts.color(Texts.placeholders(line, placeholders)))
                        .toList());
            }
            itemStack.setItemMeta(meta);
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(messages().colored("inventory-full-dropped"));
        }
    }

    private void dispatchCommand(Player player, RewardAction reward, Map<String, String> placeholders) {
        if (reward.command() == null || reward.command().isBlank()) {
            return;
        }
        String command = Texts.placeholders(reward.command(), placeholders);
        Server server = plugin.getServer();
        server.dispatchCommand(server.getConsoleSender(), command);
    }

    private void sendOutcome(Player player, RewardOutcome outcome) {
        MessageConfig messages = messages();
        switch (outcome.type()) {
            case SUCCESS -> player.sendMessage(messages.colored("claim-success"));
            case ALREADY_CLAIMED -> player.sendMessage(messages.colored("already-claimed"));
            case NOT_ENOUGH_PROGRESS -> player.sendMessage(messages.colored("not-enough-progress", Map.of(
                    "%remaining%", String.valueOf(outcome.remaining())
            )));
            case NO_CLAIMABLE_MILESTONE -> player.sendMessage(messages.colored("no-claimable-milestone"));
            case CONFIG_ERROR -> player.sendMessage(messages.colored("config-error"));
        }
    }

    private RewardConfig config() {
        return configManager.getRewardConfig();
    }

    private MessageConfig messages() {
        return configManager.getMessageConfig();
    }
}
