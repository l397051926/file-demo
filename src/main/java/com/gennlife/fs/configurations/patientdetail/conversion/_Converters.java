package com.gennlife.fs.configurations.patientdetail.conversion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.gennlife.darren.controlflow.exception.Force.force;
import static com.gennlife.darren.controlflow.exception.Try.try_;
import static com.gennlife.fs.configurations.patientdetail.conversion._EMRDataType.*;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static java.util.Arrays.asList;

class _Converters {

    public static final Map<_EMRDataType, Function<Object, Comparable>> DEFAULT_CONVERTERS = force(() -> {
        Map<_EMRDataType, Function<Object, Comparable>> ret = new HashMap<>();
        ret.put(STRING, _Converters::_toRawString);
        ret.put(DATE, _Converters::_toDate);
        ret.put(LONG, _Converters::_toLong);
        ret.put(DOUBLE, _Converters::_toDouble);
        ret.put(BOOLEAN, _Converters::_toBoolean);
        return ret;
    });

    static String _toRawString(Object object) {
        if (object instanceof Date) {
            return ((Date)object).toInstant().atOffset(ZoneOffset.UTC).toLocalDate().format(_DATE_FORMATTERS.get(0));
        } else if (object instanceof Boolean) {
            return TRUE.equals(object) ? "是" : "否";  // TODO: should be depending on field definition
        } else if (object instanceof Float || object instanceof Double) {
            return new BigDecimal(((Number)object).doubleValue()).toPlainString();
        }
        // https://stackoverflow.com/questions/37413106/java-lang-nullpointerexception-is-thrown-using-a-method-reference-but-not-a-lamb
        // noinspection Convert2MethodRef
        return try_(() -> object.toString()).orElse(null);
    }

    static String _toRawString(Object object, DateTimeFormatter dateFormatter) {
        if (object instanceof Date) {
            try {
                return ((Date)object).toInstant().atOffset(ZoneOffset.UTC).toLocalDate().format(dateFormatter);
            } catch (Exception ignore) {}
        }
        return _toRawString(object);
    }

    static Date _toDate(Object object) {
        if (object instanceof Date) {
            return (Date)object;
        } else if (object instanceof String) {
            for (DateTimeFormatter fmt: _DATE_FORMATTERS) {
                try {
                    return Date.from(LocalDateTime.parse((String)object, fmt).toInstant(ZoneOffset.UTC));
                } catch (DateTimeParseException ignored) {}
            }
            return null;
        } else if (object instanceof Number) {
            return try_(() -> Date.from(Instant.ofEpochMilli(((Number)object).longValue()))).orElse(null);
        }
        return null;
    }

    static Date _toDate(Object object, DateTimeFormatter dateFormatter) {
        if (object instanceof String) {
            try {
//                TemporalAccessor t = dateFormatter.parse("1964");
//                t.ge
//                Optional<Integer> year = try_(() -> t.get(YEAR));
//                Optional<Integer> month = try_(() -> t.get(MONTH_OF_YEAR)).orElse(1);
//                Optional<Integer> date = try_(() -> t.get(DAY_OF_MONTH)).orElse(1);
//                Optional<Integer> hours = try_(() -> t.get())
//                int year, int month, int date, int hrs, int min, int sec)
//                new Date()
                return Date.from(LocalDateTime.parse((String)object, dateFormatter).atOffset(ZoneOffset.UTC).toInstant());
            } catch (Exception ignore) {}
        }
        return _toDate(object);
    }

    static Long _toLong(Object object) {
        if (object instanceof Number) {
            return ((Number)object).longValue();
        } else if (object instanceof String) {
            try {
                return parseLong((String)object);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    static Double _toDouble(Object object) {
        if (object instanceof Number) {
            return ((Number)object).doubleValue();
        } else if (object instanceof String) {
            try {
                return parseDouble((String)object);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    static Boolean _toBoolean(Object object) {
        if (object instanceof Boolean) {
            return (boolean)object;
        } else if (object instanceof Number) {
            return ((Number)object).doubleValue() != 0;
        } else if (object instanceof String) {
            String s = (String)object;
            if ("是".equals(s)) {
                return true;  // TODO: should be depending on field definition
            }
            if ("否".equals(s)) {
                return false;  // TODO: should be depending on field definition
            }
            try {
                return parseDouble(s) != 0;
            } catch (NumberFormatException e) {
                return parseBoolean(s);
            }
        }
        return null;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static final List<DateTimeFormatter> _DATE_FORMATTERS = asList(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy-MM"),
        DateTimeFormatter.ofPattern("yyyy"));

}
