package com.gennlife.fs.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.gennlife.darren.util.GenericTypeConverters;
import lombok.val;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachValue;
import static java.util.Arrays.asList;

public class TypeUtil {

    public static Integer I(Object o) {
        return GenericTypeConverters.toInteger(o);
    }

    public static Long L(Object o) {
        if (o instanceof Timestamp) {
            return ((Timestamp)o).getTime();
        }
        return GenericTypeConverters.toLong(o);
    }

    public static Boolean B(Object o) {
        return GenericTypeConverters.toBoolean(o);
    }

    public static boolean BV(Object o) {
        return GenericTypeConverters.toBooleanValue(o);
    }

    public static String S(Object o) {
        return GenericTypeConverters.toString(o);
    }

    public static String S1(Object o) {
        if (o == null) {
            return "-";
        }
        if (o instanceof JSONArray) {
            val ret = new ArrayList<String>();
            foreachValue(o, e -> ret.add(String.valueOf(e)));
            return String.join(";", ret);
        }
        return S(o);
    }

    public static Float F(Object o) {
        return GenericTypeConverters.toFloat(o);
    }

    public static java.util.Date DT(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof java.util.Date) {
            return (java.util.Date)o;
        }
        val s = o.toString();
        for (val fmt: DATE_FORMATTERS) {
            try {
                return java.util.Date.from(LocalDateTime.parse(s, fmt).toInstant(ZoneOffset.UTC));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static final List<DateTimeFormatter> DATE_FORMATTERS = asList(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy-MM"),
        DateTimeFormatter.ofPattern("yyyy"));

}
