package com.gennlife.fs.common.comparator;

import com.gennlife.fs.common.utils.StringUtil;
import com.google.gson.JsonElement;

import java.util.Comparator;

/**
 * Created by Chenjinfeng on 2016/12/9.
 */
public class JsonVisitDateDescComparator implements Comparator<JsonElement> {
        @Override
        public int compare(JsonElement o1, JsonElement o2) {
            return StringUtil.get_visit_date(o2.getAsJsonObject()).compareTo(StringUtil.get_visit_date(o1.getAsJsonObject()));
        }

}
