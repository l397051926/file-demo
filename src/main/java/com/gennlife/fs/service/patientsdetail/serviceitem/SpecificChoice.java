package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.comparator.StringComparator;
import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Administrator on 2016/7/19.
 */
public class SpecificChoice extends PatientDetailService {
    private static final Logger logger = LoggerFactory.getLogger(SpecificChoice.class);
    private static ChoiceListCacheForOneService choiceListCacheForOneService = new ChoiceListCacheForOneService();

    @Deprecated
    public String getSpecificChoice(String param) {
        String patient_sn = null;
        String exam_name_cn = null;
        String start_date = null;
        String end_date = null;
        String visit_sn = null;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null) return ResponseMsgFactory.buildFailStr(" not json");

        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        if (param_json.has("exam_name_cn")) {
            exam_name_cn = param_json.get("exam_name_cn").getAsString();
        }
        if (param_json.has("start_date")) {
            start_date = param_json.get("start_date").getAsString();
        }
        if (param_json.has("end_date")) {
            end_date = param_json.get("end_date").getAsString();
        }
        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }
        JsonObject result = new JsonObject();
        if (StringUtils.isEmpty(patient_sn)) {
            return ResponseMsgFactory.buildFailStr(" empty patient_sn");
        }
        QueryParam qp = new QueryParam(param_json, patient_sn, "visits.inspection_reports");
        JsonArray vist_jason = new JsonArray();
        if (null == visit_sn) {
            qp.query_patient_sn(patient_sn);
            vist_jason = get_visits(qp);
            if (vist_jason == null) {
                return ResponseMsgFactory.buildFailStr(" no visit");
            }
        } else {
            JsonObject visit = get_visit(qp, visit_sn, patient_sn);
            if (visit != null) {
                vist_jason.add(visit);
            } else {
                return ResponseMsgFactory.buildFailStr("no visit");
            }

        }

        Map<String, JsonObject> report_map = new HashMap<>();
        Map<String, TreeMap<String, JsonObject>> order_list = new HashMap<>();
        JsonArray data_array = new JsonArray();

        for (JsonElement one_visit : vist_jason) {
            JsonObject one_visit_json = one_visit.getAsJsonObject();
            JsonArray all_reports = null;
            if (one_visit_json.has("inspection_reports")) {
                all_reports = one_visit_json.get("inspection_reports").getAsJsonArray();
            }
            if (all_reports == null) {
                continue;
            }

            for (JsonElement one_report : all_reports) {  //对每个检验大项
                JsonObject one_report_jason = one_report.getAsJsonObject();
                JsonArray all_sub_reports = null;

                if (one_report_jason.has("sub_inspection")) { //获取所有检验子项
                    all_sub_reports = one_report_jason.get("sub_inspection").getAsJsonArray();
                } else {
                    continue;
                }

                for (JsonElement sub_report_elem : all_sub_reports) { //每个检验子项
                    JsonObject one_sub_report = sub_report_elem.getAsJsonObject();

                    if (one_sub_report.has("SUB_INSPECTION_CN") && report_map.keySet().contains(one_sub_report.get("SUB_INSPECTION_CN").getAsString())) {
                        JsonObject map_obj = report_map.get(one_sub_report.get("SUB_INSPECTION_CN").getAsString());
                        //如果已经存在
                        logger.debug("SpecificChoice(): match an array item " + map_obj.toString());

                        if (exam_name_cn == null || exam_name_cn.isEmpty() ||
                                one_sub_report.get("SUB_INSPECTION_CN").getAsString().trim().equals(exam_name_cn)
                                        && compareTime(one_report_jason, start_date, end_date)) { //判断是否满足加入条件

                            if (one_sub_report.has(getSubInspectionResultKey()) && one_report_jason.has("ORDER_TIME")) {

                                TreeMap<String, JsonObject> order_map = order_list.get(one_sub_report.get("SUB_INSPECTION_CN").getAsString());
                                JsonObject order_obj = new JsonObject();
                                try {
                                    double result_value = Double.parseDouble(one_sub_report.get(getSubInspectionResultKey()).getAsString());
                                    order_obj.addProperty(one_report_jason.get("ORDER_TIME").getAsString(),
                                            result_value);
                                    order_map.put(one_report_jason.get("ORDER_TIME").getAsString(), order_obj);
                                } catch (NumberFormatException nfe) {
                                    order_obj.addProperty(one_report_jason.get("ORDER_TIME").getAsString(),
                                            one_sub_report.get(getSubInspectionResultKey()).getAsString());
                                    order_map.put(one_report_jason.get("ORDER_TIME").getAsString(), order_obj);
                                }
                            } // of if(one_sub_report.has("SUB_INSPECTION_RESULT")
                        } //满足加入条件
                    } else { //不存在，则创建新项
                        logger.debug("LabResultItem(): no item found ...  ");
                        if (exam_name_cn == null || exam_name_cn.isEmpty() ||
                                one_sub_report.get("SUB_INSPECTION_CN").getAsString().trim().equals(exam_name_cn)
                                        && compareTime(one_report_jason, start_date, end_date)) {

                            JsonObject new_item = new JsonObject();

                            if (one_sub_report.has("SUB_INSPECTION_CN")) {
                                new_item.addProperty("item_type_name", one_sub_report.get("SUB_INSPECTION_CN").getAsString());
                            } else {
                                logger.debug("SecificChoice() no sub_inspection name");
                                continue;
                            }

                            if (one_sub_report.has("SUB_INSPECTION_UNIT")) {
                                new_item.addProperty("item_result_unit", one_sub_report.get("SUB_INSPECTION_UNIT").getAsString());
                            } else {
                                new_item.addProperty("item_result_unit", "-");
                            }

                            TreeMap<String, JsonObject> order_map = new TreeMap<String, JsonObject>(new StringComparator());
                            if (one_sub_report.has(getSubInspectionResultKey()) && one_report_jason.has("ORDER_TIME")) {
                                JsonObject order_obj = new JsonObject();
                                if (one_sub_report.has("SUB_INSPECTION_UNIT")) {
                                    double result_value = 0;
                                    try {
                                        result_value = Double.parseDouble(one_sub_report.get(getSubInspectionResultKey()).getAsString());
                                        order_obj.addProperty(one_report_jason.get("ORDER_TIME").getAsString(),
                                                result_value);
                                    } catch (NumberFormatException nfe) {
                                        order_obj.addProperty(one_report_jason.get("ORDER_TIME").getAsString(),
                                                one_sub_report.get(getSubInspectionResultKey()).getAsString());
                                    }
                                } else {
                                    order_obj.addProperty(one_report_jason.get("ORDER_TIME").getAsString(),
                                            one_sub_report.get(getSubInspectionResultKey()).getAsString());
                                }
                                order_map.put(one_report_jason.get("ORDER_TIME").getAsString(), order_obj);
                            }
                            report_map.put(one_sub_report.get("SUB_INSPECTION_CN").getAsString(), new_item);
                            order_list.put(one_sub_report.get("SUB_INSPECTION_CN").getAsString(), order_map);
                        } // 判断加入条件
                    } // else 是否存在
                } // for(JsonElement sub_report_elem
            } //对每个检验大项
        }//对每个visits

        for (String key : report_map.keySet()) {
            JsonObject data_item = report_map.get(key);
            JsonArray order_array = new JsonArray();
            TreeMap<String, JsonObject> order_map = order_list.get(key);
            for (String date : order_map.keySet()) {
                order_array.add(order_map.get(date));
            }
            data_item.add("orders", order_array);
            data_array.add(data_item);
        }

        result.add("physical_examination", data_array);
        return ResponseMsgFactory.buildSuccessStr(result);
    }



    private String getSubInspectionResultKey() {
        return "SUB_INSPECTION_RESULT";
    }

    public static boolean compareTime(JsonObject report, String start, String end) {
        Boolean result = false;
        String report_year;
        if (!report.has("ORDER_TIME")) {
            result = true;
        } else {
            report_year = report.get("ORDER_TIME").getAsString().substring(0, 4);

            if ((start == null || start.isEmpty() || (Integer.parseInt(report_year) - Integer.parseInt(start.substring(0, 4)) >= 0))
                    && ((end == null || end.isEmpty() || (Integer.parseInt(end.substring(0, 4)) - Integer.parseInt(report_year) >= 0)))) {
                result = true;
                logger.debug("SpecificChoice()(): time matches ... ");
            }
        }

        return result;
    }


}
