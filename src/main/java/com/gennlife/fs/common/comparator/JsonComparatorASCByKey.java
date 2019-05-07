package com.gennlife.fs.common.comparator;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.google.gson.JsonElement;

/**
 * Created by Chenjinfeng on 2016/12/9.
 */
public class JsonComparatorASCByKey implements JsonComparatorInterface<String> {
    private  String key;

    public JsonComparatorASCByKey(String key) {
        this.key = key;
    }

    @Override
    public int compare(JsonElement o1, JsonElement o2) {
        return getValue(o1).compareTo(getValue(o2.getAsJsonObject()));
    }

    @Override
    public String getValue(JsonElement element) {
        return JsonAttrUtil.getStringValue(key,element.getAsJsonObject());
    }
}
