package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.model.IndexChangeResultEntity;
import com.gennlife.fs.service.patientsdetail.model.TimeValueEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Chenjinfeng on 2018/3/20.
 */
public class TriListCacheForOneService extends CacheForOneService {
    private static final Logger logger = LoggerFactory.getLogger(TriListCacheForOneService.class);
    private static HashMap<String, TriListIdMapEntity> idMap = new HashMap<>();

    static {
        idMap.put("BREATH", new TriListIdMapEntity("呼吸", "BREATH","次/分",16.0,20.0));
        idMap.put("PULSE", new TriListIdMapEntity("脉搏", "PULSE","次/分",65.0,140.0));
        idMap.put("SYSTOLIC", new TriListIdMapEntity("收缩压", "SYSTOLIC","mmhg",90.0,140.0));
        idMap.put("DIASTOLIC", new TriListIdMapEntity("舒张压", "DIASTOLIC","mmhg",60.0,90.0));
        idMap.put("HEART_RATE", new TriListIdMapEntity("心率", "HEART_RATE","次/分",60.0,100.0));
        idMap.put("TEMPRATURE", new TriListIdMapEntity("体温", "TEMPRATURE","℃",36.1,37.0));
    }

    @Override
    public Map<String, JsonElement> getData(String patient_sn) {
        QueryParam qp = new QueryParam();
        qp.addsource("visits.visit_info.VISIT_SN");
        qp.addsource("visits.triple_test_table");
        qp.query_patient_sn(patient_sn);
        QueryResult result = HttpRequestUtils.search(qp);
        JsonObject json = result.getDataDetail();
        if (json == null || JsonAttrUtil.isEmptyJsonElement(json.get("visits"))) return null;
        JsonArray visits = json.getAsJsonArray("visits");
        Map<String, JsonElement> data = new HashMap<>();
        Map<String, TreeSet<TriListIdMapEntity>> visitMap = new HashMap<>();
        Set<TriListIdMapEntity> allSet = new TreeSet<>();
        Map<String, TreeSet<TimeValueEntity>> countDataMap = new HashMap<>();
        for (JsonElement visitElem : visits) {
            JsonObject visitJson = visitElem.getAsJsonObject();
            if (JsonAttrUtil.isEmptyJsonElement(visitJson.get("triple_test_table"))) continue;
            String visit_sn = JsonAttrUtil.getStringValue("visit_info.VISIT_SN", visitJson);
            JsonArray triple_test_table = visitJson.getAsJsonArray("triple_test_table");
            for (JsonElement item : triple_test_table) {
                JsonObject itemJson = item.getAsJsonObject();
                String date = JsonAttrUtil.getStringValue("EXAM_TIME", itemJson);
                if (StringUtil.isEmptyStr(date)) continue;
                for (Map.Entry<String, TriListIdMapEntity> idMapItem : idMap.entrySet()) {
                    String key = idMapItem.getKey();
                    if (itemJson.has(key)) {
                        try {
                            double tmpValue = itemJson.get(key).getAsDouble();
                            if (tmpValue == 0) continue;
                            TimeValueEntity timeValueEntity = new TimeValueEntity(date, tmpValue);
                            if (!countDataMap.containsKey(key)) countDataMap.put(key, new TreeSet<>());
                            countDataMap.get(key).add(timeValueEntity);
                            if (!visitMap.containsKey(visit_sn)) visitMap.put(visit_sn, new TreeSet<>());
                            visitMap.get(visit_sn).add(idMapItem.getValue());
                            allSet.add(idMapItem.getValue());
                        } catch (Exception e) {
                            logger.error("", e);
                        }
                    }
                }

            }
        }
        if (allSet.size() == 0) return null;
        HashMap<String, IndexChangeResultEntity> idValueResultMap = new HashMap<>();
        for (Map.Entry<String, TreeSet<TimeValueEntity>> item : countDataMap.entrySet()) {
            IndexChangeResultEntity indexChangeResultEntity = new IndexChangeResultEntity();
            setTimeAndValue(indexChangeResultEntity, item.getValue());
            TriListIdMapEntity configItem = idMap.get(item.getKey());
            indexChangeResultEntity.setInterval_max(configItem.getMax_value());
            indexChangeResultEntity.setInterval_min(configItem.getMin_value());
            indexChangeResultEntity.setUnit(configItem.getUnit());
            indexChangeResultEntity.setName(configItem.getCn());
            idValueResultMap.put(item.getKey(), indexChangeResultEntity);
        }
        data.put(ID_VALUE_KEY, JsonAttrUtil.toJsonTree(idValueResultMap));
        data.put(ALL_KEY, JsonAttrUtil.toJsonTree(allSet));
        visitMap.entrySet().forEach(item -> data.put(item.getKey(), JsonAttrUtil.toJsonTree(item.getValue())));
        return data;
    }

    @Override
    protected JsonArray searchByKey(JsonArray array, String key) {
        LinkedList<TriListIdMapEntity> list=new LinkedList<>();
        for(JsonElement item:array)
        {
            TriListIdMapEntity tmp= JsonAttrUtil.fromJson(item,TriListIdMapEntity.class);
            if(tmp.getCn().contains(key)) list.add(tmp);
        }
        if(list==null||list.size()==0)return null;
        return JsonAttrUtil.toJsonTree(list).getAsJsonArray();
    }

    @Override
    protected Object getLock() {
        return logger;
    }

    @Override
    public String getCacheKey(String patient_sn) {
        return "TriList_" + patient_sn;
    }
}
