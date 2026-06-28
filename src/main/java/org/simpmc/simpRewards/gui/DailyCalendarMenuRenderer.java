package org.simpmc.simpRewards.gui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.simpmc.simpRewards.data.PlayerRewardRepository;
import org.simpmc.simpRewards.data.PlayerRewardState;
import org.simpmc.simpRewards.reward.RewardService;
import org.simpmc.simpRewards.reward.RewardType;
import org.simpmc.simpRewards.time.ResetService;
import org.simpmc.simpRewards.time.TimeService;
import org.simpmc.simpRewards.util.Texts;

public final class DailyCalendarMenuRenderer implements MenuClickHandler {
    private static final int SIZE = 54;
    private static final int TODAY_SLOT = 49;
    private static final int[] DAY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
            46, 47, 48, 50, 51, 52, 53
    };
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM", Locale.ROOT);

    private final PlayerRewardRepository repository;
    private final ResetService resetService;
    private final RewardService rewardService;
    private final TimeService timeService;

    public DailyCalendarMenuRenderer(
            PlayerRewardRepository repository,
            ResetService resetService,
            RewardService rewardService,
            TimeService timeService
    ) {
        this.repository = repository;
        this.resetService = resetService;
        this.rewardService = rewardService;
        this.timeService = timeService;
    }

    public void open(Player player) {
        player.openInventory(createInventory(player));
    }

    @Override
    public void handleClick(Player player, int slot) {
        if (slot == TODAY_SLOT) {
            rewardService.claim(player, RewardType.DAILY_SIGN);
            open(player);
        }
    }

    private Inventory createInventory(Player player) {
        PlayerRewardState state = repository.load(player.getUniqueId(), player.getName());
        resetService.applyResets(state);

        LocalDate today = timeService.today();
        YearMonth month = YearMonth.from(today);
        RewardMenuHolder holder = new RewardMenuHolder(this);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, Texts.color("&6每日签到日历 &7" + month.format(MONTH_FORMAT)));
        holder.setInventory(inventory);

        addWeekHeaders(inventory);
        addDays(inventory, state, today, month);
        addSummary(inventory, state, today);
        repository.save(state);
        return inventory;
    }

    private void addWeekHeaders(Inventory inventory) {
        String[] headers = {"一", "二", "三", "四", "五", "六", "日"};
        for (int i = 0; i < headers.length; i++) {
            inventory.setItem(i + 1, ItemBuilder.build(
                    Material.GRAY_STAINED_GLASS_PANE.name(),
                    "&7周" + headers[i],
                    List.of(),
                    Map.of()
            ));
        }
    }

    private void addDays(Inventory inventory, PlayerRewardState state, LocalDate today, YearMonth month) {
        LocalDate firstDay = month.atDay(1);
        int offset = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            int slotIndex = offset + day - 1;
            if (slotIndex < 0 || slotIndex >= DAY_SLOTS.length) {
                continue;
            }
            LocalDate date = month.atDay(day);
            boolean signed = state.hasSignedOn(date);
            boolean isToday = date.equals(today);
            boolean past = date.isBefore(today);
            String material = materialFor(signed, isToday, past);
            String name = nameFor(day, signed, isToday, past);
            List<String> lore = loreFor(date, signed, isToday, past);
            inventory.setItem(DAY_SLOTS[slotIndex], ItemBuilder.build(material, name, lore, Map.of()));
        }
    }

    private void addSummary(Inventory inventory, PlayerRewardState state, LocalDate today) {
        boolean signedToday = state.hasSignedOn(today);
        inventory.setItem(TODAY_SLOT, ItemBuilder.build(
                signedToday ? Material.LIME_DYE.name() : Material.SUNFLOWER.name(),
                signedToday ? "&a今日已签到" : "&e点击领取今日签到",
                List.of(
                        "&7日期：&f" + today,
                        "&7连续签到：&f" + state.signStreak() + " 天",
                        signedToday ? "&a今天已经签过了。" : "&e点击这里完成签到。"
                ),
                Map.of()
        ));
    }

    private String materialFor(boolean signed, boolean isToday, boolean past) {
        if (signed) {
            return Material.LIME_STAINED_GLASS_PANE.name();
        }
        if (isToday) {
            return Material.YELLOW_STAINED_GLASS_PANE.name();
        }
        if (past) {
            return Material.RED_STAINED_GLASS_PANE.name();
        }
        return Material.WHITE_STAINED_GLASS_PANE.name();
    }

    private String nameFor(int day, boolean signed, boolean isToday, boolean past) {
        if (signed) {
            return "&a" + day + " 日 已签到";
        }
        if (isToday) {
            return "&e" + day + " 日 今日可签";
        }
        if (past) {
            return "&c" + day + " 日 未签到";
        }
        return "&7" + day + " 日 未到";
    }

    private List<String> loreFor(LocalDate date, boolean signed, boolean isToday, boolean past) {
        if (signed) {
            return List.of("&7日期：&f" + date, "&a这一天已经签到。");
        }
        if (isToday) {
            return List.of("&7日期：&f" + date, "&e点击底部按钮领取今日签到。");
        }
        if (past) {
            return List.of("&7日期：&f" + date, "&c这一天没有签到记录。");
        }
        return List.of("&7日期：&f" + date, "&8还没到这一天。");
    }
}
