package Simpmc.Rewards.gui;

import Simpmc.Rewards.SimpRewardsPlugin;
import Simpmc.Rewards.data.PlayerData;
import Simpmc.Rewards.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class MainMenu {

    public static final String TITLE = "§8SimpRewards";

    public static void open(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                36,
                TITLE
        );

        PlayerData data = SimpRewardsPlugin
                .getInstance()
                .getDataManager()
                .getPlayerData(player.getUniqueId());

        inv.setItem(
                0,
                createItem(
                        Material.CLOCK,
                        "§a每日签到",
                        "§7点击领取今日奖励"
                )
        );

        inv.setItem(
                1,
                createItem(
                        Material.DIAMOND_SWORD,
                        "§b累计在线",
                        "§7总在线: "
                                + TimeUtil.formatMinutes(
                                data.getTotalMinutes()
                        )
                )
        );

        inv.setItem(
                2,
                createItem(
                        Material.EMERALD,
                        "§a每日任务",
                        "§7今日在线: "
                                + data.getDailyMinutes()
                                + " 分钟"
                )
        );

        inv.setItem(
                3,
                createItem(
                        Material.NETHERITE_INGOT,
                        "§6每周任务",
                        "§7本周在线: "
                                + data.getWeeklyMinutes()
                                + " 分钟"
                )
        );

        inv.setItem(
                4,
                createItem(
                        Material.ENCHANTED_BOOK,
                        "§d连续在线",
                        "§7连续活跃: "
                                + data.getStreakDays()
                                + " 天"
                )
        );

        player.openInventory(inv);
    }

    private static ItemStack createItem(
            Material material,
            String name,
            String lore
    ) {

        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        ArrayList<String> loreList = new ArrayList<>();

        loreList.add(lore);

        meta.setLore(loreList);

        item.setItemMeta(meta);

        return item;
    }
}
