package com.gennlife.fs.configurations.model;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

public enum DataType {

    STRING,
    DATE,
    LONG,
    DOUBLE,
    BOOLEAN;

    public static DataType fromString(@Nonnull String s) {
        requireNonNull(s);
        return valueOf(s.toUpperCase());
    }

}
