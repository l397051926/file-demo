package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.MapUtility;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.gennlife.fs.service.patientsdetail.model.Visit;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by zhangshijian on 2016/7/19.
 * 基本统计图形
 */
public class VisitCountByDimension extends PatientDetailService {
    public String getVisitCountByDimension(String param) {
        String patient_sn = null;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null) {
            return ResponseMsgFactory.buildFailStr(" not json");
        }
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
//        Connection instance = HbaseConnections.instance.getInstance(BeansContextUtil.getZkConfig().getZkUrls(), BeansContextUtil.getZkConfig().getZkPort());
//        Result dataByRowKey = HbaseUtil.getDataByRowKey(instance, BeansContextUtil.getHbaseCountTableConfig().getBaseCountTableName(), patient_sn, BeansContextUtil.getHbaseCountTableConfig().getBaseCountCFName(), new String[]{BeansContextUtil.getHbaseCountTableConfig().getBaseCountStatistics()});
//        Result dataByRowKey = "";
//        byte[] value = dataByRowKey.getValue(Bytes.toBytes(BeansContextUtil.getHbaseCountTableConfig().getBaseCountCFName()), Bytes.toBytes(BeansContextUtil.getHbaseCountTableConfig().getBaseCountStatistics()));
//        String res = Bytes.toString(value);
        String res = HttpRequestUtils.getStatistics(patient_sn);
        JsonElement result = JsonAttrUtil.toJsonElement(res);
        return ResponseMsgFactory.buildSuccessStr((JsonObject) result);
    }

    private void count_frequency(HashMap<String, Long> visit_type_map, TreeMap<String, Long> visit_year_map, HashMap<String, Long> disease_type_map, HashMap<String, Long> department_map, HashMap<String, Long> event_map, JsonArray visits) {
        for (JsonElement elem : visits) {
            JsonObject visit = elem.getAsJsonObject();
            Visit vst_data = new Visit(visit);
            //将 -  修改为 未知
            String date = vst_data.get_visit_date_year();
            if("-".equals(date)){
                date = "未知";
            }
            MapUtility.increaseMapKeyCount(visit_type_map, vst_data.get_visit_type());
            MapUtility.increaseMapKeyCount(department_map, vst_data.get_visit_dept());
            MapUtility.increaseMapKeyCount(visit_year_map, date);
            MapUtility.increaseMapKeyCount(disease_type_map, vst_data.get_disease());
            for (int i = 0; i < vst_data.get_visit_event().size(); i++) {
                String value = vst_data.get_visit_event_item(i);
                if (!"".equals(value))
                    MapUtility.increaseMapKeyCount(event_map, value);
            }
        }
    }

}
