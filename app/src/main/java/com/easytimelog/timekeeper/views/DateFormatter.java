package com.easytimelog.timekeeper.views;

import org.joda.time.DateTime;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * Created by James Burnett on 5/4/2014.
 */
public class DateFormatter {
    public static final PeriodFormatter DEFAULT;
    static {
        DEFAULT = new PeriodFormatterBuilder()
                .printZeroRarelyLast()
                .appendYears()
                .appendSuffix(" year", " years")
                .appendSeparator(" and ")
                .appendMonths()
                .appendSuffix(" month", " months")
                .appendSeparator(", ")
                .appendDays()
                .appendSuffix(" day", " days")
                .appendSeparator(", ")
                .appendHours()
                .appendSuffix(" hour", " hours")
                .appendSeparator(", ")
                .appendMinutes()
                .appendSuffix(" minute", " minutes")
                .appendSeparator(", ")
                .appendSeconds()
                .appendSuffix(" second", " seconds")
                .toFormatter();
    }

    public static final String getHumanFriendlyDate(DateTime dateTime) {
        return dateTime.toString();
    }
}
