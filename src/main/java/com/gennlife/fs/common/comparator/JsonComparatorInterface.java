package com.gennlife.fs.common.comparator;

import com.google.gson.JsonElement;

import java.util.Comparator;

/**
 * Created by Chenjinfeng on 2017/6/21.
 */
public interface JsonComparatorInterface<T> extends Comparator<JsonElement> {
    T getValue(JsonElement element);
}
