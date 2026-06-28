package org.simpmc.simpRewards.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public record WeekKey(LocalDate startDate) implements Comparable<WeekKey> {
    public static WeekKey from(LocalDate date, DayOfWeek startDay) {
        LocalDate start = date.with(TemporalAdjusters.previousOrSame(startDay));
        return new WeekKey(start);
    }

    public static WeekKey parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new WeekKey(LocalDate.parse(value));
        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    public String toString() {
        return startDate.toString();
    }

    @Override
    public int compareTo(WeekKey other) {
        return startDate.compareTo(other.startDate);
    }
}
