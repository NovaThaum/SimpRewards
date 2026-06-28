package org.simpmc.simpRewards.gui;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class RewardMenuHolder implements InventoryHolder {
    private final Map<Integer, RewardButtonType> buttons = new HashMap<>();
    private final MenuClickHandler clickHandler;
    private Inventory inventory;

    public RewardMenuHolder() {
        this.clickHandler = null;
    }

    public RewardMenuHolder(MenuClickHandler clickHandler) {
        this.clickHandler = clickHandler;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setButton(int slot, RewardButtonType type) {
        buttons.put(slot, type);
    }

    public RewardButtonType buttonAt(int slot) {
        return buttons.getOrDefault(slot, RewardButtonType.DECORATION);
    }

    public MenuClickHandler clickHandler() {
        return clickHandler;
    }
}
