package com.gennlife.fs.configurations.model;

public enum DataType {

    STRING,
    DATE,
    LONG,
    DOUBLE,
    BOOLEAN;

    public static DataType fromString(String s) {
        return valueOf(s.toUpperCase());
    }

}
