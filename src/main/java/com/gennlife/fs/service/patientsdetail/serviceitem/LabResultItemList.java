package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeSet;


/**
 * Created by Administrator on 2016/7/19.
 */
public class LabResultItemList extends PatientDetailService {
    /**
     * 实验室检验-检验分类
     *
     * @param url
     * UI/detail/LabResultItemList
     * @param param
     * {"patient_sn":"279991"}
     * @return URL 所代表远程资源的响应结果
     * {"success":true,"lab_result_item_list":["生化20","丙型肝炎抗体测定(微粒、发光法)","动态红细胞沉降率",
     * "梅毒血清特异性抗体测定(TPHA等)(进口试剂)","全血细胞分析(五分类)","凝血分析","肿瘤常规6","全血细胞分析(含血型)",
     * "尿10项(含尿沉渣及自动分析)","电解质分析","艾滋病毒抗体检测","粪便常规(含潜血)","乙肝2","C-反应蛋白(进口试剂)"]}
     */


    private static final Logger logger = LoggerFactory.getLogger(LabResultItemList.class);

    public String getLabResultItemList(String param) {
//        logger.debug("param " + param);
        String patient_sn = null;
        String visit_sn = null;
        TreeSet<String> data_array = new TreeSet<>();
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null) return ResponseMsgFactory.buildFailStr(" not json");
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }

        JsonObject result = new JsonObject();

        if (StringUtils.isEmpty(patient_sn)) {
            return ResponseMsgFactory.buildFailStr(" empty patient_sn");
        }
        QueryParam qp = new QueryParam(param_json, patient_sn, "inspection_reports");
//        qp.addsource("visits.inspection_reports.sub_inspection.SUB_INSPECTION_CN");
        qp.setQuery("[患者基本信息.患者编号] 包含 " + patient_sn);
        qp.setIndexName(BeansContextUtil.getUrlBean().getVisitIndexName());
        qp.setSize(1);
        JsonArray visits = new JsonArray();
        if (visit_sn == null) {
            qp = new QueryParam(param_json, patient_sn, "visits.inspection_reports");
            visits = filterPatientVisitsJsonArray(qp, param_json);
            if (visits == null) {
                return ResponseMsgFactory.buildFailStr(" no visit");
            }
        } else {
            JsonObject visit = get_visit(qp, visit_sn, patient_sn);
            if (visit == null) {
                return ResponseMsgFactory.buildFailStr(" no visit");
            }
            visits.add(visit);
        }

        for (JsonElement one_visit : visits) {
            JsonObject one_visit_json = one_visit.getAsJsonObject();
            JsonArray all_reports = null;
            if (one_visit_json.has("inspection_reports")) {
                all_reports = one_visit_json.get("inspection_reports").getAsJsonArray();
            }

            if (all_reports != null) {
                for (JsonElement one_report : all_reports) {
                    JsonObject one_report_jason = one_report.getAsJsonObject();
                    if (one_report_jason.has("INSPECTION_NAME")) {
                        if (one_report_jason.has("sub_inspection")) {
                            data_array.add(exchange(one_report_jason.get("INSPECTION_NAME").getAsString()));
                        }
                    }
                }
            }
        }
        if (data_array != null) data_array.remove("");
        if (data_array == null || data_array.size() == 0) return ResponseMsgFactory.buildFailStr("no data");
        result.add("lab_result_itemList", JsonAttrUtil.toJsonTree(data_array));
        return ResponseMsgFactory.buildSuccessStr(result);
    }
    public static String exchange(String inspectName)
    {
        if(inspectName==null)return "";
        inspectName=inspectName.replaceAll("[\t| ]+","");
        return inspectName;
    }
}
