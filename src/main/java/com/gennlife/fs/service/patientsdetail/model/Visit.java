package com.gennlife.fs.service.patientsdetail.model;


import com.gennlife.fs.common.utils.DateUtil;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.serviceitem.VisitImage;
import com.gennlife.fs.system.config.DiseaseCodeConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Visit implements Comparable<Visit> {
    private static Logger logger = LoggerFactory.getLogger(Visit.class);
    private String[] tmp;


    public static String[] getVisits_map_keys() {
        return visits_map_keys;
    }

    public static final String[] visits_map_keys = new String[]{
            "diagnose", "imaging_exam_diagnosis_reports",
            "pathology_reports", "genomics", "operation",
            "systemic_therapy", "radiotherapy", "evaluation", "follow_up", "adverse_event"
    };
    public static final String TIME_EMPTY = "-";
    private static final String UNKNOWN = "未知";
    private String patient_sn = "";
    private String visit_sn = "";
    private String visit_date = "";//入院时间
    private String visit_type = "";//门诊/住院/急诊
    private String visit_dept = "";
    private String discharge_out_date = "";//出院时间
    private String diagnosis_department = "";
    private boolean main_diagnosis_flag = false;
    private String disease_name = ""; //疾病名称
    private String diagnosis_type_name = "";
    private String diagnosis_date = "";
    private String disease_code = "";//疾病代码
    private String disease_code_type = "";//疾病种类
    private List<String> event = new ArrayList<String>();
    private JsonObject visit_info = null;
    private JsonObject main_diagnose_jason;
    private HashMap<String, JsonElement> visits_map = null;
    private Date visit_item_out_date = null;
    private Date visit_item_in_date = null;
    private JsonArray disease_normal_code;
    private JsonArray disease_normal_name;

    public Visit(JsonObject visit) {
        super();
        if (visit.has("visit_info")) {
            JsonElement visitelem = visit.get("visit_info");
            if (visitelem.isJsonArray() && visitelem.getAsJsonArray().size() > 0) {
                this.visit_info = visitelem.getAsJsonArray().get(0).getAsJsonObject();
                this.patient_sn = getJsonValue(visit_info, "PATIENT_SN", "");
                this.visit_sn = getJsonValue(visit_info, "VISIT_SN", "");
                this.visit_type = getJsonValue(visit_info, "VISIT_TYPE_NAME", "");
                if (StringUtil.isEmptyStr(this.visit_type)) {
                    this.visit_type = getJsonValue(visit_info, "VISIT_TYPE", "");
                }
                visit_date = StringUtil.get_visit_date(this.visit_info);
                if (StringUtil.isEmptyStr(visit_date))
                    visit_date = TIME_EMPTY;
                if (this.visit_info.has("REGISTERED_DEPT")) {
                    this.visit_dept = this.visit_info.get("REGISTERED_DEPT").getAsString();
                } else if (this.visit_info.has("ADMISSION_DEPT")) {
                    this.visit_dept = this.visit_info.get("ADMISSION_DEPT").getAsString();
                } else {
                    this.visit_dept = UNKNOWN;
                }

                if (this.visit_info.has("EVENT_TYPE") && this.visit_info.get("EVENT_TYPE").isJsonArray()) {
                    for (JsonElement elem : this.visit_info.get("EVENT_TYPE").getAsJsonArray()) {
                        this.event.add(elem.getAsString());
                    }

                }
                if (this.visit_info.has("REGISTERED_DEPT")) {
                    this.diagnosis_department = this.visit_info.get("REGISTERED_DEPT").getAsString();
                } else if (this.visit_info.has("ADMISSION_DEPT")) {
                    this.diagnosis_department = this.visit_info.get("ADMISSION_DEPT").getAsString();
                } else {
                    this.diagnosis_department = UNKNOWN;
                }
            }
        }

        if (!JsonAttrUtil.isEmptyArray(visit, "diagnose")) {
            diagnoseOperate(visit);
        } else {
            // logger.debug("Visit construct: ... no diagnose " );
            this.disease_name = UNKNOWN;
            disease_normal_code = new JsonArray();
            this.diagnosis_department = UNKNOWN;
            this.diagnosis_type_name = UNKNOWN;
            this.disease_code = UNKNOWN;
            this.diagnosis_date = visit_date;
            disease_normal_name = new JsonArray();

        }

    }


    public void diagnoseOperate(JsonObject visit) {
        JsonArray diagnose = visit.get("diagnose").getAsJsonArray();
        boolean has_main_diag = false;
        main_diagnose_jason = null;
        for (JsonElement one_diagnose : diagnose) {
            JsonObject one_diagnose_jason = one_diagnose.getAsJsonObject();

            if (one_diagnose_jason.has("MAIN_DIAGNOSIS_FLAG")
                    && one_diagnose_jason.get("MAIN_DIAGNOSIS_FLAG").getAsBoolean()) {
                main_diagnose_jason = one_diagnose_jason;
                has_main_diag = true;
                //logger.debug("Visit construct: main diagnose... " + one_diagnose_jason.toString());
                this.main_diagnosis_flag = true;
                break;
            }
        }
        if (!has_main_diag) { //如果没有主诊断，则选择一个非主诊断
            this.main_diagnosis_flag = false;
            if (diagnose.size() > 0)
                main_diagnose_jason = diagnose.get(0).getAsJsonObject();
        }
        //processNormal(main_diagnose_jason);
        this.disease_name = getJsonValue(main_diagnose_jason, "DIAGNOSIS", UNKNOWN);
        this.diagnosis_department = StringUtil.getDept(visit_info);
        if (StringUtil.isEmptyStr(diagnosis_department)) {
            diagnosis_department = UNKNOWN;
        }
        //诊断类别名称
        this.diagnosis_type_name = getJsonValue(main_diagnose_jason, "DIAGNOSIS_TYPE", UNKNOWN);
        this.disease_code = getJsonValue(main_diagnose_jason, "DIAGNOSIS_CODE", UNKNOWN);
        this.discharge_out_date = getJsonValue(main_diagnose_jason, "DISCHARGE_DATE", "");
        this.diagnosis_date = getJsonValue(main_diagnose_jason, "DIAGNOSTIC_DATE", TIME_EMPTY);
        //this.diagnosis_department = getJsonValue(main_diagnose_jason, "DIAGNOSTIC_DEPT", UNKNOWN);
        this.disease_normal_code = main_diagnose_jason.getAsJsonArray("DIAGNOSIS_NORMAL_CODE");
        this.disease_normal_name = main_diagnose_jason.getAsJsonArray("DIAGNOSIS_NORMAL");

    }

    public void init_normal_code(JsonArray diagnose) {
        if (diagnose == null || diagnose.size() == 0) return;
        this.disease_normal_code = new JsonArray();
        for (JsonElement one_diagnose : diagnose) {
            if (one_diagnose.getAsJsonObject().has("DIAGNOSIS_NORMAL_CODE")) {
                try {
                    Iterator<JsonElement> iterator = one_diagnose.getAsJsonObject().getAsJsonArray("DIAGNOSIS_NORMAL_CODE").iterator();
                    while (iterator.hasNext()) {
                        String tmpItem = iterator.next().getAsString();
                        this.disease_normal_code.add(tmpItem.substring(0, 3) + "." + tmpItem.substring(3));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (one_diagnose.getAsJsonObject().has("DIAGNOSIS_CODE")) {
                this.disease_code = "";
                String key = one_diagnose.getAsJsonObject().get("DIAGNOSIS_CODE").getAsString();
                if (!StringUtil.isEmptyStr(key))
                    this.disease_normal_code.add(one_diagnose.getAsJsonObject().get("DIAGNOSIS_CODE").getAsString());

            }
        }

    }

    public JsonElement getMapValue(String key) {
        if (!visits_map.containsKey(key)) return null;
        return visits_map.get(key);
    }

    public JsonElement removeMapValue(String key) {

        if (!visits_map.containsKey(key)) return null;
        JsonElement result = visits_map.get(key);
        visits_map.remove(key);
        return result;
    }

    public Visit saveClassicfyData(JsonObject visitItem, JsonObject adds) {
        //添加时间
        if (!saveTimeIntomap()) return null;

        if (!visitItem.has("diagnose")) return null;
        visits_map = new HashMap<String, JsonElement>();
        //诊断
        //crf
        JsonObject diagnose_json = new JsonObject();
        JsonArray arr = visitItem.get("diagnose").getAsJsonArray();
        for (int i = 0; i < arr.size(); i++) {
            try {
                JsonElement tmp = arr.get(i);
                if (tmp.getAsJsonObject().get("MAIN_DIAGNOSIS_FLAG").getAsBoolean()) {
                    arr.set(i, arr.get(0));
                    arr.set(0, tmp);
                    break;
                }
            } catch (Exception e) {
                continue;
            }
        }
        diagnose_json.add("diagnose", arr);
        JsonAttrUtil.add_propery(visitItem, diagnose_json, "hospitalization", "outpatient");
        visits_map.put("diagnose", diagnose_json);


        //影像
        save_imaging(visitItem);


        //病理
        savePathologyIntoMap(visitItem);


        //手术
        if (visitItem.has("operation_records")) {
            JsonObject operation = new JsonObject();
            JsonArray operations = visitItem.getAsJsonArray("operation_records");
            if (operations != null) operation.add("operation", operations);
            JsonAttrUtil.add_propery(visitItem, operation, "OPERATION_INFO", "OPERATION_PROCESS");
            visits_map.put("operation", operation);
        }
        //分子检测
        saveGenomices(visitItem, adds);

        //系统治疗

        saveSystemicTherapyIntoMap(visitItem);

        //放疗
        JsonObject radiotherapy = new JsonObject();
        JsonAttrUtil.add_propery(visitItem, radiotherapy, "radiotherapy", "radiotherapy_orders");
        if (radiotherapy.entrySet().size() > 0)
            visits_map.put("radiotherapy", radiotherapy);
        //疗效评估 字段是啥
        //"evaluation"

        //随访 指的是什么随访
        //"follow_up"


        //不良事件
        //"adverse_event"
        if (visitItem.has("AE_combination_drug")) {
            JsonObject adverse_event = new JsonObject();
            adverse_event.add("adverse_event", visitItem.get("AE_combination_drug"));
            visits_map.put("adverse_event", adverse_event);
            //saveIntoMapNotFilter(visitItem,"AE_combination_drug","adverse_event");
        }

        //补充数据
        eventAnalise();
        return this;
    }

    protected void saveGenomices(JsonObject visitItem, JsonObject adds) {
    /*if(adds!=null && adds.has("genomics"))
    {
        if(event.contains("第1次手术"))
        {
            saveIntoMapNotFilter(adds, "genomics");
            adds.remove("genomics");
            JsonArray genomicsArray = visits_map.get("genomics").getAsJsonArray();
            genomicsArray = JsonAttrUtil.setAttr("VISIT_SN", visit_sn, genomicsArray);
            JsonObject genomicsJson = new JsonObject();
            genomicsJson.add("genomics",genomicsArray);
            visits_map.put("genomics", genomicsJson);
        }
    }*/
        if (adds != null && JsonAttrUtil.checkKeys(adds, new String[]{"surgery_date", "genomics"})) {
            if (visitItem.has("operation"))//有手术
            {
                // logger.info(visitItem.toString());
                // logger.info("has operation");
                //JsonArray op_arr=JsonAttrUtil.getJsonArrayValue("operation.records",visitItem);
                // for (JsonElement op_arr_elem : visitItem.get("operation").getAsJsonArray()) {
                JsonArray op_arr = visitItem.getAsJsonArray("operation_records");
                for (JsonElement op_elem : op_arr) {
                    if (op_elem.getAsJsonObject().has("OPERATION_DATE")) {
                        if (op_elem.getAsJsonObject().get("OPERATION_DATE").getAsString()
                                .startsWith(adds.get("surgery_date").getAsString())) {
                            saveIntoMapNotFilter(adds, "genomics");
                                /*logger.info("OPERATION_DATE "+op_elem.getAsJsonObject().get("OPERATION_DATE").getAsString()+
                                        " surgery_date "+adds.get("surgery_date").getAsString());*/
                            adds.remove("surgery_date");
                            adds.remove("genomics");
                            JsonArray genomicsArray = visits_map.get("genomics").getAsJsonArray();
                            genomicsArray = JsonAttrUtil.setAttr("VISIT_SN", visit_sn, genomicsArray);
                            JsonObject genomicsJson = new JsonObject();
                            genomicsJson.add("genomics", genomicsArray);
                            visits_map.put("genomics", genomicsJson);
                            return;
                        }
                    }
                }
            }
            // }

        }
    }

    /*  private Map.Entry<String,String> missing data(String key)
      {

          Entry<String, String> entry = new Map.Entry<String, String>();
      }*/
    private void eventAnalise() {
        // 系统治疗-化疗  系统治疗-靶向治疗 系统治疗-DC治疗  放疗  手术
        String outline_key = null;
        String group_key = null;
        //"chemotherapy","DC_orders","targeted_drug"
        for (String key : event) {
            //系统治疗
            if (key.contains("化疗")) {
                group_key = "systemic_therapy";
                outline_key = "chemotherapy";
            } else if (key.contains("靶向治疗")) {
                group_key = "systemic_therapy";
                outline_key = "targeted_drug";
            } else if (key.contains("DC治疗")) {
                group_key = "systemic_therapy";
                outline_key = "DC_orders";
            } else if (key.contains("放疗")) {
                group_key = "radiotherapy";
                outline_key = "radiotherapy";
            } else if (key.contains("手术")) {
                group_key = "operation";
                outline_key = "operation";
            } else
                continue;
            addEmpty(group_key, outline_key);

        }
    }

    private void addEmpty(String group, String key) {
        if (visits_map.containsKey(group)) {
            JsonObject json = visits_map.get(group).getAsJsonObject();
            if (json.entrySet().size() > 0) {
                if (!json.has(key)) json.add(key, new JsonArray());
            }
        } else
            visits_map.put(group, new JsonObject());
    }


    private void save_imaging(JsonObject visitItem) {
        JsonObject imaging_json = new JsonObject();
        if (visitItem.has("imaging_exam_diagnosis_reports")) {
            JsonArray imaging_exam_array = visitItem.get("imaging_exam_diagnosis_reports").getAsJsonArray();
            imaging_exam_array = JsonAttrUtil.setAttr(VisitImage.IMAGE_TYPE_KEY, VisitImage.IMAGE_TYPE_IMAGING_EXAM, imaging_exam_array);
            imaging_json.add("imaging_exam_diagnosis_reports", imaging_exam_array);
        }
        JsonAttrUtil.add_propery(visitItem, imaging_json, "CT", "MRI", "IVP");
        if (imaging_json.entrySet().size() > 0)
            visits_map.put("imaging_exam_diagnosis_reports", imaging_json);
    }

    private void OperationsCRF(JsonArray operations, int i, JsonElement item, String groupkey, String propkey) {
        if (item.getAsJsonObject().has(groupkey)) {
            JsonElement op = item.getAsJsonObject().get(groupkey);
            JsonObject tmpdata = null;
            if (op.isJsonArray()) tmpdata = op.getAsJsonArray().get(0).getAsJsonObject();
            else tmpdata = op.getAsJsonObject();
            operations.get(i)
                    .getAsJsonObject().add(propkey, tmpdata.get(propkey));
        }
    }

    private void saveIntoMapNotFilter(JsonObject visitItem, String key) {
        saveIntoMapNotFilter(visitItem, key, key);
    }

    //添加时间
    private boolean saveTimeIntomap() {
        String[] tmp = null;

        if (!StringUtil.isDateEmpty(visit_date)) {
            tmp = visit_date.split(" ");
            if (tmp.length > 0) this.visit_item_in_date = DateUtil.getDate_ymd(tmp[0]);
        }
        if (!StringUtil.isDateEmpty(discharge_out_date)) {
            tmp = discharge_out_date.split(" ");
            if (tmp.length > 0)
                this.visit_item_out_date = DateUtil.getDate_ymd(tmp[0]);
        }
        if (this.visit_item_out_date == null && this.visit_item_in_date == null) return false;
        return true;


    }

    private void saveSystemicTherapyIntoMap(JsonObject visitItem) {
        JsonObject systemic_therapy = new JsonObject();
        JsonAttrUtil.add_propery(visitItem, systemic_therapy, "chemotherapy", "DC_orders", "targeted_drug");
        if (systemic_therapy.entrySet().size() > 0) {
            visits_map.put("systemic_therapy", systemic_therapy);
        }

    }

    /**
     * array
     * --key(jsonObject)
     * --array
     */
   /* private void saveIntoMapNotFilter(JsonArray array,String key, String hashkey)
    {
        if(array==null || array.size()==0 ) return ;
        JsonArray all=null;
        if(array.get(0).getAsJsonObject().has(key))
        {
            for(JsonElement arrayItem:array)
            {
                JsonElement tmp = arrayItem.getAsJsonObject().get(key);
                if(all==null) all=tmp.getAsJsonArray();
                else all.addAll(tmp.getAsJsonArray());
            }
            if(all !=null )visits_map.put(hashkey,all);
        }
    }*/
    private JsonArray get_all_data(JsonArray array, String key) {
        if (array == null || array.size() == 0) return null;
        JsonArray all = null;
        if (array.get(0).getAsJsonObject().has(key)) {
            for (JsonElement arrayItem : array) {
                JsonElement tmp = arrayItem.getAsJsonObject().get(key);
                if (all == null) all = tmp.getAsJsonArray();
                else all.addAll(tmp.getAsJsonArray());
            }
            if (all != null) return all;
        }
        return null;
    }

    private void saveIntoMapNotFilter(JsonObject visitItem, String key, String hashkey) {
        if (visitItem.has(key)) {
            visits_map.put(hashkey, visitItem.getAsJsonArray(key));
        }

    }

    private void savePathologyIntoMap(JsonObject visitItem) {

        JsonArray pathology = visitItem.getAsJsonArray("pathology_reports");
        JsonArray cell_pathology = visitItem.getAsJsonArray("cell_pathology_reports");

        JsonObject pathologyobj = new JsonObject();
        if (pathology == null && cell_pathology == null) return;
        //细胞病理学 cell
        if (cell_pathology != null) {

            cell_pathology = JsonAttrUtil.setAttr(VisitImage.IMAGE_TYPE_KEY, VisitImage.IMAGE_TYPE_PATHOLOGY, cell_pathology);
            pathologyobj.add("cell_pathology_reports", cell_pathology);
        }
        if (pathology != null) {
            pathology = JsonAttrUtil.setAttr(VisitImage.IMAGE_TYPE_KEY, VisitImage.IMAGE_TYPE_PATHOLOGY, pathology);
            pathologyobj.add("pathology_reports", pathology);
        }

        if (pathologyobj.entrySet().size() > 0) {
            visits_map.put("pathology_reports", pathologyobj);
        }


    }

    //查找疾病类别
    public String findDiseaseType() {

        return this.disease_code_type;

    }

    //疾病归类
    public boolean init_disease_Type() {
        String normal_code = UNKNOWN;
        if (this.main_diagnose_jason == null)
            return false;
        else {
            try {
                normal_code = this.disease_normal_code.get(0).getAsString();
            } catch (Exception e) {
                normal_code = UNKNOWN;
            }
            if (normal_code.equals(UNKNOWN) || normal_code.equals("")) {
                return false;
            } else {
                disease_code_type = new DiseaseCodeConfig().decode(normal_code);

                if (disease_code_type == null)
                    return false;
                else
                    return true;
            }

        }


    }

    //伴随疾病,进行按照ICD编码进行归类
    //该类疾病下的诊断名称、门诊：诊断时间、住院：入院时间、出院时间
    //疾病的大类
    public JsonObject get_classify_other_detail() {
        JsonObject json = new JsonObject();
        json.addProperty("visit_date", visit_date);
        if (discharge_out_date.equals("")) {
            json.addProperty("duration", 0);
        } else {
            json.addProperty("discharge_out_date", discharge_out_date);
            json.addProperty("duration", DateUtil.getDurationWithDays(visit_date, discharge_out_date));
        }
        json.addProperty("disease_name", disease_name);
        json.addProperty("disease_type", findDiseaseType());
        json.addProperty("visit_sn", visit_sn);
        return json;
    }

    public String get_visit_date() {
        return visit_date;
    }

    public String get_visit_date_year() {
        if (StringUtil.isDateEmpty(visit_date)) return TIME_EMPTY;
        return visit_date.substring(0, 4);
    }

    public void setVisitDate(String date) {
        visit_date = date;
    }

    public String get_disease() {
        return disease_name;
    }

    public String get_visit_type() {
        return visit_type;
    }

    public String get_visit_dept() {
        return visit_dept;
    }

    public String get_disease_code() {
        return disease_code;
    }

    public JsonArray get_disease_normal_code() {
        return disease_normal_code;
    }

    public List<String> get_visit_event() {
        return event;
    }

    /**
     * 剔除次数的后的event item值
     * 重大事件
     */
    public String get_visit_event_item(int index) {
        if (index < 0 || index > event.size()) return "";
        String value = get_visit_event().get(index);
        if (value.equals("")) return "";
        //不去掉生存状态
       /* for(String item: EventStatusUtil.get_patient_status())
            if(item.equals(value)) return "";
        */
        String timevalue = StringUtil.getTimesEvent(value);
        if (timevalue != null) {
            if (event.contains(timevalue)) return "";
        }

        return value;
    }

    /**
     * 手术，第一次手术，保留第一次手术
     */
    public void remain_visit_event_item() {
        if (event == null) return;
        Iterator<String> iter = event.iterator();
        while (iter.hasNext()) {
            String item = iter.next();
            item = StringUtil.getTimesEvent(item);
            if (!StringUtil.isEmptyStr(item)) {
                if (event.remove(item)) {
                    iter = event.iterator();
                }

            }

        }
    }

    //获取事件tag
    public List<String> get_event_type() {
        return event;
    }

    @Override
    public int compareTo(Visit o) {
        return visit_date.compareTo(o.visit_date);
    }

    public Date getVisit_item_out_date() {
        return visit_item_out_date;
    }

    public Date getVisit_item_in_date() {
        return visit_item_in_date;
    }

    private String getJsonValue(JsonObject json, String key, String default_value) {
        if (json == null) return default_value;
        if (json.has(key)) return json.get(key).getAsString();
        else return default_value;
    }

    public boolean ismapEmpty() {
        return visits_map == null || visits_map.isEmpty();

    }

    public String get_diagnosis_type_name() {
        return diagnosis_type_name.equals(UNKNOWN) ? "" : diagnosis_type_name;
    }

    public String get_diagnosis_date() {
        return diagnosis_date;
    }

    public String get_disease_name() {
        return disease_name;
    }

    public boolean get_main_diagnosis_flag() {
        return main_diagnosis_flag;
    }

    public String get_visit_sn() {
        return visit_sn;
    }

    public String get_patient_sn() {
        return patient_sn;
    }

    public String get_diagnosis_department() {
        return diagnosis_department;
    }

    public JsonArray get_disease_normal_name() {
        return this.disease_normal_name;
    }

    public static JsonObject processNormal(JsonObject diagnose) {
        processNormalkey(diagnose, "DIAGNOSIS_NORMAL", "DIAGNOSIS", UNKNOWN);
        processNormalkey(diagnose, "DIAGNOSIS_NORMAL_CODE", "DIAGNOSIS_CODE", "");
        return diagnose;
    }

    private static void processNormalkey(JsonObject diagnose, String normalkey, String key, String value) {
        if (!diagnose.has(normalkey)) {
            JsonArray array = JsonAttrUtil.strToJsonArray(diagnose, key);
            diagnose.add(normalkey, array);
            if (array.size() > 0) diagnose.addProperty(key, value);
        }

    }
}