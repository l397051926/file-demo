package com.gennlife.fs.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.gennlife.darren.util.GenericTypeConverters;
import lombok.val;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachValue;
import static java.util.stream.Collectors.toList;

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
        return DT(o, null);
    }

    public static java.util.Date DT(Object o, DateTimeFormatter defaultFormatter) {
        if (o == null) {
            return null;
        }
        if (o instanceof java.util.Date) {
            return (java.util.Date)o;
        }
        val s = o.toString();
        if (defaultFormatter != null) {
            try {
                return DateTimeUtil.toDate(s, defaultFormatter);
            } catch (DateTimeParseException ignored) {}
        }
        for (val fmt: DATE_FORMATTERS) {
            try {
                return DateTimeUtil.toDate(s, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Stream
        .of("yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy-MM",
            "yyyy")
        .map(DateTimeUtil::compileTimePattern)
        .collect(toList());

}
