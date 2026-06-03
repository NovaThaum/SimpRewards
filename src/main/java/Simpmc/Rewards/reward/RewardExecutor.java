package Simpmc.Rewards.reward;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RewardExecutor {

    private static Economy economy;

    public static void setEconomy(Economy eco) {
        economy = eco;
    }

    public static void execute(Player player, List<RewardObject> rewards) {

        for (RewardObject reward : rewards) {

            switch (reward.getType()) {

                case XP -> {

                    player.giveExp(
                            (int) reward.getAmount()
                    );
                }

                case ITEM -> {

                    player.getInventory().addItem(
                            new ItemStack(
                                    reward.getMaterial(),
                                    reward.getItemAmount()
                            )
                    );
                }

                case COMMAND -> {

                    Bukkit.dispatchCommand(
                            Bukkit.getConsoleSender(),
                            reward.getCommand()
                                    .replace(
                                            "%player%",
                                            player.getName()
                                    )
                    );
                }

                case MONEY -> {

                    if (economy != null) {

                        economy.depositPlayer(
                                player,
                                reward.getAmount()
                        );
                    }
                }
            }
        }
    }
}
