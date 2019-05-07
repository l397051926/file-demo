package com.gennlife.fs.common.configurations;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public enum HeaderType {

    TREE(1),
    FLAT(2);

    HeaderType(int value) {
        _value = value;
    }

    public int value() {
        return _value;
    }

    public static HeaderType withValue(int value) {
        return _enums.get(value);
    }

    private int _value;

    private static Map<Integer, HeaderType> _enums = Stream.of(HeaderType.values())
        .collect(toMap(HeaderType::value, identity()));

}
