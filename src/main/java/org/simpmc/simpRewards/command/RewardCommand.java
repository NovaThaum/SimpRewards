package org.simpmc.simpRewards.command;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.simpmc.simpRewards.SimpRewards;
import org.simpmc.simpRewards.config.ConfigManager;
import org.simpmc.simpRewards.data.PlayerRewardRepository;
import org.simpmc.simpRewards.data.PlayerRewardState;
import org.simpmc.simpRewards.gui.RewardMenuRenderer;
import org.simpmc.simpRewards.time.ResetService;

public final class RewardCommand implements CommandExecutor, TabCompleter {
    private final SimpRewards plugin;
    private final ConfigManager configManager;
    private final PlayerRewardRepository repository;
    private final ResetService resetService;
    private final RewardMenuRenderer rewardMenuRenderer;

    public RewardCommand(
            SimpRewards plugin,
            ConfigManager configManager,
            PlayerRewardRepository repository,
            ResetService resetService,
            RewardMenuRenderer rewardMenuRenderer
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.repository = repository;
        this.resetService = resetService;
        this.rewardMenuRenderer = rewardMenuRenderer;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(configManager.getMessageConfig().colored("player-only"));
                return true;
            }
            if (!player.hasPermission("simprewards.use")) {
                player.sendMessage(configManager.getMessageConfig().colored("no-permission"));
                return true;
            }
            rewardMenuRenderer.open(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "open" -> {
                return handleOpen(sender, args);
            }
            case "reload" -> {
                return handleReload(sender);
            }
            case "stats" -> {
                return handleStats(sender, args);
            }
            default -> {
                sender.sendMessage("/reward [open|reload|stats]");
                return true;
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return List.of("open", "reload", "stats").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && List.of("open", "stats").contains(args[0].toLowerCase(Locale.ROOT))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }

    private boolean handleOpen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("simprewards.admin") && !(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessageConfig().colored("no-permission"));
            return true;
        }
        if (args.length >= 2) {
            if (!sender.hasPermission("simprewards.admin")) {
                sender.sendMessage(configManager.getMessageConfig().colored("no-permission"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(configManager.getMessageConfig().colored("player-not-found"));
                return true;
            }
            rewardMenuRenderer.open(target);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getMessageConfig().colored("player-only"));
            return true;
        }
        if (!player.hasPermission("simprewards.use")) {
            player.sendMessage(configManager.getMessageConfig().colored("no-permission"));
            return true;
        }
        rewardMenuRenderer.open(player);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("simprewards.reload")) {
            sender.sendMessage(configManager.getMessageConfig().colored("no-permission"));
            return true;
        }
        plugin.reloadPlugin();
        sender.sendMessage(configManager.getMessageConfig().colored("reload-success"));
        return true;
    }

    private boolean handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("simprewards.stats")) {
            sender.sendMessage(configManager.getMessageConfig().colored("no-permission"));
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(configManager.getMessageConfig().colored("player-only"));
            return true;
        }
        if (target == null) {
            sender.sendMessage(configManager.getMessageConfig().colored("player-not-found"));
            return true;
        }
        PlayerRewardState state = repository.load(target.getUniqueId(), target.getName());
        resetService.applyResets(state);
        sender.sendMessage(configManager.getMessageConfig().colored("stats", Map.of(
                "%player%", state.name(),
                "%total_minutes%", String.valueOf(state.totalOnlineMinutes()),
                "%daily_minutes%", String.valueOf(state.dailyOnlineMinutes()),
                "%weekly_minutes%", String.valueOf(state.weeklyOnlineMinutes()),
                "%sign_streak%", String.valueOf(state.signStreak()),
                "%active_streak%", String.valueOf(state.activeStreak())
        )));
        return true;
    }
}
