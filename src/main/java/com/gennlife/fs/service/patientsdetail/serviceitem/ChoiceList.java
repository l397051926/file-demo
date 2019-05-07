package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.choicelist.ChoiceListABS;
import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.cache.OnePatAccessCache;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mohaowen on 2016/7/19.
 */
public class ChoiceList extends PatientDetailService {
    private static Map<Integer, CacheForOneService> serviceMap = new HashMap<>();
    public static final Integer TRI = 1;
    public static final Integer SUB_INSPECTION = 2;

    static {
        //三测单与医院无关直接写到代码里
        serviceMap.put(TRI, new TriListCacheForOneService());
        //实验室检验与医院原始数据有关需要额外的配置
        serviceMap.put(SUB_INSPECTION, new ChoiceListCacheForOneService());
    }

    private static final Logger logger = LoggerFactory.getLogger(ChoiceList.class);


    /**
     * 分类详情-指标变化——指标列表
     * <p>
     * UI/detail/ChoiceList
     * param: patient_sn
     * 所有检验指标的中文名称列表 {[SUB_INSPECTION_CN]}
     */
    @Deprecated
    public String getChoiceListOld(String param) {
        String patient_sn = null;
        final JsonArray choice_array = new JsonArray();

        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (JsonAttrUtil.has_key(param_json, "patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }

        if (StringUtils.isEmpty(patient_sn)) {
            return ResponseMsgFactory.buildFailStr("no patient_sn");
        }
        JsonObject result = new JsonObject();
        QueryParam qp = new QueryParam(param_json, patient_sn, "visits.inspection_reports.sub_inspection.SUB_INSPECTION_CN");
        JsonArray vist_jason = get_visits(qp);
        ChoiceListABS<JsonArray> choice = new ChoiceListABS<JsonArray>() {
            @Override
            protected boolean visit_filter(JsonObject one_visit_json) {
                return false;
            }

            @Override
            protected boolean inspection_reports_filter(JsonObject one_report_jason) {
                return false;
            }

            @Override
            protected boolean countItem(JsonObject one_sub_report) {
                if (one_sub_report.has("SUB_INSPECTION_CN")) {
                    if (!choice_array.contains(one_sub_report.get("SUB_INSPECTION_CN"))) {
                        choice_array.add(one_sub_report.get("SUB_INSPECTION_CN"));
                    }
                }
                return true;
            }
        };
        choice.count(vist_jason);
        result.add("physical_examination_list", choice_array);
        return ResponseMsgFactory.buildSuccessStr(result);
    }

    public String getChoiceList(String param, int type) {
        CacheForOneService cacheForOneService = serviceMap.get(type);
        String patient_sn = null;
        String visit_sn = ChoiceListCacheForOneService.ALL_KEY;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (JsonAttrUtil.has_key(param_json, "patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        if (JsonAttrUtil.has_key(param_json, "visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }
        String key = JsonAttrUtil.getStringValue("key", param_json);
        if (StringUtils.isEmpty(patient_sn)) {
            return ResponseMsgFactory.buildFailStr("no patient_sn");
        }
        if (!OnePatAccessCache.hasAccess(patient_sn, param_json))
            return ResponseMsgFactory.buildFailStr("no data or no roles");
        JsonElement data = cacheForOneService.getMatch(patient_sn, visit_sn,key);
        if (data == null) return ResponseMsgFactory.buildFailStr("no data");
        JsonObject result = new JsonObject();
        result.add("data", data);
        return ResponseMsgFactory.buildSuccessStr(result);
    }

    public String getItemInfo(String param, int type) {
        CacheForOneService cacheForOneService = serviceMap.get(type);
        String patient_sn = null;
        String id = null;
        String start_date = null;
        String end_date = null;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null) return ResponseMsgFactory.buildFailStr(" not json");
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        if (param_json.has("id")) {
            id = JsonAttrUtil.getStringValue("id", param_json);
        }
        if (param_json.has("start_date")) {
            start_date = JsonAttrUtil.getStringValue("start_date", param_json);
        }
        if (param_json.has("end_date")) {
            end_date = JsonAttrUtil.getStringValue("end_date", param_json);
        }
        if (StringUtil.isEmptyStr(id)) return ResponseMsgFactory.buildFailStr("错误id");
        if (StringUtil.isEmptyStr(patient_sn))
            return ResponseMsgFactory.buildFailStr("patient_sn 为空");
        boolean access = OnePatAccessCache.hasAccess(patient_sn, param_json);
        if (!access) return ResponseMsgFactory.buildFailStr("no data or no roles");
        JsonObject data = cacheForOneService.getIdList(patient_sn, String.valueOf(id), start_date, end_date);
        if (data == null) return ResponseMsgFactory.buildFailStr("no data");
        return ResponseMsgFactory.buildSuccessStr(data);
    }

}
