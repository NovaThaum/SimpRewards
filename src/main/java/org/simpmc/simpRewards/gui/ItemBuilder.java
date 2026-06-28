package org.simpmc.simpRewards.gui;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.simpmc.simpRewards.util.Materials;
import org.simpmc.simpRewards.util.Texts;

public final class ItemBuilder {
    private ItemBuilder() {
    }

    public static ItemStack build(String materialName, String name, List<String> lore, Map<String, String> placeholders) {
        Material material = Materials.parse(materialName, Material.STONE);
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Texts.color(Texts.placeholders(name == null ? "" : name, placeholders)));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream()
                        .map(line -> Texts.color(Texts.placeholders(line, placeholders)))
                        .toList());
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
