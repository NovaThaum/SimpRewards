package org.simpmc.simpRewards.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.simpmc.simpRewards.gui.RewardMenuHolder;
import org.simpmc.simpRewards.gui.RewardMenuRenderer;

public final class InventoryClickListener implements Listener {
    private final RewardMenuRenderer rewardMenuRenderer;

    public InventoryClickListener(RewardMenuRenderer rewardMenuRenderer) {
        this.rewardMenuRenderer = rewardMenuRenderer;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RewardMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }
        if (holder.clickHandler() != null) {
            holder.clickHandler().handleClick(player, event.getRawSlot());
            return;
        }
        rewardMenuRenderer.handleClick(player, holder.buttonAt(event.getRawSlot()));
    }
}
