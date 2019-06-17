package com.gennlife.fs.common.utils;

import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.model.VisitTypeEntity;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedList;

/**
 * Created by Chenjinfeng on 2017/8/4.
 */
public class SearchTransposeUtils {
    public static QueryResult getSearchResult(QueryResult queryResult) {
         JsonArray datas = queryResult.getDatas();
        if (datas != null) {
            for (JsonElement element : datas) {
                JsonObject patient = JsonAttrUtil.getJsonObjectValue("_source", element.getAsJsonObject());
                if (patient != null) {
                    LinkedList<JsonElement> visitInfos = JsonAttrUtil.getJsonArrayAllValue("visits.visit_info", patient);
                    if (visitInfos != null) {
                        for (JsonElement visitInfo : visitInfos) {
                            JsonObject infoJson = visitInfo.getAsJsonObject();
                            if (infoJson.has("VISIT_TYPE_NAME")) continue;
                            String visitType = JsonAttrUtil.getStringValue("VISIT_TYPE", infoJson);
                            if (StringUtil.isEmptyStr(visitType)) continue;
                            if (visitType.equals(VisitTypeEntity.MZ)) {
                                infoJson.addProperty("VISIT_TYPE_NAME", "门诊");
                            } else if (visitType.equals(VisitTypeEntity.ZY)) {
                                infoJson.addProperty("VISIT_TYPE_NAME", "住院");
                            } else if (visitType.equals(VisitTypeEntity.TZ)) {
                                infoJson.addProperty("VISIT_TYPE_NAME", "体检");
                            } else infoJson.addProperty("VISIT_TYPE_NAME", visitType);
                        }
                    }
                }
            }
        }
        queryResult.setTmpdata(datas);
        return queryResult;
    }
}
