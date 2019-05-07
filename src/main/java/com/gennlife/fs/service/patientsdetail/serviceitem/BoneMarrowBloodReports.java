package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by Chenjinfeng on 2018/1/16.
 */
public class BoneMarrowBloodReports {
    public String getData(String param) {
        VisitSNResponse vt = new VisitSNResponse("bone_marrow_blood_tests_reports");
        vt.execute(JsonAttrUtil.toJsonObject(param));
        JsonObject result = vt.get_result();
        if (result == null) return ResponseMsgFactory.buildFailStr(vt.get_error());
        LinkedList<JsonElement> parents = JsonAttrUtil.getJsonArrayAllValue("bone_marrow_blood_tests_reports", result);
        if (parents != null) {
            for (JsonElement p : parents) {
                if (p != null && p.isJsonObject()) {
                    JsonObject pjson = p.getAsJsonObject();
                    LinkedList<JsonElement> subs = JsonAttrUtil.getJsonArrayAllValue("sub_test", pjson);
                    if (subs != null && subs.size() > 0) {
                        Map<String, LinkedList<JsonElement>> subMap = new LinkedHashMap<>();
                        for (JsonElement subItem : subs) {
                            if (subItem != null && subItem.isJsonObject()) {
                                JsonObject subJson = subItem.getAsJsonObject();
                                String key = JsonAttrUtil.getStringValue("BELONG_TO_TEST_ITEM_CN", subJson);
                                if (StringUtil.isEmptyStr(key)) {
                                    String name = JsonAttrUtil.getStringValue("CELL_CN", subJson);
                                    if (name != null) {
                                        name = name.toString();
                                        key = name;
                                    }
                                }
                                if (!StringUtil.isEmptyStr(key)) {
                                    if (!subMap.containsKey(key)) subMap.put(key, new LinkedList<>());
                                    LinkedList<JsonElement> list = subMap.get(key);
                                    list.add(subJson);
                                }
                            }
                        }
                        pjson.add("sub_test", JsonAttrUtil.toJsonTree(subMap));
                    }
                }
            }
        }
        return ResponseMsgFactory.buildResponseStr(result);
    }
}
