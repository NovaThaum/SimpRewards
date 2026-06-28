package org.simpmc.simpRewards.config;

import java.util.List;
import java.util.Map;

public record GuiConfig(String title, int size, Map<String, GuiItemConfig> items) {
    public GuiItemConfig item(String key) {
        return items.get(key);
    }

    public record GuiItemConfig(
            int slot,
            String material,
            String name,
            List<String> lore,
            Map<String, GuiItemStateConfig> states,
            List<Integer> slots
    ) {
        public GuiItemStateConfig state(String state) {
            if (states == null) {
                return null;
            }
            return states.get(state);
        }
    }

    public record GuiItemStateConfig(String material, String name, List<String> lore) {
    }
}
