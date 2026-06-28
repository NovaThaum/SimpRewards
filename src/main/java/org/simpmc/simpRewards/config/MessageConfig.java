package org.simpmc.simpRewards.config;

import java.util.Map;
import org.simpmc.simpRewards.util.Texts;

public record MessageConfig(String prefix, Map<String, String> messages) {
    public String raw(String key) {
        return messages.getOrDefault(key, key);
    }

    public String colored(String key) {
        return Texts.color(prefix + raw(key));
    }

    public String colored(String key, Map<String, String> placeholders) {
        return Texts.color(prefix + Texts.placeholders(raw(key), placeholders));
    }
}
