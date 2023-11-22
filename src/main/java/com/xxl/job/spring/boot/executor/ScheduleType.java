package com.xxl.job.spring.boot.executor;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author xuxueli 2020-10-29 21:11:23
 */
@Getter
@AllArgsConstructor
public enum ScheduleType {

    NONE("无"),

    /**
     * schedule by cron
     */
    CRON("CRON"),

    /**
     * schedule by fixed rate (in seconds)
     */
    FIX_RATE("固定速度"),

    /**
     * schedule by fix delay (in seconds)， after the last time
     */
    /*FIX_DELAY(I18nUtil.getString("schedule_type_fix_delay"))*/;

    private final String title;

    public static ScheduleType match(String name, ScheduleType defaultItem){
        for (ScheduleType item: ScheduleType.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }

}