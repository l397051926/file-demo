package com.gennlife.fs.common.comparator;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.google.gson.JsonElement;

/**
 * Created by Chenjinfeng on 2017/7/7.
 */
public class JsonComparatorDESCByKey implements JsonComparatorInterface<String> {

    private String key;

    public JsonComparatorDESCByKey(String key) {
        this.key = key;
    }

    @Override
    public int compare(JsonElement o1, JsonElement o2) {
        return getValue(o2).compareTo(getValue(o1.getAsJsonObject()));
    }

    @Override
    public String getValue(JsonElement element) {
        return JsonAttrUtil.getStringValue(key, element.getAsJsonObject());
    }

}
