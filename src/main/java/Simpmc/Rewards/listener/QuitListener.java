package Simpmc.Rewards.listener;

import Simpmc.Rewards.SimpRewardsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    private final SimpRewardsPlugin plugin;

    public QuitListener(SimpRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        plugin.getDataManager()
                .save(event.getPlayer().getUniqueId());
    }
}
