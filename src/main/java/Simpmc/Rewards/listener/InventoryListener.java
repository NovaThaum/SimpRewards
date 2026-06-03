package Simpmc.Rewards.listener;

import Simpmc.Rewards.SimpRewardsPlugin;
import Simpmc.Rewards.data.PlayerData;
import Simpmc.Rewards.gui.MainMenu;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.io.File;

public class InventoryListener implements Listener {

    private final SimpRewardsPlugin plugin;

    public InventoryListener(SimpRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!event.getView().getTitle()
                .equals(MainMenu.TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        PlayerData data = plugin.getDataManager()
                .getPlayerData(player.getUniqueId());

        switch (event.getSlot()) {

            case 0 -> {

                String today =
                        java.time.LocalDate.now().toString();

                if (data.getLastSignDate().equals(today)) {

                    player.sendMessage("§c今天已经签到过了");

                    return;
                }

                data.setLastSignDate(today);

                data.setStreakDays(
                        data.getStreakDays() + 1
                );

                player.sendMessage("§a签到成功");

                MainMenu.open(player);
            }
        }
    }
}
