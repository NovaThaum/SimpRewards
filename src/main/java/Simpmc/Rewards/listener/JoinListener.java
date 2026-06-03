package Simpmc.Rewards.listener;

import Simpmc.Rewards.SimpRewardsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final SimpRewardsPlugin plugin;

    public JoinListener(SimpRewardsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        plugin.getDataManager()
                .getPlayerData(event.getPlayer().getUniqueId());
    }
}
