package com.gennlife.fs.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

import static java.time.temporal.ChronoField.*;

public class DateTimeUtil {

    public static DateTimeFormatter compileTimePattern(String pattern) {
        return new DateTimeFormatterBuilder()
            .appendPattern(pattern)
            .parseDefaulting(YEAR_OF_ERA, 1)
            .parseDefaulting(MONTH_OF_YEAR, 1)
            .parseDefaulting(DAY_OF_MONTH, 1)
            .parseDefaulting(HOUR_OF_DAY, 0)
            .parseDefaulting(MINUTE_OF_HOUR, 0)
            .parseDefaulting(SECOND_OF_MINUTE, 0)
            .parseDefaulting(MILLI_OF_SECOND, 0)
            .toFormatter();
    }

    public static Date toDate(String s, DateTimeFormatter fmt) {
        return java.util.Date.from(LocalDateTime.parse(s, fmt).toInstant(ZoneOffset.UTC));
    }

}
