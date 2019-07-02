package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.common.utils.TimerShaftSort;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.gennlife.fs.service.patientsdetail.model.TimeValueEntity;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.gennlife.fs.configurations.model.Model.emrModel;
import static com.gennlife.fs.service.patientsdetail.serviceitem.LabResultItemList.exchange;


/**
 * Created by Administrator on 2016/7/19.
 */
public class LabResultItem extends PatientDetailService {
    /**
     * 分类详情-检验检查-实验室检验
     * <p>
     * param    url
     * UI/detail/LabResultItem
     * param param
     * param:{"patient_sn":"279991","item_name_cn":"","limit":"1,10"}
     * return jason 所代表远程资源的响应结果
     * {"total":87,"lab_result_item":[{"item_unit":"%","normal_value_range":"50-70",
     * "exam_time":"2011-04-23 14:54:22","item_value":"59.1","item_name_cn":"中性细胞百分比",
     * "qualitative_results":""},{"item_unit":"%","normal_value_range":"20-40",
     * "exam_time":"2011-04-23 14:54:22","item_value":"35.4","item_name_cn":"淋巴细胞百分比","
     * qualitative_results":""}],"success":true,"limit":"1,10"}
     */
    private static final Logger logger = LoggerFactory.getLogger(LabResultItem.class);

    public String getLabResultItem(String param) {
//        logger.debug("param " + param);
        String patient_sn = null;
        String visit_sn = null;
        String item_name_cn = null;
        String limit;
        int page = 0;
        int size = 10;

        LinkedList<JsonElement> data_array = new LinkedList<JsonElement>();

        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null) return ResponseMsgFactory.buildFailStr(" not json");
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        if (param_json.has("item_name_cn")) {
            item_name_cn = param_json.get("item_name_cn").getAsString();
        }
        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }
        if (param_json.has("limit")) {
            limit = param_json.get("limit").getAsString();
            String[] pageParam = limit.split(",");
            try {
                page = Integer.valueOf(pageParam[0]);
                size = Integer.valueOf(pageParam[1]);
            } catch (NumberFormatException e) {
                return ResponseMsgFactory.buildFailStr("pageparam is not number ");
            }
        }
        if (-1 >= page) {
            page = 0;
        } else if (0 == page) {
            page = 1;
        }
        if (0 >= size) {
            size = 10;
        }
        if (StringUtil.isEmptyStr(patient_sn)) {
            return ResponseMsgFactory.buildFailStr("no patient_sn");
        }
        JsonObject result = new JsonObject();
        String INSPECTION_NAME = null;
        String SUB_INSPECTION_CN = null;
        String SUB_INSPECTION_EN = null;
        String SUB_INSPECTION_RESULT = null;
        String SUB_INSPECTION_RESULT_NUMBER = null;
        String SUB_INSPECTION_UNIT = null;
        String SUB_INSPECTION_REFERENCE_INTERVAL = null;
        String REPORT_TIME = null;
        String REPORT_TIME_KEY = null;
        String inspection_reports = null;
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            INSPECTION_NAME = "visits.inspection_report.INSPECTION_NAME";
            SUB_INSPECTION_CN =  "visits.inspection_report.sub_inspection.SUB_INSPECTION_CN";
            SUB_INSPECTION_EN = "visits.inspection_report.sub_inspection.SUB_INSPECTION_EN";
            SUB_INSPECTION_RESULT = "visits.inspection_report.sub_inspection.SUB_INSPECTION_RESULT";
            SUB_INSPECTION_RESULT_NUMBER = "visits.inspection_report.sub_inspection.SUB_INSPECTION_RESULT_NUMBER";
            SUB_INSPECTION_UNIT ="visits.inspection_report.sub_inspection.SUB_INSPECTION_UNIT";
            SUB_INSPECTION_REFERENCE_INTERVAL = "visits.inspection_report.sub_inspection.SUB_INSPECTION_REFERENCE_INTERVAL";
            REPORT_TIME = "visits.inspection_report.REPORT_DATE";
            REPORT_TIME_KEY = "REPORT_DATE";
            inspection_reports = "inspection_report";
        }else {
            INSPECTION_NAME = "visits.inspection_reports.INSPECTION_NAME";
            SUB_INSPECTION_CN =  "visits.inspection_reports.sub_inspection.SUB_INSPECTION_CN";
            SUB_INSPECTION_EN = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_EN";
            SUB_INSPECTION_RESULT = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_RESULT";
            SUB_INSPECTION_RESULT_NUMBER = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_RESULT_NUMBER";
            SUB_INSPECTION_UNIT ="visits.inspection_reports.sub_inspection.SUB_INSPECTION_UNIT";
            SUB_INSPECTION_REFERENCE_INTERVAL = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_REFERENCE_INTERVAL";
            REPORT_TIME = "visits.inspection_reports.REPORT_TIME";
            REPORT_TIME_KEY = "REPORT_TIME";
            inspection_reports = "inspection_reports";
        }

        QueryParam qp = new QueryParam(param_json, patient_sn, new String[]{
            inspection_reports,
        });
        qp.setQuery("[患者基本信息.患者编号] 包含 " + patient_sn);
        qp.setIndexName(BeansContextUtil.getUrlBean().getVisitIndexName());
        qp.setSize(1);
        JsonArray visits = null;

        if (visit_sn == null) {
            qp.addsource(INSPECTION_NAME,
                SUB_INSPECTION_CN,
                SUB_INSPECTION_EN,
                SUB_INSPECTION_RESULT,
                SUB_INSPECTION_RESULT_NUMBER,
                SUB_INSPECTION_UNIT,
                SUB_INSPECTION_REFERENCE_INTERVAL,
                REPORT_TIME);
            visits = filterPatientVisitsJsonArray(qp, param_json);
            if (visits == null) {
                return ResponseMsgFactory.buildFailStr(" no visit ");
            }
        } else {
            visits = new JsonArray();
            JsonObject visit = get_visit(qp, visit_sn, patient_sn);
            if (visit == null) {
                return ResponseMsgFactory.buildFailStr("no visit");
            }
            visits.add(visit);
        }
        for (JsonElement one_visit : visits) {
//            logger.debug("getLabResultItem(): one visit ...");
            JsonObject one_visit_json = one_visit.getAsJsonObject();
            JsonArray all_reports = null;
            if (one_visit_json.has(inspection_reports)) {
                all_reports = one_visit_json.get(inspection_reports).getAsJsonArray();
            } else {
                continue;
            }

            for (JsonElement one_report : all_reports) {
                JsonObject one_report_jason = one_report.getAsJsonObject();
                JsonArray all_sub_reports = null;
                String inspectionName = exchange(JsonAttrUtil.getStringValue("INSPECTION_NAME", one_report_jason));
                if (item_name_cn == null || item_name_cn.isEmpty()
                        || item_name_cn.equals(inspectionName)) {
                    //获取所有检验子项
                    all_sub_reports = JsonAttrUtil.getJsonArrayValue("sub_inspection", one_report_jason);
                    if(all_sub_reports ==null){
                        continue;
                    }
                    if (one_report_jason.has(REPORT_TIME_KEY))
                        JsonAttrUtil.setAttr(REPORT_TIME_KEY, one_report_jason.get(REPORT_TIME_KEY).getAsString(), all_sub_reports);
                }

                if (all_sub_reports != null) {
                    for (JsonElement sub_elem : all_sub_reports) { //每个检验子项
                        JsonObject one_sub_report = sub_elem.getAsJsonObject();
                        data_array.add(one_sub_report);
                    }
                }
                // of if(one_report_jason
            }// of for(JsonElement one_report
        } // for(JsonElement one_visit

        //分页
        int start = (page - 1) * size;
        int end = start + size;
        if (data_array.size() < end) {
            end = data_array.size();
        }

        //排除分页错情况
        if (start * end < 0 || start >= end) {
            result.add("lab_result_item", JsonAttrUtil.toJsonTree(data_array));

        } else {
            LinkedList<JsonElement> dataOut = new LinkedList<JsonElement>();
            Iterator<JsonElement> iter = data_array.iterator();
            while (iter.hasNext() && start > 0) {
                iter.next();
                start--;
            }
            end = size;
            while (iter.hasNext() && end > 0) {
                dataOut.add(iter.next());
                end--;
            }
            for (JsonElement element : dataOut) {
                if (element.isJsonObject()) {
                    JsonObject json = element.getAsJsonObject();
                    String number = JsonAttrUtil.getStringValue("SUB_INSPECTION_RESULT_NUMBER", json);
                    String result1 = JsonAttrUtil.getStringValue("SUB_INSPECTION_RESULT", json);

                    if(StringUtil.isNotEmptyStr(result1) && StringUtil.isNotEmptyStr(number)){
                        if(equalsStringForNumber(number,result1)){
                            json.addProperty("SUB_INSPECTION_RESULT",number);
                        }else {
                            String tmp = result1+"；"+number;
                            json.addProperty("SUB_INSPECTION_RESULT",tmp);
                            json.addProperty("SUB_INSPECTION_RESULT_NUMBER",tmp);
                        }
                    }else if(StringUtil.isNotEmptyStr(result1)) {
                        json.addProperty("SUB_INSPECTION_RESULT_NUMBER",number);
                    }else if(StringUtil.isNotEmptyStr(number)){
                        json.addProperty("SUB_INSPECTION_RESULT",subZeroAndDot(number));
                    }else {
                        json.addProperty("SUB_INSPECTION_RESULT_NUMBER","-");
                        json.addProperty("SUB_INSPECTION_RESULT", "-");
                    }
                }
            }
            result.add("lab_result_item", JsonAttrUtil.toJsonTree(dataOut));

        }
        //如果数据为0  不展示
        if(data_array.size() ==0){
            return ResponseMsgFactory.buildFailStr("no visit");
        }
        result.addProperty("limit", "" + page + "," + size);
        result.addProperty("total", data_array.size());
        return ResponseMsgFactory.buildSuccessStr(result);
    }

    public static String subZeroAndDot(String s){
        if(s.indexOf(".") > 0){
            s = s.replaceAll("0+?$", "");//去掉多余的0
            s = s.replaceAll("[.]$", "");//如最后一位是.则去掉
        }
        return s;
    }

    /**
     *
     " inspection_reports.INSPECTION_SN",
     "inspection_reports.INSPECTION_NAME",
     "inspection_reports.SPECIMEN_NAME",
     "inspection_reports.SUBMITTING_DEPARTMENT",
     "inspection_reports.ACQUISITION_TIME",
     "inspection_reports.RECEIVE_TIME",
     "inspection_reports.REPORT_TIME"
     * @param param
     * @return
     */
    public String getLabResul(String param) {
//        logger.debug("param " + param);
        String patient_sn = null;
        String visit_sn = null;
        String item_name_cn = null;
        String limit;
        int page = 0;
        int size = 10;

        LinkedList<JsonElement> data_array = new LinkedList<JsonElement>();

        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null) return ResponseMsgFactory.buildFailStr(" not json");
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        if (param_json.has("item_name_cn")) {
            item_name_cn = param_json.get("item_name_cn").getAsString();
        }
        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }
        if (param_json.has("limit")) {
            limit = param_json.get("limit").getAsString();
            String[] pageParam = limit.split(",");
            try {
                page = Integer.valueOf(pageParam[0]);
                size = Integer.valueOf(pageParam[1]);
            } catch (NumberFormatException e) {
                return ResponseMsgFactory.buildFailStr("pageparam is not number ");
            }
        }
        if (-1 >= page) {
            page = 0;
        } else if (0 == page) {
            page = 1;
        }
        if (0 >= size) {
            size = 10;
        }
        if (StringUtil.isEmptyStr(patient_sn)) {
            return ResponseMsgFactory.buildFailStr("no patient_sn");
        }
        JsonObject result = new JsonObject();
        String INSPECTION_SN =  "inspection_reports.INSPECTION_SN";
        String INSPECTION_NAME = "inspection_reports.INSPECTION_NAME";
        String SPECIMEN_NAME = "inspection_reports.SPECIMEN_NAME";
        String SUBMITTING_DEPARTMENT = "inspection_reports.SUBMITTING_DEPARTMENT";
        String ACQUISITION_TIME = "inspection_reports.ACQUISITION_TIME";
        String RECEIVE_TIME ="inspection_reports.RECEIVE_TIME";
        String REPORT_TIME = "inspection_reports.REPORT_TIME";
        String REPORT_TIME_KEY = "REPORT_TIME";
        String APPLY_DEPT = "inspection_reports.APPLY_DEPT";
        String inspection_reports = "inspection_reports";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
             INSPECTION_SN =  "inspection_report.INSPECTION_SN";
             INSPECTION_NAME = "inspection_report.INSPECTION_NAME";
             SPECIMEN_NAME = "inspection_report.SPECIMEN_NAME";
             SUBMITTING_DEPARTMENT = "inspection_report.SUBMITTING_DEPARTMENT";
             ACQUISITION_TIME = "inspection_report.ACQUISITION_TIME";
             RECEIVE_TIME ="inspection_report.RECEIVE_TIME";
             REPORT_TIME = "inspection_report.REPORT_DATE";
            REPORT_TIME_KEY = "REPORT_DATE";
            inspection_reports = "inspection_report";
            APPLY_DEPT = "inspection_report.APPLY_DEPT";
        }
        QueryParam qp = new QueryParam(param_json, patient_sn, new String[]{
            INSPECTION_SN,
            INSPECTION_NAME,
            SPECIMEN_NAME,
            SUBMITTING_DEPARTMENT,
            ACQUISITION_TIME,
            RECEIVE_TIME,
            REPORT_TIME,
            APPLY_DEPT
        });
        qp.setQuery("[患者基本信息.患者编号] 包含 " + patient_sn);
        qp.setIndexName(BeansContextUtil.getUrlBean().getVisitIndexName());
        qp.setSize(1);
        JsonObject visit = get_visit(qp, visit_sn, patient_sn);
        if (visit == null) {
            return ResponseMsgFactory.buildFailStr("no visit");
        }

        JsonArray visits = visit.getAsJsonArray(inspection_reports);
        if(visits == null){
            return ResponseMsgFactory.buildFailStr("no inspection_reports");
        }
        //排序
        JsonArray sortVisits = inspectionReportsSort(visits,item_name_cn,REPORT_TIME_KEY);
        //分页
        int start = (page - 1) * size;
        int end = start + size;
        if (sortVisits.size() < end) {
            end = sortVisits.size();
        }

        //排除分页错情况
        if (start * end < 0 || start >= end) {
            result.add("lab_result", sortVisits);

        } else {
            LinkedList<JsonElement> dataOut = new LinkedList<JsonElement>();
            Iterator<JsonElement> iter = sortVisits.iterator();
            while (iter.hasNext() && start > 0) {
                iter.next();
                start--;
            }
            end = size;
            while (iter.hasNext() && end > 0) {
                dataOut.add(iter.next());
                end--;
            }
            result.add(inspection_reports, JsonAttrUtil.toJsonTree(dataOut));

        }
        if(sortVisits.size() ==0){
            return ResponseMsgFactory.buildFailStr("no visit");
        }
        result.addProperty("limit", "" + page + "," + size);
        result.addProperty("total", sortVisits.size());
        result.addProperty("configSchema",inspection_reports);
        return ResponseMsgFactory.buildSuccessStr(result);
    }

    private JsonArray inspectionReportsSort(JsonArray visit,String item_name_cn,String REPORT_TIME_KEY) {
        List<JsonObject> resultObj = new LinkedList<>();
        List<String> sortList = new LinkedList<>();
        for (JsonElement element : visit){
            JsonObject object = element.getAsJsonObject();
            String time = JsonAttrUtil.getStringValue(REPORT_TIME_KEY,object);
            String inspectionName = exchange(JsonAttrUtil.getStringValue("INSPECTION_NAME", object));
            if(StringUtil.isNotEmptyStr(item_name_cn) && !item_name_cn.equals(inspectionName) ){
                continue;
            }
            TimerShaftSort.getInstance().transforTimeByJsonObject(resultObj,sortList,object,time);
        }
        return (JsonArray) JsonAttrUtil.toJsonTree(resultObj);

    }

    public String getNewLabResultItem(String param) {
//        logger.debug("param " + param);
        String patient_sn = null;
        String visit_sn = null;
        String item_name_cn = null;
        String inspection_sn = null;
        String limit;
        int page = 0;
        int size = 10;

        LinkedList<JsonElement> data_array = new LinkedList<JsonElement>();

        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null) return ResponseMsgFactory.buildFailStr(" not json");
        if (param_json.has("inspection_sn")) {
            inspection_sn = param_json.get("inspection_sn").getAsString();
        }else {
            return ResponseMsgFactory.buildFailStr("pageparam is not inspection_sn ");
        }
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        if (param_json.has("item_name_cn")) {
            item_name_cn = param_json.get("item_name_cn").getAsString();
        }
        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }
        if (param_json.has("limit")) {
            limit = param_json.get("limit").getAsString();
            String[] pageParam = limit.split(",");
            try {
                page = Integer.valueOf(pageParam[0]);
                size = Integer.valueOf(pageParam[1]);
            } catch (NumberFormatException e) {
                return ResponseMsgFactory.buildFailStr("pageparam is not number ");
            }
        }
        if (-1 >= page) {
            page = 0;
        } else if (0 == page) {
            page = 1;
        }
        if (0 >= size) {
            size = 10;
        }
        if (StringUtil.isEmptyStr(patient_sn)) {
            return ResponseMsgFactory.buildFailStr("no patient_sn");
        }
        JsonObject result = new JsonObject();

        String INSPECTION_NAME = null;
        String SUB_INSPECTION_CN = null;
        String SUB_INSPECTION_EN = null;
        String SUB_INSPECTION_RESULT = null;
        String SUB_INSPECTION_RESULT_NUMBER = null;
        String SUB_INSPECTION_UNIT = null;
        String SUB_INSPECTION_REFERENCE_INTERVAL = null;
        String REPORT_TIME = null;
        String REPORT_TIME_KEY = null;
        String inspection_reports = null;
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            INSPECTION_NAME = "visits.inspection_report.INSPECTION_NAME";
            SUB_INSPECTION_CN =  "visits.inspection_report.sub_inspection.SUB_INSPECTION_CN";
            SUB_INSPECTION_EN = "visits.inspection_report.sub_inspection.SUB_INSPECTION_EN";
            SUB_INSPECTION_RESULT = "visits.inspection_report.sub_inspection.SUB_INSPECTION_RESULT";
            SUB_INSPECTION_RESULT_NUMBER = "visits.inspection_report.sub_inspection.SUB_INSPECTION_RESULT_NUMBER";
            SUB_INSPECTION_UNIT ="visits.inspection_report.sub_inspection.SUB_INSPECTION_UNIT";
            SUB_INSPECTION_REFERENCE_INTERVAL = "visits.inspection_report.sub_inspection.SUB_INSPECTION_REFERENCE_INTERVAL";
            REPORT_TIME = "visits.inspection_report.REPORT_DATE";
            REPORT_TIME_KEY = "visits.inspection_report.REPORT_DATE";
            inspection_reports = "inspection_report";
        }else {
            INSPECTION_NAME = "visits.inspection_reports.INSPECTION_NAME";
            SUB_INSPECTION_CN =  "visits.inspection_reports.sub_inspection.SUB_INSPECTION_CN";
            SUB_INSPECTION_EN = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_EN";
            SUB_INSPECTION_RESULT = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_RESULT";
            SUB_INSPECTION_RESULT_NUMBER = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_RESULT_NUMBER";
            SUB_INSPECTION_UNIT ="visits.inspection_reports.sub_inspection.SUB_INSPECTION_UNIT";
            SUB_INSPECTION_REFERENCE_INTERVAL = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_REFERENCE_INTERVAL";
            REPORT_TIME = "visits.inspection_reports.REPORT_TIME";
            REPORT_TIME_KEY = "REPORT_TIME";
            inspection_reports = "inspection_reports";
        }
        QueryParam qp = new QueryParam(param_json, patient_sn, new String[]{
            inspection_reports,
        });
        qp.setQuery("[患者基本信息.患者编号] 包含 " + patient_sn);
        qp.setIndexName(BeansContextUtil.getUrlBean().getVisitIndexName());
        qp.setSize(1);
        JsonArray visits = null;

        if (visit_sn == null) {
            qp.addsource(INSPECTION_NAME,
                SUB_INSPECTION_CN,
                SUB_INSPECTION_EN,
                SUB_INSPECTION_RESULT,
                SUB_INSPECTION_RESULT_NUMBER,
                SUB_INSPECTION_UNIT,
                SUB_INSPECTION_REFERENCE_INTERVAL,
                REPORT_TIME);
            visits = filterPatientVisitsJsonArray(qp, param_json);
            if (visits == null) {
                return ResponseMsgFactory.buildFailStr(" no visit ");
            }
        } else {
            visits = new JsonArray();
            JsonObject visit = get_visit(qp, visit_sn, patient_sn);
            if (visit == null) {
                return ResponseMsgFactory.buildFailStr("no visit");
            }
            visits.add(visit);
        }

        for (JsonElement one_visit : visits) {
//            logger.debug("getLabResultItem(): one visit ...");
            JsonObject one_visit_json = one_visit.getAsJsonObject();
            JsonArray all_reports = null;
            if (one_visit_json.has(inspection_reports)) {
                all_reports = one_visit_json.get(inspection_reports).getAsJsonArray();
            } else {
                continue;
            }

            for (JsonElement one_report : all_reports) {
                JsonObject one_report_jason = one_report.getAsJsonObject();
                String inspectionSn = JsonAttrUtil.getStringValue("INSPECTION_SN",one_report_jason);
                if(!inspection_sn.equals(inspectionSn)){
                    continue;
                }
                JsonArray all_sub_reports = null;
                String inspectionName = exchange(JsonAttrUtil.getStringValue("INSPECTION_NAME", one_report_jason));
                if (item_name_cn == null || item_name_cn.isEmpty()
                    || item_name_cn.equals(inspectionName)) {
                    //获取所有检验子项
                    all_sub_reports = JsonAttrUtil.getJsonArrayValue("sub_inspection", one_report_jason);
                    if(all_sub_reports ==null){
                        continue;
                    }
                    if (one_report_jason.has(REPORT_TIME_KEY))
                        JsonAttrUtil.setAttr(REPORT_TIME_KEY, one_report_jason.get(REPORT_TIME_KEY).getAsString(), all_sub_reports);
                }

                if (all_sub_reports != null) {
                    for (JsonElement sub_elem : all_sub_reports) { //每个检验子项
                        JsonObject one_sub_report = sub_elem.getAsJsonObject();
                        data_array.add(one_sub_report);
                    }
                }
                // of if(one_report_jason
            }// of for(JsonElement one_report
        } // for(JsonElement one_visit

        //分页
        int start = (page - 1) * size;
        int end = start + size;
        if (data_array.size() < end) {
            end = data_array.size();
        }

        //排除分页错情况
        if (start * end < 0 || start >= end) {
            result.add("lab_result_item", JsonAttrUtil.toJsonTree(data_array));

        } else {
            LinkedList<JsonElement> dataOut = new LinkedList<JsonElement>();
            Iterator<JsonElement> iter = data_array.iterator();
            while (iter.hasNext() && start > 0) {
                iter.next();
                start--;
            }
            end = size;
            while (iter.hasNext() && end > 0) {
                dataOut.add(iter.next());
                end--;
            }
            for (JsonElement element : dataOut) {
                if (element.isJsonObject()) {
                    JsonObject json = element.getAsJsonObject();
                    String number = JsonAttrUtil.getStringValue("SUB_INSPECTION_RESULT_NUMBER", json);
                    String result1 = JsonAttrUtil.getStringValue("SUB_INSPECTION_RESULT", json);

                    if(StringUtil.isNotEmptyStr(result1) && StringUtil.isNotEmptyStr(number)){
                        if(equalsStringForNumber(number,result1)){
                            json.addProperty("SUB_INSPECTION_RESULT",number);
                        }else {
                            String tmp = result1+"；"+number;
                            json.addProperty("SUB_INSPECTION_RESULT",tmp);
                            json.addProperty("SUB_INSPECTION_RESULT_NUMBER",tmp);
                        }
                    }else if(StringUtil.isNotEmptyStr(result1)) {
                        json.addProperty("SUB_INSPECTION_RESULT_NUMBER",number);
                    }else if(StringUtil.isNotEmptyStr(number)){
                        json.addProperty("SUB_INSPECTION_RESULT",subZeroAndDot(number));
                    }else {
                        json.addProperty("SUB_INSPECTION_RESULT_NUMBER","-");
                        json.addProperty("SUB_INSPECTION_RESULT", "-");
                    }
                }
            }
            result.add("sub_inspection", JsonAttrUtil.toJsonTree(dataOut));

        }
        //如果数据为0  不展示
        if(data_array.size() ==0){
            return ResponseMsgFactory.buildFailStr("no visit");
        }
        result.addProperty("configSchema","sub_inspection");
        result.addProperty("limit", "" + page + "," + size);
        result.addProperty("total", data_array.size());
        return ResponseMsgFactory.buildSuccessStr(result);
    }

    public Boolean equalsStringForNumber(String a, String b){
        try {
            return Objects.equals(Double.valueOf(a),Double.valueOf(b));
        }catch (NumberFormatException e){
            return Objects.equals(a,b);
        }
    }
    public String getNewQuotaReports(String param){
//        logger.debug("param " + param);
        String patient_sn = null;
        String sub_inspection_cn = null;
        String specimen_name = null;
        String sub_inspection_unit = null;
        Double interval_max = 0.0;
        Double interval_min = 0.0;
        String interval = "";

        LinkedList<JsonElement> data_array = new LinkedList<JsonElement>();

        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null) return ResponseMsgFactory.buildFailStr(" not json");
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        if (param_json.has("sub_inspection_cn")) {
            sub_inspection_cn = param_json.get("sub_inspection_cn").getAsString();
        }else {
            return ResponseMsgFactory.buildFailStr("pageparam is not sub_inspection_cn ");
        }
        if (param_json.has("specimen_name")) {
            specimen_name = param_json.get("specimen_name").getAsString();
        }else {
            return ResponseMsgFactory.buildFailStr("pageparam is not specimen_name ");
        }
        if (param_json.has("sub_inspection_unit")) {
            sub_inspection_unit = param_json.get("sub_inspection_unit").getAsString();
        }else {
            return ResponseMsgFactory.buildFailStr("pageparam is not sub_inspection_unit ");
        }
        String INSPECTION_NAME = null;
        String SPECIMEN_NAME = null;
        String SUB_INSPECTION_CN = null;
        String SUB_INSPECTION_EN = null;
        String SUB_INSPECTION_RESULT = null;
        String SUB_INSPECTION_RESULT_NUMBER = null;
        String SUB_INSPECTION_UNIT = null;
        String SUB_INSPECTION_REFERENCE_INTERVAL = null;
        String REPORT_TIME = null;
        String REPORT_TIME_KEY = null;
        String inspection_reports = null;
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            INSPECTION_NAME = "visits.inspection_report.INSPECTION_NAME";
            SPECIMEN_NAME = "visits.inspection_report.SPECIMEN_NAME";
            SUB_INSPECTION_CN =  "visits.inspection_report.sub_inspection.SUB_INSPECTION_CN";
            SUB_INSPECTION_EN = "visits.inspection_report.sub_inspection.SUB_INSPECTION_EN";
            SUB_INSPECTION_RESULT = "visits.inspection_report.sub_inspection.SUB_INSPECTION_RESULT";
            SUB_INSPECTION_RESULT_NUMBER = "visits.inspection_report.sub_inspection.SUB_INSPECTION_RESULT_NUMBER";
            SUB_INSPECTION_UNIT ="visits.inspection_report.sub_inspection.SUB_INSPECTION_UNIT";
            SUB_INSPECTION_REFERENCE_INTERVAL = "visits.inspection_report.sub_inspection.SUB_INSPECTION_REFERENCE_INTERVAL";
            REPORT_TIME = "visits.inspection_report.REPORT_DATE";
            REPORT_TIME_KEY = "REPORT_DATE";
            inspection_reports = "inspection_report";
        }else {
            INSPECTION_NAME = "visits.inspection_reports.INSPECTION_NAME";
            SPECIMEN_NAME = "visits.inspection_reports.SPECIMEN_NAME";
            SUB_INSPECTION_CN =  "visits.inspection_reports.sub_inspection.SUB_INSPECTION_CN";
            SUB_INSPECTION_EN = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_EN";
            SUB_INSPECTION_RESULT = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_RESULT";
            SUB_INSPECTION_RESULT_NUMBER = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_RESULT_NUMBER";
            SUB_INSPECTION_UNIT ="visits.inspection_reports.sub_inspection.SUB_INSPECTION_UNIT";
            SUB_INSPECTION_REFERENCE_INTERVAL = "visits.inspection_reports.sub_inspection.SUB_INSPECTION_REFERENCE_INTERVAL";
            REPORT_TIME = "visits.inspection_reports.REPORT_TIME";
            REPORT_TIME_KEY = "REPORT_TIME";
            inspection_reports = "inspection_reports";
        }
        QueryParam qp = new QueryParam(param_json, patient_sn, new String[]{
            inspection_reports,
        });
        qp.setQuery("[患者基本信息.患者编号] 包含 " + patient_sn);
        qp.setIndexName(BeansContextUtil.getUrlBean().getVisitIndexName());
        qp.setSize(1);
        qp.addsource(
            INSPECTION_NAME,
            SPECIMEN_NAME,
            REPORT_TIME,
            SUB_INSPECTION_CN,
            SUB_INSPECTION_EN,
            SUB_INSPECTION_RESULT,
            SUB_INSPECTION_RESULT_NUMBER,
            SUB_INSPECTION_UNIT,
            SUB_INSPECTION_REFERENCE_INTERVAL);
        JsonArray visits = null;
        visits = filterPatientVisitsJsonArray(qp, param_json);
        if (visits == null) {
            return ResponseMsgFactory.buildFailStr(" no visit ");
        }
        TreeSet<TimeValueEntity> allIds = new TreeSet<>();
        INS: for (JsonElement one_visit : visits) {

            JsonObject one_visit_json = one_visit.getAsJsonObject();
            JsonArray all_reports = null;
            if (one_visit_json.has(inspection_reports)) {
                all_reports = one_visit_json.get(inspection_reports).getAsJsonArray();
            } else {
                continue INS;
            }

            SUB: for (JsonElement one_report : all_reports) {
                JsonObject one_report_json = one_report.getAsJsonObject();
                JsonArray sub_inspection = null;
                if (one_report_json.has("sub_inspection")) {
                    sub_inspection = one_report_json.get("sub_inspection").getAsJsonArray();
                } else {
                    continue SUB;
                }

                if(!one_report_json.has("SPECIMEN_NAME") || !one_report_json.get("SPECIMEN_NAME").getAsString().equals(specimen_name) ){
                    continue SUB;
                }
                String reportTime = JsonAttrUtil.getStringValue(REPORT_TIME_KEY, one_report_json);
                if(StringUtil.isEmptyStr(reportTime)){
                    continue SUB;
                }

                REP: for (JsonElement sub_report : sub_inspection){

                    JsonObject sub_report_jason = sub_report.getAsJsonObject();
                    if(!sub_report_jason.has("SUB_INSPECTION_CN") || !sub_report_jason.get("SUB_INSPECTION_CN").getAsString().equals(sub_inspection_cn)){
                        continue REP;
                    }

                    if(StringUtil.isEmptyStr(sub_inspection_unit)){
                        if(sub_report_jason.has("SUB_INSPECTION_UNIT") && StringUtil.isNotEmptyStr(sub_report_jason.get("SUB_INSPECTION_UNIT").getAsString()) ){
                            continue REP;
                        }
                    }else {
                        if(!sub_report_jason.has("SUB_INSPECTION_UNIT") || !sub_report_jason.get("SUB_INSPECTION_UNIT").getAsString().equals(sub_inspection_unit)){
                            continue REP;
                        }
                    }
                    interval= JsonAttrUtil.getValByJsonObject(sub_report_jason,"SUB_INSPECTION_REFERENCE_INTERVAL");
                    try {
                        double value = Double.valueOf(JsonAttrUtil.getStringValue("SUB_INSPECTION_RESULT_NUMBER", sub_report_jason));
                        if(value > interval_max){
                            interval_max = value;
                        }
                        if(interval_min == 0.0 || value < interval_min){
                            interval_min = value;
                        }
                        TimeValueEntity entity = new TimeValueEntity(reportTime, value);
                        allIds.add(entity);
                    }catch (Exception e){
                        logger.info("检验子项数值解析问题？ ： "+e.getMessage());
                        continue;
                    }
                }

            }
        }
        JsonObject result = new JsonObject();
        JsonArray xAys = new JsonArray();
        JsonArray data = new JsonArray();
        if(allIds.size()<1){
            return ResponseMsgFactory.buildFailStr("no data");
        }

        Iterator i = allIds.iterator();
        while (i.hasNext()) {
            TimeValueEntity entity = (TimeValueEntity) i.next();
            xAys.add(entity.getTime());
            data.add(entity.getValue());
        }
        result.add("xAxis",xAys);
        result.add("data",data);
        result.addProperty("yAxisName",sub_inspection_unit);
        result.addProperty("seriesName",sub_inspection_cn);
        result.addProperty("interval_max",interval_max);
        result.addProperty("interval_min",interval_min);
        result.addProperty("interval",interval);
        return ResponseMsgFactory.buildSuccessStr(result);

    }

}
