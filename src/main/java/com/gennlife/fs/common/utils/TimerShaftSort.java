package com.gennlife.fs.common.utils;

import com.gennlife.fs.configurations.GeneralConfiguration;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;
import static com.gennlife.fs.configurations.model.Model.emrModel;

/**
 * @author
 * @create 2019 29 17:23
 * @desc
 **/
public class TimerShaftSort {
    private static Map<String,String> examResult = new LinkedHashMap<>();  //检查
    private static Map<String,String> medicalCourse = new LinkedHashMap<>(); //病例文书
    private static Map<String,String> courseRecords = new LinkedHashMap<>(); //病程文书
    private static Map<String,String> operation = new LinkedHashMap<>();//手术
    private static Map<String,String> sortMap = new HashMap<>();
    private static Map<String,String> examResultName = new HashMap<>();

    private static class SingleTimerShaftSortInstance{
        private static final TimerShaftSort instance = new TimerShaftSort();
    }
    private TimerShaftSort(){};
    public static TimerShaftSort getInstance(){
        return SingleTimerShaftSortInstance.instance;
    }

    static{
        courseRecords.put("admissions_records","入院记录");
        courseRecords.put("discharge_records","出院记录");
        courseRecords.put("first_course_records","首次病程记录");
        courseRecords.put("attending_physician_rounds_records","上级医师查房记录");
        courseRecords.put("course_record","日常病程记录");
        courseRecords.put("post_course_record","术后病程记录");

        courseRecords.put("admissions_record","入院记录");
        courseRecords.put("discharge_record","出院记录");
        courseRecords.put("first_course_record","首次病程记录");
        courseRecords.put("attending_physician_rounds_record","上级医师查房记录");
        courseRecords.put("course_record","日常病程记录");
        courseRecords.put("operation_post_course_record","术后病程记录");
        courseRecords.put("admission_discharge_record","24小时入出院记录");
        courseRecords.put("admission_death_record","24小时入院死亡记录");
    }

    static {
        medicalCourse.put("discharge_summary","出院小结");
        medicalCourse.put("death_discuss_records","死亡病例讨论记录");
        medicalCourse.put("death_records","死亡记录");
        medicalCourse.put("death_summary","死亡小结");
        medicalCourse.put("difficulty_case_records","疑难病例讨论记录");
        medicalCourse.put("handover_record","交接班记录");
        medicalCourse.put("rescue_records","抢救记录");
        medicalCourse.put("stage_summary","阶段小结");
        medicalCourse.put("transferred_in_records","转入记录");
        medicalCourse.put("transferred_out_records","转出记录");

        medicalCourse.put("discharge_summary","出院小结");
        medicalCourse.put("death_discuss_record","死亡病例讨论记录");
        medicalCourse.put("death_record","死亡记录");
        medicalCourse.put("death_summary","死亡小结");
        medicalCourse.put("difficulty_case_record","疑难病例讨论记录");
        medicalCourse.put("handover_record","交接班记录");
        medicalCourse.put("rescue_record","抢救记录");
        medicalCourse.put("stage_summary","阶段小结");
        medicalCourse.put("transferred_in_record","转入记录");
        medicalCourse.put("transferred_out_record","转出记录");
        medicalCourse.put("invasive_record","有创诊疗操作记录");
        medicalCourse.put("consultation_opinion_record","会诊意见记录");
        medicalCourse.put("transfusion_record","输血记录");

    }

    static {
        examResult.put("ultrasonic_diagnosis_reports","超声检查");
        examResult.put("xray_image_reports","X线影像诊断");
        examResult.put("ct_reports","CT检查");
        examResult.put("ect_reports","ECT检查");
        examResult.put("mr_reports","MR检查");
        examResult.put("pet_ct_reports","PET_CT检查");
        examResult.put("pet_mr_reports","PET_MR检查");
        examResult.put("microscopic_exam_reports","镜检");
        examResult.put("lung_functional_exam","肺功能检查");
        examResult.put("electrocardiogram_reports","心电图报告");
        examResult.put("other_imaging_exam_diagnosis_reports","其他影像学检查诊断报告");

        examResult.put("ultrasonic_diagnosis_report","超声检查");
        examResult.put("xray_image_report","X线影像诊断");
        examResult.put("lung_functional_exam","肺功能检查");
        examResult.put("electrocardiogram_report","心电图报告");
        examResult.put("imaging_exam_diagnosis_report","影像学检查诊断报告");

    }

    static {
        operation.put("operation_pre_summary","术前小结");
        operation.put("operation_info","手术信息");
        operation.put("operation_records","手术记录");
        operation.put("operation_record","术前讨论记录");
        operation.put("operation_pre_conference_record","手术记录");
        operation.put("forsz_study","放疗检查");
        operation.put("forsz_visit","放疗疗程");
    }

    static {
        sortMap.put("admissions_records","RECORD_DATE");
        sortMap.put("discharge_records","HOSPITAL_DISCHARGE_DATE");
        sortMap.put("first_course_records","RECORD_DATE");
        sortMap.put("attending_physician_rounds_records","RECORD_DATE");
        sortMap.put("post_course_record","RECORD_DATE");
        sortMap.put("admissions_record","RECORD_DATE");
        sortMap.put("discharge_record","RECORD_DATE");
        sortMap.put("first_course_record","RECORD_DATE");
        sortMap.put("attending_physician_rounds_record","RECORD_DATE");
        sortMap.put("course_record","RECORD_DATE");
        sortMap.put("operation_post_course_record","RECORD_DATE");
        sortMap.put("discharge_summary","RECORD_DATE");
        sortMap.put("death_discuss_records","DISCUSS_DATE");
        sortMap.put("death_records","RECORD_DATE");
        sortMap.put("death_summary","RECORD_DATE");
        sortMap.put("difficulty_case_records","DISCUSSED_DATE");
        sortMap.put("handover_record","HANDOVER_DATE");
        sortMap.put("rescue_records","RECORD_DATE");
        sortMap.put("stage_summary","SUMMARY_DATE");
        sortMap.put("transferred_in_records","TRANSFERRED_IN_DATE");
        sortMap.put("transferred_out_records","TRANSFERRED_OUT_DATE");
        sortMap.put("consultation_opinion_records","CONSULTATION_TIME");
        sortMap.put("invasive_records","OPERATION_DATE");
        sortMap.put("death_discuss_record","RECORD_TIME");
        sortMap.put("death_record","RECORD_DATE");
        sortMap.put("difficulty_case_record","RECORD_TIME");
        sortMap.put("rescue_record","RECORD_DATE");
        sortMap.put("transferred_in_record","RECORD_TIME");
        sortMap.put("transferred_out_record","RECORD_TIME");
        sortMap.put("consultation_opinion_record","CONSULTATION_TIME");
        sortMap.put("invasive_record","OPERATION_DATE");
        sortMap.put("ultrasonic_diagnosis_reports","REPORT_DATE");
        sortMap.put("xray_image_reports","REPORT_DATE");
        sortMap.put("ct_reports","REPORT_DATE");
        sortMap.put("ect_reports","REPORT_DATE");
        sortMap.put("mr_reports","REPORT_DATE");
        sortMap.put("pet_ct_reports","REPORT_DATE");
        sortMap.put("pet_mr_reports","REPORT_DATE");
        sortMap.put("microscopic_exam_reports","REPORT_DATE");
        sortMap.put("lung_functional_exa","REPORT_DATE");
        sortMap.put("imaging_exam_diagnosis_report","REPORT_DATE");
        sortMap.put("electrocardiogram_reports","REPORT_DATE");
        sortMap.put("ultrasonic_diagnosis_report","REPORT_DATE");
        sortMap.put("xray_image_report","REPORT_DATE");
        sortMap.put("lung_functional_exam","REPORT_DATE");
        sortMap.put("other_imaging_exam_diagnosis_reports","REPORT_DATE");
        sortMap.put("electrocardiogram_report","REPORT_DATE");
        sortMap.put("operation_pre_summary","RECORD_DATE");
        sortMap.put("operation_info","OPERATION_START_TIME");
        sortMap.put("operation_records","OPERATION_DATE");
        sortMap.put("admission_death_record","RECORD_DATE");
        sortMap.put("admission_discharge_record","RECORD_DATE");
        sortMap.put("transfusion_record","RECORD_TIME");
        sortMap.put("operation_record","OPERATION_DATE");
        sortMap.put("forsz_study","APPLY_TIME");
        sortMap.put("forsz_visit","RADIO_START_DATE");
        sortMap.put("operation_pre_conference_record","RECORD_TIME");
    }

    static {
        examResultName.put("xray_image_reports","INTUBATION_ITEM");
        examResultName.put("ct_reports","EXAMINATION_ITEM");
        examResultName.put("ect_reports","EXAMINATION_ITEM");
        examResultName.put("mr_reports","EXAMINATION_ITEM");
        examResultName.put("pet_ct_reports","EXAMINATION_ITEM");
        examResultName.put("pet_mr_reports","EXAMINATION_ITEM");
        examResultName.put("lung_functional_exam","EXAMINATION_ITEM");
        examResultName.put("other_imaging_exam_diagnosis_reports","EXAMINATION_ITEM");

        examResultName.put("xray_image_report","INTUBATION_ITEM");
        examResultName.put("ct_report","EXAMINATION_ITEM");
        examResultName.put("ect_report","EXAMINATION_ITEM");
        examResultName.put("mr_report","EXAMINATION_ITEM");
        examResultName.put("pet_ct_report","EXAMINATION_ITEM");
        examResultName.put("pet_mr_report","EXAMINATION_ITEM");
//        examResultName.put("electrocardiogram_reports","other");
    }

    public  JsonObject disposeExamResult(JsonObject examObj){
        JsonObject result = new JsonObject();
        List<JsonObject> resultList = new LinkedList<>();
        List<JsonObject> nameList = new ArrayList<>();
        List<String> sortList = new ArrayList<>();
        JsonObject allObj =  examObj.getAsJsonObject("exam_result");
        for (Map.Entry<String,JsonElement> entry : allObj.entrySet()){
            String key = entry.getKey();
            JsonArray value = entry.getValue().getAsJsonArray();
            int titleNum = 0;
            for (JsonElement element : value){
                String titleKey = key+"_"+titleNum;
                titleNum++;
                JsonObject eleObj = element.getAsJsonObject();
                JsonObject nameObj = new JsonObject();
                String examName = null;
                if(eleObj.has("sub_item") && eleObj.get("sub_item") instanceof JsonArray ){
                    JsonArray subArray = eleObj.get("sub_item").getAsJsonArray();
                    JsonArray subData = new JsonArray();
                    for (JsonElement e1 : subArray){
                        JsonObject o1 = e1.getAsJsonObject();
                        o1.addProperty("configSchema",key+"_sub_item");
                        subData.add(o1);
                    }
                    eleObj.remove("sub_item");
                    eleObj.add("subData",subData);
                }
                if(examResultName.containsKey(key)){
                    if(key.equals("other_imaging_exam_diagnosis_reports")){
                        examName = JsonAttrUtil.getStringValue("EXAM_ITEM",eleObj);
                        if(StringUtil.isEmptyStr(examName)){
                            examName = JsonAttrUtil.getStringValue("EXAMINATION_ITEM",eleObj);
                        }
                    }else {
                        examName = JsonAttrUtil.getStringValue(examResultName.get(key),eleObj);
                    }
                }
                if(StringUtil.isEmptyStr(examName)){
                    examName = examResult.get(key);
                }
                nameObj.addProperty("titleName",examName);
                nameObj.addProperty("titleKey",titleKey);
                eleObj.addProperty("configSchema",key);
                eleObj.addProperty("titleName",examName);
                eleObj.addProperty("titleKey",titleKey);
                String time = eleObj.get(sortMap.get(key)).getAsString();
                transforTime(resultList, nameList, sortList, eleObj, nameObj, time,false);
            }
        }
        result.add("data",JsonAttrUtil.toJsonTree(resultList));
        result.add("catalogue",JsonAttrUtil.toJsonTree(nameList));
        return result;
    }

    public  JsonObject disposeMedicalCourse(JsonObject examObj){
        return disposeForOnlyBySort(examObj, medicalCourse);
    }

    public  JsonObject disposeOperator(JsonObject examObj){
        return disposeForOnlyBySort(examObj, operation);
    }

    private  JsonObject disposeForOnlyBySort(JsonObject examObj, Map<String, String> operation) {
        JsonObject result = new JsonObject();
        List<JsonObject> resultList = new LinkedList<>();
        List<JsonObject> nameList = new ArrayList<>();
        List<String> sortList = new ArrayList<>();
        for (Map.Entry<String,JsonElement> entry : examObj.entrySet()){
            String key = entry.getKey();
            JsonArray value = entry.getValue().getAsJsonArray();
            int titleNum = 0;
            transforValuesArray(resultList, nameList, sortList, key, value, titleNum, operation);
        }
        result.add("data", JsonAttrUtil.toJsonTree(resultList));
        result.add("catalogue",JsonAttrUtil.toJsonTree(nameList));

        return result;
    }

    public  JsonObject disposeCourseRecord(JsonObject obj) {
        JsonObject result = new JsonObject();
        List<JsonObject> resultList = new LinkedList<>();
        List<JsonObject> nameList = new ArrayList<>();
        List<String> sortList = new ArrayList<>();
        String admissions_records = "admissions_records";
        String discharge_records = "discharge_records";
        String first_course_record = "first_course_records";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
             admissions_records = "admissions_record";
             discharge_records = "discharge_record";
             first_course_record = "first_course_record";
        }
        //处理入院记录
        for (Map.Entry<String,JsonElement> entry : obj.entrySet()){
            String key = entry.getKey();
            if(admissions_records.equals(key) || discharge_records.equals(key) || first_course_record.equals(key)){
                continue;
            }
            JsonArray value = entry.getValue().getAsJsonArray();
            int titleNum = 0;
            transforValuesArray(resultList, nameList, sortList, key, value, titleNum, courseRecords);
        }
       Integer num1 = addCourseRecordByKey(obj,resultList,nameList,admissions_records,0);
       Integer num2 =  addCourseRecordByKey(obj,resultList,nameList,discharge_records,num1);
        addCourseRecordByKey(obj,resultList,nameList,first_course_record,num2);
        result.add("data",JsonAttrUtil.toJsonTree(resultList));
        result.add("catalogue",JsonAttrUtil.toJsonTree(nameList));

        return result;
    }

    private  void transforValuesArray(List<JsonObject> resultList, List<JsonObject> nameList, List<String> sortList, String key, JsonArray value, int titleNum, Map<String, String> courseRecords) {
        for (JsonElement element : value){
            String titleKey = key+"_"+titleNum;
            titleNum++;
            JsonObject eleObj = element.getAsJsonObject();
            JsonObject nameObj = new JsonObject();
            String examName = null;
            if(StringUtil.isEmptyStr(examName)){
                examName = courseRecords.get(key);
            }
            nameObj.addProperty("titleName",examName);
            nameObj.addProperty("titleKey",titleKey);
            eleObj.addProperty("configSchema",key);
            eleObj.addProperty("titleName",examName);
            eleObj.addProperty("titleKey",titleKey);
            String time = null;
            if(sortMap.containsKey(key) && eleObj.has(sortMap.get(key))){
                time = eleObj.get(sortMap.get(key)).getAsString();
            }
            transforTime(resultList, nameList, sortList, eleObj, nameObj, time, true);
        }
    }

    private  Integer addCourseRecordByKey(JsonObject obj, List<JsonObject> resultList, List<JsonObject> nameList, String key, int num) {
        JsonArray admissionsObj = obj.getAsJsonArray(key);
        int titleNums = 0;
        for (JsonElement element : admissionsObj){
            JsonObject nameObj = new JsonObject();
            String titleKey = key+"_"+titleNums;
            JsonObject eleObj = element.getAsJsonObject();
            String time = JsonAttrUtil.getStringValue(sortMap.get(key),eleObj);
            if(StringUtil.isEmptyStr(time)){
                time = "未知";
            }
            eleObj.addProperty("configSchema",key);
            eleObj.addProperty("titleName",courseRecords.get(key));
            eleObj.addProperty("titleKey",titleKey);
            nameObj.addProperty("titleName",courseRecords.get(key));
            nameObj.addProperty("titleKey",titleKey);
            nameObj.addProperty("time",time);
            resultList.add(num,eleObj);
            nameList.add(num,nameObj);
            titleNums++;
            num++;
        }
        return num;
    }

    private void transforTime(List<JsonObject> resultList, List<JsonObject> nameList, List<String> sortList, JsonObject eleObj, JsonObject nameObj, String time,boolean rank) {
        if(StringUtil.isEmptyStr(time)){
            resultList.add(eleObj);
            nameObj.addProperty("time","未知");
            nameList.add(nameObj);
        }else {
            int num = 0;
            if(rank){
                //正序
                 num = getSortListNum(sortList,time);
            }else {
                //倒序
                 num = getSortListNumDesc(sortList,time);
            }
            if(num == sortList.size() && sortList.size() >= resultList.size() ){
                sortList.add(time);
                resultList.add(eleObj);
                nameObj.addProperty("time",time);
                nameList.add(nameObj);
            }else {
                sortList.add(num,time);
                resultList.add(num,eleObj);
                nameObj.addProperty("time",time);
                nameList.add(num,nameObj);
            }
        }
    }

    public void transforTimeByJsonObject(List<JsonObject> resultList, List<String> sortList, JsonObject eleObj, String time) {
        if(StringUtil.isEmptyStr(time)){
            resultList.add(eleObj);
        }else {
            int num = getSortListNum(sortList,time);
            if(num == sortList.size() && sortList.size() >= resultList.size() ){
                sortList.add(time);
                resultList.add(eleObj);
            }else {
                sortList.add(num,time);
                resultList.add(num,eleObj);
            }
        }
    }

    public int getSortListNum(List<String> sortList, String time) {

        if(sortList.size() == 0){
            return 0;
        }else if(sortList.size() ==1){
            String lastTime = sortList.get(0);
            if (time.compareTo(lastTime) > 0){
                return 1;
            }else {
                return 0;
            }
        }else {
            return binSearch(sortList,0,sortList.size()-1,time);
        }
    }

    public int binSearch(List<String> sortList, int start, int end, String key) {
        int mid = (end - start) / 2 + start;
        if (key.equals(sortList.get(mid))) {
            return mid;
        }
        String midKey = sortList.get(mid);
        if (start >= end) {
            if (key.compareTo(midKey) > 0){
                return end+1;
            }else {
                return end < 0 ? 0 : end;
            }
        } else if (key.compareTo(midKey) > 0){
            return binSearch(sortList, mid + 1, end, key);
        } else if (key.compareTo(midKey) < 0) {
            return binSearch(sortList, start, mid - 1, key);
        }
        return -1;
    }

    public int getSortListNumDesc(List<String> sortList, String time) {

        if(sortList.size() == 0){
            return 0;
        }else if(sortList.size() ==1){
            String lastTime = sortList.get(0);
            if (time.compareTo(lastTime) > 0){
                return 0;
            }else {
                return 1;
            }
        }else {
            return binSearchDesc(sortList,0,sortList.size()-1,time);
        }
    }

    public int binSearchDesc(List<String> sortList, int start, int end, String key) {
        int mid = (end - start) / 2 + start;
        if (key.equals(sortList.get(mid))) {
            return mid;
        }
        String midKey = sortList.get(mid);
        if (start >= end) {
            if (key.compareTo(midKey) < 0){
                return end+1;
            }else {
                return end < 0 ? 0 : end;
            }
        } else if (key.compareTo(midKey) > 0){
            return binSearchDesc(sortList, start, mid - 1, key);
        } else if (key.compareTo(midKey) < 0) {
            return binSearchDesc(sortList, mid + 1, end, key);

        }
        return -1;
    }

}
