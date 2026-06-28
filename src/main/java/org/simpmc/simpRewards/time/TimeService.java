package org.simpmc.simpRewards.time;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class TimeService {
    private ZoneId zoneId;

    public TimeService(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public ZonedDateTime now() {
        return ZonedDateTime.now(zoneId);
    }

    public LocalDate today() {
        return now().toLocalDate();
    }
}
