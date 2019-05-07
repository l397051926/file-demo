package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.model.IndexChangeResultEntity;
import com.gennlife.fs.service.patientsdetail.model.InspectionPResult;
import com.gennlife.fs.service.patientsdetail.model.TimeValueEntity;
import com.gennlife.fs.system.bean.DataBean;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Chenjinfeng on 2018/3/19.
 * no roles
 * 2018-03-28 改用样本名称，不用检验大项名称。 INSPECTION_NAME 变成 SPECIMEN_NAME。
 */
public class ChoiceListCacheForOneService extends CacheForOneService {
    private static final Logger logger = LoggerFactory.getLogger(ChoiceListCacheForOneService.class);

    private String getPKey() {
        return "SPECIMEN_NAME";
    }

    protected final TreeMap<Integer, IndexChangeResultEntity> exchangeIdValueMap(Map<Integer, TreeSet<TimeValueEntity>> idValueMap) {
        TreeMap<Integer, IndexChangeResultEntity> result = new TreeMap<>();
        JsonObject idStdMap = DataBean.getIdMap();
        for (Map.Entry<Integer, TreeSet<TimeValueEntity>> item : idValueMap.entrySet()) {
            JsonElement valueElem = idStdMap.get(String.valueOf(item.getKey()));
            if (valueElem == null || valueElem.isJsonNull()) continue;
            JsonObject valueJson = valueElem.getAsJsonObject();
            String name = JsonAttrUtil.getStringValue("stdSubName", valueJson);
            IndexChangeResultEntity resultItem = JsonAttrUtil.fromJson(valueJson, IndexChangeResultEntity.class);
            TreeSet<TimeValueEntity> dataList = item.getValue();
            setTimeAndValue(resultItem, dataList);
            resultItem.setName(name);
            result.put(item.getKey(), resultItem);
        }
        return result;
    }


    @Override
    public Map<String, JsonElement> getData(String patient_sn) {
        QueryParam queryParam = new QueryParam();
        queryParam.ignoreRoles();
        queryParam.query_patient_sn(patient_sn);
        queryParam.addsource("visits.inspection_reports",
                "visits.visit_info.VISIT_SN",
                "visits.visit_info.ADMISSION_DATE",
                "visits.visit_info.REGISTERED_DATE");
        QueryResult result = HttpRequestUtils.search(queryParam);
        JsonObject json = result.getDataDetail();
        if (json == null || !json.has("visits")) return null;
        JsonArray array = json.getAsJsonArray("visits");
        if (array.size() == 0) return null;
        Map<String, TreeSet<Integer>> visitMap = new LinkedHashMap<>();
        TreeSet<Integer> allIds = new TreeSet<>();
        TreeMap<Integer, TreeSet<TimeValueEntity>> idValueMap = new TreeMap<>();
        JsonObject originMap = DataBean.getOriginMap();
        for (JsonElement visitItem : array) {
            JsonObject visitJson = visitItem.getAsJsonObject();
            if (!visitJson.has("inspection_reports")) continue;
            String visit_sn = JsonAttrUtil.getStringValue("visit_info.VISIT_SN", visitJson);
            JsonArray inspection_reports = visitJson.getAsJsonArray("inspection_reports");
            for (JsonElement inspection_report : inspection_reports) {
                JsonObject inspectionJson = inspection_report.getAsJsonObject();
                String pName = JsonAttrUtil.getStringValue(getPKey(), inspectionJson);
                String date = StringUtil.get_visit_date(visitJson);
                if (StringUtil.isEmptyStr(date)) {
                    continue;
                }
                //锦州数据抽错了
                if (StringUtil.isEmptyStr(pName) || !originMap.has(pName)) {
                    pName = JsonAttrUtil.getStringValue("SPECIMEN_TYPE", inspectionJson);
                }

                if (StringUtil.isEmptyStr(pName)) continue;
                if (originMap.has(pName) && inspectionJson.has("sub_inspection")) {
                    String reportTime = JsonAttrUtil.getStringValue("REPORT_TIME", inspectionJson);
                    if(StringUtil.isEmptyStr(reportTime)){
                        continue;
                    }
                    JsonObject matchSubName = originMap.getAsJsonObject(pName);
                    JsonArray subArrays = inspectionJson.getAsJsonArray("sub_inspection");
                    for (JsonElement subItem : subArrays) {
                        JsonObject subItemJson = subItem.getAsJsonObject();
                        String subName = JsonAttrUtil.getStringValue("SUB_INSPECTION_CN", subItemJson);
                        if (StringUtil.isEmptyStr(subName)) continue;
                        if (!matchSubName.has(subName)) continue;
                        JsonObject unitJson = matchSubName.getAsJsonObject(subName);
                        String unit = JsonAttrUtil.getStringValue("SUB_INSPECTION_UNIT", subItemJson);
                        if (unit == null) unit = "";
                        String unit2Upper = unit.toUpperCase();
                        if (!unitJson.has(unit2Upper)) continue;
                        try {
                            double value = Double.valueOf(JsonAttrUtil.getStringValue("SUB_INSPECTION_RESULT_NUMBER", subItemJson));
                            int matchId = unitJson.get(unit2Upper).getAsInt();
                            TimeValueEntity entity = new TimeValueEntity(reportTime, value);
                            if (!idValueMap.containsKey(matchId)) idValueMap.put(matchId, new TreeSet<>());
                            idValueMap.get(matchId).add(entity);
                            if (!visitMap.containsKey(visit_sn)) visitMap.put(visit_sn, new TreeSet<>());
                            visitMap.get(visit_sn).add(matchId);
                            allIds.add(matchId);
                        } catch (Exception e) {
                            logger.info("检验子项数值解析问题？ ： "+e.getMessage());
                            continue;
                        }

                    }
                }

            }
        }
        if (allIds.size() == 0) return null;
        Map<String, JsonElement> returnValue = new HashMap<>();
        TreeMap<Integer, IndexChangeResultEntity> idValueResultMap = exchangeIdValueMap(idValueMap);
        returnValue.put(ID_VALUE_KEY, JsonAttrUtil.toJsonObject(idValueResultMap));
        returnValue.put(ALL_KEY, JsonAttrUtil.toJsonTree(exchageForPResult(allIds)));
        for (Map.Entry<String, TreeSet<Integer>> item : visitMap.entrySet()) {
            returnValue.put(item.getKey(), JsonAttrUtil.toJsonTree(exchageForPResult(item.getValue())));
        }
        return returnValue;
    }

    @Override
    public JsonArray searchByKey(JsonArray array, String key) {
        LinkedList<InspectionPResult> result = new LinkedList<>();
        for (JsonElement item : array) {
            InspectionPResult p = JsonAttrUtil.fromJson(item, InspectionPResult.class);
            if (p.search(key)) result.add(p);
        }
        if (result.size() == 0) return null;
        return JsonAttrUtil.toJsonTree(result).getAsJsonArray();
    }

    private List<InspectionPResult> exchageForPResult(Collection<Integer> ids) {
        List<InspectionPResult> result = new LinkedList<>();
        TreeMap<String, LinkedList<InspectionPResult.Inner>> tmpMap = new TreeMap<>();
        JsonObject idMap = DataBean.getIdMap();
        for (Integer item : ids) {
            JsonElement match = idMap.get(String.valueOf(item));
            if (match == null || match.isJsonNull()) continue;
            JsonObject matchJson = match.getAsJsonObject();
            String stdSubName = JsonAttrUtil.getStringValue("stdSubName", matchJson);
            String stdPName = JsonAttrUtil.getStringValue("stdPName", matchJson);
            InspectionPResult.Inner inner = new InspectionPResult.Inner();
            inner.setId(item);
            inner.setName(stdSubName);
            if (!tmpMap.containsKey(stdPName)) tmpMap.put(stdPName, new LinkedList<>());
            tmpMap.get(stdPName).add(inner);
        }
        for (Map.Entry<String, LinkedList<InspectionPResult.Inner>> item : tmpMap.entrySet()) {
            InspectionPResult p = new InspectionPResult();
            p.setpName(item.getKey());
            p.setSub(item.getValue());
            result.add(p);
        }
        return result;
    }


    public String getCacheKey(String patient_sn) {
        return "ChoiceList_" + patient_sn;
    }

    @Override
    protected Object getLock() {
        return logger;
    }
}
