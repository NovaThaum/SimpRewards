package Simpmc.Rewards.reward;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class RewardParser {

    public static List<RewardObject> parse(ConfigurationSection section) {

        List<RewardObject> list = new ArrayList<>();

        if (section == null) {
            return list;
        }

        for (String key : section.getKeys(false)) {

            ConfigurationSection reward = section.getConfigurationSection(key);

            if (reward == null) {
                continue;
            }

            RewardObject object = new RewardObject();

            object.setType(
                    RewardType.valueOf(
                            reward.getString("type").toUpperCase()
                    )
            );

            switch (object.getType()) {

                case MONEY, XP -> {
                    object.setAmount(reward.getDouble("amount"));
                }

                case ITEM -> {

                    object.setMaterial(
                            Material.valueOf(
                                    reward.getString("material")
                            )
                    );

                    object.setItemAmount(
                            reward.getInt("amount")
                    );
                }

                case COMMAND -> {
                    object.setCommand(
                            reward.getString("command")
                    );
                }
            }

            list.add(object);
        }

        return list;
    }
}
