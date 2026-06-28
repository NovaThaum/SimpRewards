package org.simpmc.simpRewards.data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerRewardState {
    private final UUID uuid;
    private String name;
    private long totalOnlineMinutes;
    private int dailyOnlineMinutes;
    private int weeklyOnlineMinutes;
    private LocalDate lastSeenDate;
    private LocalDate lastDailyResetDate;
    private String lastWeeklyResetWeek;
    private LocalDate signedDate;
    private int signStreak;
    private int activeStreak;
    private LocalDate lastActiveDate;
    private final Set<Integer> claimedTotalMilestones = new HashSet<>();
    private LocalDate claimedDailyDate;
    private String claimedWeeklyWeek;
    private final Set<Integer> claimedActiveStreakMilestones = new HashSet<>();
    private final Set<LocalDate> signedDates = new HashSet<>();

    public PlayerRewardState(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long totalOnlineMinutes() {
        return totalOnlineMinutes;
    }

    public void setTotalOnlineMinutes(long totalOnlineMinutes) {
        this.totalOnlineMinutes = Math.max(0, totalOnlineMinutes);
    }

    public int dailyOnlineMinutes() {
        return dailyOnlineMinutes;
    }

    public void setDailyOnlineMinutes(int dailyOnlineMinutes) {
        this.dailyOnlineMinutes = Math.max(0, dailyOnlineMinutes);
    }

    public int weeklyOnlineMinutes() {
        return weeklyOnlineMinutes;
    }

    public void setWeeklyOnlineMinutes(int weeklyOnlineMinutes) {
        this.weeklyOnlineMinutes = Math.max(0, weeklyOnlineMinutes);
    }

    public LocalDate lastSeenDate() {
        return lastSeenDate;
    }

    public void setLastSeenDate(LocalDate lastSeenDate) {
        this.lastSeenDate = lastSeenDate;
    }

    public LocalDate lastDailyResetDate() {
        return lastDailyResetDate;
    }

    public void setLastDailyResetDate(LocalDate lastDailyResetDate) {
        this.lastDailyResetDate = lastDailyResetDate;
    }

    public String lastWeeklyResetWeek() {
        return lastWeeklyResetWeek;
    }

    public void setLastWeeklyResetWeek(String lastWeeklyResetWeek) {
        this.lastWeeklyResetWeek = lastWeeklyResetWeek;
    }

    public LocalDate signedDate() {
        return signedDate;
    }

    public void setSignedDate(LocalDate signedDate) {
        this.signedDate = signedDate;
        if (signedDate != null) {
            signedDates.add(signedDate);
        }
    }

    public int signStreak() {
        return signStreak;
    }

    public void setSignStreak(int signStreak) {
        this.signStreak = Math.max(0, signStreak);
    }

    public int activeStreak() {
        return activeStreak;
    }

    public void setActiveStreak(int activeStreak) {
        this.activeStreak = Math.max(0, activeStreak);
    }

    public LocalDate lastActiveDate() {
        return lastActiveDate;
    }

    public void setLastActiveDate(LocalDate lastActiveDate) {
        this.lastActiveDate = lastActiveDate;
    }

    public Set<Integer> claimedTotalMilestones() {
        return claimedTotalMilestones;
    }

    public LocalDate claimedDailyDate() {
        return claimedDailyDate;
    }

    public void setClaimedDailyDate(LocalDate claimedDailyDate) {
        this.claimedDailyDate = claimedDailyDate;
    }

    public String claimedWeeklyWeek() {
        return claimedWeeklyWeek;
    }

    public void setClaimedWeeklyWeek(String claimedWeeklyWeek) {
        this.claimedWeeklyWeek = claimedWeeklyWeek;
    }

    public Set<Integer> claimedActiveStreakMilestones() {
        return claimedActiveStreakMilestones;
    }

    public Set<LocalDate> signedDates() {
        return signedDates;
    }

    public boolean hasSignedOn(LocalDate date) {
        return date != null && signedDates.contains(date);
    }

    public void addSignedDate(LocalDate date) {
        if (date != null) {
            signedDates.add(date);
        }
    }

    public void addOnlineMinutes(int minutes) {
        if (minutes <= 0) {
            return;
        }
        totalOnlineMinutes += minutes;
        dailyOnlineMinutes += minutes;
        weeklyOnlineMinutes += minutes;
    }
}
