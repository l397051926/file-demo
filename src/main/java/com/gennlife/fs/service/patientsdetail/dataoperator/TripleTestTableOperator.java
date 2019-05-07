package com.gennlife.fs.service.patientsdetail.dataoperator;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Iterator;

/**
 * Created by Chenjinfeng on 2017/7/7.
 */
public class TripleTestTableOperator {
    public void transform(Iterator<JsonElement> triple_test_tables)
    {
        if(triple_test_tables==null)return;
        while (triple_test_tables.hasNext()) {
            JsonObject itemJson = triple_test_tables.next().getAsJsonObject();
            itemJson.addProperty("BLOOD_PRESSURE", JsonAttrUtil.getStringValue("SYSTOLIC", itemJson) + "/" + JsonAttrUtil.getStringValue("DIASTOLIC", itemJson));
        }
    }
}
