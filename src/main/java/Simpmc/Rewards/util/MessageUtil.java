package Simpmc.Rewards.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtil {

    public static String color(String text) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }

    public static void send(Player player, String msg) {

        player.sendMessage(
                color(msg)
        );
    }
}
