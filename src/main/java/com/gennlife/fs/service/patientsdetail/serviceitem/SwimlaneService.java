package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.comparator.JsonComparatorASCByKey;
import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.PaginationMemoryResponse;
import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.common.utils.*;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.gennlife.fs.system.config.GroupVisitSearch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;
import static com.gennlife.fs.configurations.model.Model.emrModel;

/**
 *
 * 分类详情-病程记录
 */
public class SwimlaneService {

    private static Logger LOGGER = LoggerFactory.getLogger(SwimlaneService.class);
    private  final JsonObject SWIMLANCE_SORT ;
    private  final JsonObject SWIMLANCE_SHOW_NAME ;
    private  final JsonObject SWIMLANCE_SHOW_STR_NAME ;
    private  final JsonArray FIELD_NAME;
    private  final JsonObject SWIMLANCE_SHOW_CONFIG;
    private static final String OPERATION_DATE_COUNT="OperationDays";
    private static final String ADMISSION_DATE_COUNT="hospitalDays";
    private static final String TIME="Dates";
    private static final String IMAGING_REPORTS="ImagingReports";
    private static final String INSPECTION_REPORTS="InspectionReports";
    private static final String OPERATION="Operation";
    private static final String LONG_MEDICINE="LongMedicine";
    private static final String ONCE_MEDICINE="OnceMedicine";
    private static final String NON_ORDER="NonOrder";
    private static final String MEDICAL_RECORDS="MedicalRecords";
    private static final String ONCE_MEDICINE_CONFIG = "medicine_order";
    private static final String ONCE_MEDICINE_CONFIG_4 = "drug_order";
     {
         JsonObject swimlanceConfig = null;
         JsonObject swimlanceshowconfig = JsonAttrUtil.getJsonObjectfromFile(SystemUtil.getPath("/data/swimlanceShowName.json"));
         if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
             swimlanceConfig = JsonAttrUtil.getJsonObjectfromFile(SystemUtil.getPath("/data/swimlance2.0.json"));
         }else {
             swimlanceConfig = JsonAttrUtil.getJsonObjectfromFile(SystemUtil.getPath("/data/swimlance1.0.json"));
         }
        SWIMLANCE_SORT = swimlanceConfig.getAsJsonObject("sort");
        SWIMLANCE_SHOW_NAME = swimlanceConfig.getAsJsonObject("showName");
        SWIMLANCE_SHOW_STR_NAME = swimlanceConfig.getAsJsonObject("strongShowName");
        FIELD_NAME = swimlanceshowconfig.getAsJsonArray("fieldName");
        SWIMLANCE_SHOW_CONFIG = swimlanceshowconfig.getAsJsonObject("swimlanceConfig");
    }

    public String getSwimlane(String param) {
        String patient_sn = null;//from param   病人ID
        String visit_sn = null;
        Integer page = 0;
        Integer size = 0;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null) {
            return ResponseMsgFactory.buildFailStr(" not json");
        }
        if (JsonAttrUtil.has_key(param_json,"patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }else {
            return ResponseMsgFactory.buildFailStr("no patient_sn");
        }

        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }else {
            return ResponseMsgFactory.buildFailStr("no visit_sn");
        }
        if (param_json.has("page")) {
            page = param_json.get("page").getAsInt();
        }else {
            return ResponseMsgFactory.buildFailStr("no page");
        }
        if (param_json.has("size")) {
            size = param_json.get("size").getAsInt();
        }else {
            return ResponseMsgFactory.buildFailStr("no size");
        }

        JsonObject diagnose = getDiannoseCount(param_json);
        String startTime = DateUtil.getDateStr_ymd(JsonAttrUtil.getStringValue("ADMISSION_DATE",diagnose));
        if(StringUtil.isEmptyStr(startTime)){
            return ResponseMsgFactory.buildFailStr("no admission_date");
        }
        String endTime = DateUtil.getDateStr_ymd(JsonAttrUtil.getStringValue("DISCHARGE_DATE",diagnose));
//        Map<String,JsonObject> timeLines = getTimeLines(startTime,endTime);
        Map<String,JsonObject> timeLines = new TreeMap<String,JsonObject>(String::compareTo);

        getExamResult(timeLines,param);
        getLabResultItem(timeLines,param);
        getOperationRecods(timeLines,param);
        getOrders(timeLines,param);
        getElectronic(timeLines,param);
        getMedicalOrders(timeLines,param,endTime);

        Map<String,JsonObject> resultTimeLines = transForTimeLinesDays(timeLines,startTime,endTime,startTime);

        List<JsonObject> resultList = new LinkedList<>();
        for (Map.Entry<String,JsonObject> entry : resultTimeLines.entrySet()){
            JsonObject object = new JsonObject();
            object.addProperty(TIME,entry.getKey());
            object.add("data",entry.getValue());
            resultList.add(object);
        }
        List<JsonObject> resultData = PagingUtils.getPageContentByApi(resultList,page,size);
        JsonObject data = new JsonObject();
        data.add("diagnose",diagnose);
        data.add("timerShaft", JsonAttrUtil.toJsonTree(resultData));
        JsonObject result = new JsonObject();
        result.add("fieldName",FIELD_NAME);
        result.addProperty("code",1);
        result.addProperty("msg","success");
        result.add("data",data);
        result.addProperty("total",resultList.size());
        return JsonAttrUtil.toJsonStr(result);
    }

    private Map<String, JsonObject> transForTimeLinesDays(Map<String, JsonObject> timeLines, String startTime, String endTime, String admissionDate) {
        Map<String,JsonObject> resultMap = new LinkedHashMap<>();
        String beforTime = "";
        String[] operatorData = new String [2];
        for (Map.Entry<String,JsonObject> entry : timeLines.entrySet()){
            String time = entry.getKey();
            JsonObject val = entry.getValue();
            if(endTime.compareTo(time) < 0){
                break;
            }
            while (resultMap.size()<=0 && startTime.compareTo(time) < 0){
                JsonObject obj = new JsonObject();
                addDays(obj,startTime,admissionDate,operatorData);
                resultMap.put(startTime,obj);
                startTime = DateUtil.getSpecifiedDayAfter(startTime);
            }
            while (StringUtil.isNotEmptyStr(beforTime) && beforTime.compareTo(time) < 0){
                JsonObject obj = new JsonObject();
                addDays(obj,beforTime,admissionDate,operatorData);
                resultMap.put(beforTime,obj);
                beforTime = DateUtil.getSpecifiedDayAfter(beforTime);
            }
            addDays(val,time,admissionDate,operatorData);
            beforTime = DateUtil.getSpecifiedDayAfter(time);
            resultMap.put(time,val);
        }
        while ( resultMap.size() % 7 !=0){
            JsonObject obj = new JsonObject();
            addDays(obj,beforTime,admissionDate,operatorData);
            resultMap.put(beforTime,obj);
            beforTime = DateUtil.getSpecifiedDayAfter(beforTime);
        }
        return resultMap;

    }

    private void addDays(JsonObject val,String time,String admissionDate,String[] operatorData) {
        Long admissionDays = DateUtil.getDurationWithDays(time,admissionDate) + 1;
        val.addProperty(ADMISSION_DATE_COUNT,admissionDays);
        //手术逻辑

        if(StringUtil.isNotEmptyStr(operatorData[0]) ){
            if(val.has("OPERATION_DATE")){
                String operTimeTmp = JsonAttrUtil.getStringValue("OPERATION_DATE",val);
                Long days = DateUtil.getDurationWithDays(operatorData[0],operTimeTmp);
                if(days>14){
                    operatorData[0] = operTimeTmp;
                    operatorData[1] = "";
                    Long count = DateUtil.getDurationWithDays(time,operatorData[0])+1;
                    val.addProperty(OPERATION_DATE_COUNT,count);
                }else {
                    Long count1 = DateUtil.getDurationWithDays(time,operatorData[0])+1;
                    Long count2 = DateUtil.getDurationWithDays(time,operatorData[1])+1;
                    operatorData[1] = operTimeTmp;
                    val.addProperty(OPERATION_DATE_COUNT,count2+"/"+count1);
                }
                val.addProperty("OPERATION_DATE",operTimeTmp);
            }else {
                val.addProperty("OPERATION_DATE",operatorData[0]);
                Long count = DateUtil.getDurationWithDays(time,operatorData[0])+1;
                val.addProperty(OPERATION_DATE_COUNT,count);
            }
        }else if(val.has("OPERATION_DATE")){
            operatorData[0] = JsonAttrUtil.getStringValue("OPERATION_DATE",val);
            Long count = DateUtil.getDurationWithDays(time,operatorData[0])+1;
            val.addProperty(OPERATION_DATE_COUNT,count);
        }else {
            val.addProperty(OPERATION_DATE_COUNT,"");
        }
    }

    private void getMedicalOrders(Map<String, JsonObject> timeLines, String param, String endTime) {
         String medicine_order = "medicine_order";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            medicine_order = "drug_order";
        }
        VisitSNResponse vt =  new VisitSNResponse(medicine_order,medicine_order);
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        if(paramJson==null)return  ;
        vt.execute(JsonAttrUtil.toJsonObject(paramJson));
        JsonObject result = vt.get_result();
        JsonArray medicin = result.get(medicine_order).getAsJsonArray();
        JsonArray longMedicin = new JsonArray();
        JsonArray shortMedicin = new JsonArray();
        for (JsonElement element : medicin){
            JsonObject object = element.getAsJsonObject();
            String LONG_ONCE_FLAG = JsonAttrUtil.getStringValue("LONG_ONCE_FLAG",object);
            String ORDER_STATUS_NAME = JsonAttrUtil.getStringValue("ORDER_STATUS_NAME",object);
            String CANCEL_FLAG = JsonAttrUtil.getStringValue("CANCEL_FLAG",object);
            if("是".equals(CANCEL_FLAG)){
                continue;
            }
            if("作废".equals(ORDER_STATUS_NAME)){
                continue;
            }
            if("长期医嘱".equals(LONG_ONCE_FLAG)){
                String ORDER_STOP_TIME = JsonAttrUtil.getStringValue("ORDER_STOP_TIME",object);
                if(StringUtil.isEmptyStr(ORDER_STATUS_NAME)){
                    if(StringUtil.isEmptyStr(endTime)){
                        ORDER_STATUS_NAME = DateUtil.getDateStr_ymd(new Date());
                    }else {
                        ORDER_STATUS_NAME = endTime;
                    }
                }
                longMedicin.add(object);
            }
            if("临时医嘱".equals(LONG_ONCE_FLAG)){
                shortMedicin.add(object);
            }
        }
        transForArrayTimeMap(longMedicin,timeLines,LONG_MEDICINE,LONG_MEDICINE);
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            transForArrayTimeMap(shortMedicin, timeLines, ONCE_MEDICINE,ONCE_MEDICINE_CONFIG_4);
        }else {
            transForArrayTimeMap(shortMedicin, timeLines, ONCE_MEDICINE,ONCE_MEDICINE_CONFIG);
        }
    }

    private void transForArrayTimeMap(JsonArray longMedicin, Map<String, JsonObject> timeLines, String key,String configSchema) {
        Map<String,List<JsonObject>> longTimeMap = new HashMap<>();
        for (JsonElement element : longMedicin){
            JsonObject object = element.getAsJsonObject();
            String time = DateUtil.getDateStr_ymd(JsonAttrUtil.getStringValue("ORDER_START_TIME",object));
            if(StringUtil.isEmptyStr(time)){
                continue;
            }
            String titleName = JsonAttrUtil.getStringValue("ORDER_NAME",object);
            if(StringUtil.isEmptyStr(titleName)){
                titleName = "非药品医嘱";
            }
            object.addProperty("type",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(key).get("type").getAsString());
            object.addProperty("unfold",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(key).get("unfold").getAsBoolean());
            object.addProperty("port",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(key).get("port").getAsString());
            object.addProperty("titleName",titleName);
            object.addProperty("configSchema",configSchema);
            if(!longTimeMap.containsKey(time)){
                longTimeMap.put(time,new LinkedList<>());
            }
            longTimeMap.get(time).add(object);
        }
        putTimeLinesValues(longTimeMap,timeLines,key);
    }

    private void getElectronic(Map<String, JsonObject> timeLines, String param) {
        String[] s1 = null;
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            s1 = new String[]{
                "admissions_records",
                "discharge_records",
                "first_course_record",
                "attending_physician_rounds_records",
                "course_record",
                "post_course_record",
                "operation_pre_summary",
                "discharge_summary",
                "death_discuss_records",
                "death_records",
                "death_summary",
                "difficulty_case_records",
                "handover_record",
                "rescue_records",
                "stage_summary",
                "transferred_in_records",
                "transferred_out_records"};
        }else {
            s1 = new String[]{
                "discharge_summary",
                "death_discuss_record",
                "death_record",
                "death_summary",
                "difficulty_case_record",
                "handover_record",
                "rescue_record",
                "stage_summary",
                "transferred_in_record",
                "transferred_out_record"
            };
        }
        VisitSNResponse template=new VisitSNResponse(
            s1
        );
        template.execute(JsonAttrUtil.toJsonObject( JsonAttrUtil.toJsonObject(param)));
        JsonObject result = template.get_result();
        if(result != null){
            Map<String,List<JsonObject>> timeMap = new HashMap<>();
            ONE: for (Map.Entry<String,JsonElement> entry : result.entrySet()){
                String key = entry.getKey();
                JsonArray array = entry.getValue().getAsJsonArray();
                TWO: for (JsonElement element : array){
                    JsonObject tmpObj = element.getAsJsonObject();
                    String time = DateUtil.getDateStr_ymd(JsonAttrUtil.getStringValue(JsonAttrUtil.getStringValue(key,SWIMLANCE_SORT),tmpObj));
                    if(StringUtil.isEmptyStr(time)){
                        continue TWO;
                    }
                    String titleName = "-";
                    if(SWIMLANCE_SHOW_STR_NAME.has(key)){
                        titleName = JsonAttrUtil.getStringValue(key,SWIMLANCE_SHOW_STR_NAME);
                    }else {
                        titleName = key;
                    }
                    tmpObj.addProperty("type",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(MEDICAL_RECORDS).get("type").getAsString());
                    tmpObj.addProperty("unfold",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(MEDICAL_RECORDS).get("unfold").getAsBoolean());
                    tmpObj.addProperty("port",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(MEDICAL_RECORDS).get("port").getAsString());
                    tmpObj.addProperty("titleName",titleName);
                    tmpObj.addProperty("configSchema",key);
                    if(!timeMap.containsKey(time)){
                        timeMap.put(time,new LinkedList<>());
                    }

                    timeMap.get(time).add(tmpObj);
                }
            }
            putTimeLinesValues(timeMap,timeLines,MEDICAL_RECORDS);
        }

    }

    private void getOrders(Map<String, JsonObject> timeLines, String param) {
        String orders = "orders";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            orders= "non_drug_orders";
        }
        ResponseInterface vt=new PaginationMemoryResponse(new SortResponse(new VisitSNResponse(orders,orders),orders, QueryResult.getSortKey(orders),false),orders);
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        paramJson.addProperty("page_size",Integer.MAX_VALUE-1);
        paramJson.addProperty("currentPage",1);
        if(paramJson==null){
            return;
        }
        vt.execute(JsonAttrUtil.toJsonObject(paramJson));
        JsonObject obj = vt.get_result();
        if(obj == null){
            return ;
        }
        JsonArray res = obj.get(orders).getAsJsonArray();
        transForArrayTimeMap(res, timeLines, NON_ORDER,orders);
    }

    private void getOperationRecods(Map<String, JsonObject> timeLines, String param) {
        String operation_record = "operation_records";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            operation_record = "operation_record";
        }
        VisitSNResponse vt=new VisitSNResponse(
            new String[]{operation_record}
        );
        vt.execute(JsonAttrUtil.toJsonObject( JsonAttrUtil.toJsonObject(param)));
        JsonObject obj = vt.get_result();
        if(obj == null){
            return ;
        }
        JsonArray array = obj.getAsJsonArray(operation_record);
        Map<String,List<JsonObject>> timeMap = new HashMap<>();
        for (JsonElement element : array){
            JsonObject elObj = element.getAsJsonObject();
            String date = JsonAttrUtil.getStringValue("OPERATION_DATE",elObj);
            if(StringUtil.isEmptyStr(date)){
                continue;
            }
            String titleName = JsonAttrUtil.getStringValue("OPERATION",elObj);
            if(StringUtil.isEmptyStr(titleName)){
                titleName = "手术记录";
            }
            elObj.addProperty("type",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(OPERATION).get("type").getAsString());
            elObj.addProperty("unfold",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(OPERATION).get("unfold").getAsBoolean());
            elObj.addProperty("port",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(OPERATION).get("port").getAsString());
            elObj.addProperty("titleName",titleName);
            elObj.addProperty("configSchema",operation_record);
            String time = DateUtil.getDateStr_ymd(date);
            if(!timeMap.containsKey(time)){
                timeMap.put(time,new LinkedList<>());
            }
            timeMap.get(time).add(elObj);
            if(!timeLines.containsKey(time)){
                timeLines.put(time,new JsonObject());
            }
            timeLines.get(time).addProperty("OPERATION_DATE",time);
        }
        putTimeLinesValues(timeMap,timeLines,OPERATION);
    }

    private void getLabResultItem(Map<String, JsonObject> timeLines, String param) {
        String patient_sn = null;
        String visit_sn = null;

        LinkedList<JsonElement> data_array = new LinkedList<JsonElement>();

        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }

        String INSPECTION_SN =  "inspection_reports.INSPECTION_SN";
        String INSPECTION_NAME = "inspection_reports.INSPECTION_NAME";
        String SPECIMEN_NAME = "inspection_reports.SPECIMEN_NAME";
        String SUBMITTING_DEPARTMENT = "inspection_reports.SUBMITTING_DEPARTMENT";
        String ACQUISITION_TIME = "inspection_reports.ACQUISITION_TIME";
        String RECEIVE_TIME ="inspection_reports.RECEIVE_TIME";
        String REPORT_TIME = "inspection_reports.REPORT_TIME";
        String REPORT_TIME_KEY = "REPORT_TIME";
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
        }
        QueryParam qp = new QueryParam(param_json, patient_sn, new String[]{
            INSPECTION_SN,
            INSPECTION_NAME,
            SPECIMEN_NAME,
            SUBMITTING_DEPARTMENT,
            ACQUISITION_TIME,
            RECEIVE_TIME,
            REPORT_TIME
        });

        GroupVisitSearch groupVisitSearch = new GroupVisitSearch(BeansContextUtil.getUrlBean().getVisitIndexName(),patient_sn,visit_sn);
        groupVisitSearch.addSource(qp.getSource());
        String vis = HttpRequestUtils.getSearchEmr(JsonAttrUtil.toJsonStr(groupVisitSearch));
        JsonObject visit = JsonAttrUtil.toJsonObject(vis);

        if (visit == null) {
            return;
        }
        JsonArray visits = visit.getAsJsonArray(inspection_reports);
        if(visits == null){
            return;
        }
        Map<String,List<JsonObject>> timeMap = new HashMap<>();
        for (JsonElement element : visits){
            JsonObject object = element.getAsJsonObject();
            String titleName = JsonAttrUtil.getStringValue("INSPECTION_NAME",object);
            if(StringUtil.isEmptyStr(titleName)){
                titleName = "实验室检验";
            }
            object.addProperty("type",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(INSPECTION_REPORTS).get("type").getAsString());
            object.addProperty("unfold",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(INSPECTION_REPORTS).get("unfold").getAsBoolean());
            object.addProperty("port",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(INSPECTION_REPORTS).get("port").getAsString());
            object.addProperty("titleName",titleName);
            object.addProperty("configSchema",inspection_reports);
            String time = DateUtil.getDateStr_ymd(JsonAttrUtil.getStringValue(REPORT_TIME_KEY,object));
            if(StringUtil.isEmptyStr(time)){
                continue;
            }
            if (!timeMap.containsKey(time)) {
                timeMap.put(time,new LinkedList<>());
            }
            timeMap.get(time).add(object);
        }
        putTimeLinesValues(timeMap,timeLines,INSPECTION_REPORTS);

    }

    private void getExamResult(Map<String, JsonObject> timeLines, String param) {
        String[] keys = new String[]{
            "ultrasonic_diagnosis_reports",//超声检查
            "xray_image_reports",//X线影像诊断
            "ct_reports",//ct检查
            "ect_reports",//ect检查
            "mr_reports",//mr检查
            "pet_ct_reports",//pet_ct检查
            "pet_mr_reports",//pet_mr检查
            "microscopic_exam_reports",//镜像检查
            "lung_functional_exam",//肺功能检查
            "other_imaging_exam_diagnosis_reports",
            "electrocardiogram_reports"
        };
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            keys = new String[]{
                "ultrasonic_diagnosis_report",//超声检查
                "lung_functional_exam",
                "imaging_exam_diagnosis_report"
        };
        }
        ResponseInterface template = ImageResponseUtils.getImageResponseInterface(keys);
        template.execute(JsonAttrUtil.toJsonObject(param));
        JsonObject result = template.get_result();
        if (result != null) {
            Map<String,List<JsonObject>> timeMap = new HashMap<>();
            ONE: for (Map.Entry<String,JsonElement> entry : result.entrySet()){
                String key = entry.getKey();
                JsonArray array = entry.getValue().getAsJsonArray();
                TWO: for (JsonElement element : array){
                    JsonObject tmpObj = element.getAsJsonObject();
                    String time = DateUtil.getDateStr_ymd(tmpObj.get("REPORT_DATE").getAsString());
                    if(StringUtil.isEmptyStr(time)){
                        continue TWO;
                    }
                    String tmpKey = JsonAttrUtil.getStringValue(key,SWIMLANCE_SHOW_NAME) ;
                    String titleName = StringUtil.isEmptyStr(tmpKey) ? "" : JsonAttrUtil.getStringValue(tmpKey,tmpObj);
                    if(StringUtil.isEmptyStr(titleName)){
                        titleName = JsonAttrUtil.getStringValue(key,SWIMLANCE_SHOW_STR_NAME);
                    }
                    tmpObj.addProperty("type",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(IMAGING_REPORTS).get("type").getAsString());
                    tmpObj.addProperty("unfold",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(IMAGING_REPORTS).get("unfold").getAsBoolean());
                    tmpObj.addProperty("port",SWIMLANCE_SHOW_CONFIG.getAsJsonObject(IMAGING_REPORTS).get("port").getAsString());
                    tmpObj.addProperty("titleName",titleName);
                    tmpObj.addProperty("configSchema",key);
                    if(!timeMap.containsKey(time)){
                        timeMap.put(time,new LinkedList<>());
                    }
                    timeMap.get(time).add(tmpObj);
                }
            }
            putTimeLinesValues(timeMap,timeLines,IMAGING_REPORTS);
        }

    }

    public void putTimeLinesValues( Map<String,List<JsonObject>> timeMap,Map<String, JsonObject> timeLines,String key){
        for (Map.Entry<String,List<JsonObject>> entry : timeMap.entrySet()){
            String time = entry.getKey();
            entry.getValue().sort((o1, o2) -> {
                String key1 = JsonAttrUtil.getStringValue("configSchema",o1);
                String key2 = JsonAttrUtil.getStringValue("configSchema",o2);
                return JsonAttrUtil.getStringValue(JsonAttrUtil.getStringValue(key1,SWIMLANCE_SORT),o1).compareTo(JsonAttrUtil.getStringValue(JsonAttrUtil.getStringValue(key2,SWIMLANCE_SORT),o2));
            });
            if(!timeLines.containsKey(time)){
                timeLines.put(time,new JsonObject());
            }
            timeLines.get(time).add(key, JsonAttrUtil.toJsonTree(entry.getValue()));
        }
    }

    private JsonObject getDiannoseCount(JsonObject param_json) {
        VisitSNResponse snResponse = new VisitSNResponse(new String[]{"diagnose","visit_info"});
        snResponse.execute(param_json);
        JsonObject visit= snResponse.get_result();
        JsonObject resultObj = new JsonObject();
        if(visit == null){
            return resultObj;
        }
        transforDiagnose(resultObj,visit);
        transforVisitInfo(resultObj,visit);
        return resultObj;
    }

    private void transforVisitInfo(JsonObject resultObj, JsonObject visit) {
        if(visit.has("visit_info")){
            JsonArray visitArray = visit.get("visit_info").getAsJsonArray();
            for (JsonElement element : visitArray){
                JsonObject obj = element.getAsJsonObject();
                //入院（就诊）时间
                String admissionDate = JsonAttrUtil.getStringValue("ADMISSION_DATE",obj);
                //出院时间
                String dischargeDate = JsonAttrUtil.getStringValue("DISCHARGE_DATE",obj);
                //入院（就诊）科室名称
                String ADMISSION_DEPT = JsonAttrUtil.getStringValue("ADMISSION_DEPT",obj);
                //就诊医生姓名
                String docterName = JsonAttrUtil.getStringValue("ADMISSION_DOCTOR",obj);
                //天数
                String days = "-";
                if(StringUtil.isNotEmptyStr(admissionDate) && StringUtil.isNotEmptyStr(dischargeDate)){
                    long day = DateUtil.getDurationWithDays(DateUtil.getDate(admissionDate), DateUtil.getDate(dischargeDate)) + 1 ;
                    days = String.valueOf(day);
                }
                addResultObj(resultObj,obj,new String[]{"ADMISSION_DATE","DISCHARGE_DATE","ADMISSION_DEPT","ADMISSION_DOCTOR"});
                resultObj.addProperty("DURATION_TIME",days);
                System.out.println();
            }
        }
    }

    private void transforDiagnose(JsonObject resultObj, JsonObject visit) {
        if(visit.has("diagnose")){
            JsonArray diagnoseArray = visit.getAsJsonArray("diagnose");
            List<JsonElement> list = JsonAttrUtil.jsonArrayToList(diagnoseArray);
            list = JsonAttrUtil.sort(list, new JsonComparatorASCByKey("DIAGNOSTIC_DATE"));
            boolean admission = true;
            for (int i = 0; i < list.size(); i++) {
                JsonObject obj = list.get(i).getAsJsonObject();
                String DIAGNOSIS = JsonAttrUtil.getStringValue("DIAGNOSIS",obj);
                String DIAGNOSIS_TYPE = JsonAttrUtil.getStringValue("DIAGNOSIS_TYPE",obj);
                String MAIN_DIAGNOSIS_FLAG = JsonAttrUtil.getStringValue("MAIN_DIAGNOSIS_FLAG",obj);
                if(i == 0){
                    addResultObj(resultObj,obj,new String[]{"DIAGNOSIS","DIAGNOSIS_TYPE"});
                }
                if(admission && "入院诊断".equals(DIAGNOSIS_TYPE)){
                    addResultObj(resultObj,obj,new String[]{"DIAGNOSIS","DIAGNOSIS_TYPE"});
                    admission = false;
                }
                if ("出院主要诊断".equals(DIAGNOSIS_TYPE) && "true".equals(MAIN_DIAGNOSIS_FLAG)){
                    addResultObj(resultObj,obj,new String[]{"DIAGNOSIS","DIAGNOSIS_TYPE"});
                    break;
                }
            }
        }
    }

    private void addResultObj(JsonObject resultObj,JsonObject sourceObj, String[] strs) {
        for (String str : strs){
            resultObj.addProperty(str, JsonAttrUtil.getStringValue(str,sourceObj));
        }
    }

}