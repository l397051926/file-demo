package com.gennlife.fs.configurations.model;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

public enum SourceType {

    SEARCH_SERVICE,
    RWS_SERVICE,
    EMPI_SERVICE;

    public static SourceType fromString(@Nonnull String s) {
        requireNonNull(s);
        return valueOf(s.toUpperCase());
    }

}
