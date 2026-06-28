package org.simpmc.simpRewards.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simpRewards.config.ConfigManager;
import org.simpmc.simpRewards.gui.DailyCalendarMenuRenderer;

public final class DailyCommand implements CommandExecutor {
    private final ConfigManager configManager;
    private final DailyCalendarMenuRenderer dailyCalendarMenuRenderer;

    public DailyCommand(ConfigManager configManager, DailyCalendarMenuRenderer dailyCalendarMenuRenderer) {
        this.configManager = configManager;
        this.dailyCalendarMenuRenderer = dailyCalendarMenuRenderer;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getMessageConfig().colored("player-only"));
            return true;
        }
        if (!player.hasPermission("simprewards.use")) {
            player.sendMessage(configManager.getMessageConfig().colored("no-permission"));
            return true;
        }
        dailyCalendarMenuRenderer.open(player);
        return true;
    }
}
