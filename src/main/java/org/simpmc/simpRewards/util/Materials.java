package org.simpmc.simpRewards.util;

import java.util.Locale;
import org.bukkit.Material;

public final class Materials {
    private Materials() {
    }

    public static Material parse(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }
}
