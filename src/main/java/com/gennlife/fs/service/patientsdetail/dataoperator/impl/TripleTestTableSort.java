package com.gennlife.fs.service.patientsdetail.dataoperator.impl;

import com.gennlife.fs.common.comparator.JsonComparatorASCByKey;
import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.patientsdetail.dataoperator.interfaces.IDataSortOperate;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.List;

/**
 * Created by Chenjinfeng on 2017/7/7.
 */
public class TripleTestTableSort implements IDataSortOperate {
    @Override
    public List<JsonElement> sort(JsonArray sources) {
        List<JsonElement> list = JsonAttrUtil.jsonArrayToList(sources);
        list = JsonAttrUtil.sort(list, new JsonComparatorASCByKey(getSortKey()));
        return list;
    }

    @Override
    public String getSortKey() {
        return QueryResult.getSortKey("triple_test_table");
    }
}
