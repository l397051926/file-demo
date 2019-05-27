package com.gennlife.fs.common.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

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

    static{
        courseRecords.put("admissions_records","入院记录");
        courseRecords.put("discharge_records","出院记录");
        courseRecords.put("first_course_record","首次病程记录");
        courseRecords.put("attending_physician_rounds_records","上级医师查房记录");
        courseRecords.put("course_record","日常病程记录");
        courseRecords.put("post_course_record","术后病程记录");
    }

    static {
        medicalCourse.put("difficulty_case_records","疑难病例讨论记录");
        medicalCourse.put("stage_summary","阶段小结");
        medicalCourse.put("discharge_summary","出院小结");
        medicalCourse.put("transferred_in_records","转入记录");
        medicalCourse.put("transferred_out_records","转出记录");
        medicalCourse.put("rescue_records","抢救记录");
        medicalCourse.put("death_summary","死亡小结");
        medicalCourse.put("death_records","死亡记录");
        medicalCourse.put("death_discuss_records","死亡病例讨论记录");
        medicalCourse.put("handover_record","交接班记录");
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
        examResult.put("other_imaging_exam_diagnosis_reports","其他影像学检查诊断报告");
        examResult.put("electrocardiogram_reports","心电图报告");
        examResult.put("pathology_reports","病理检测");
    }

    static {
        operation.put("operation_pre_summary","术前小结");
        operation.put("operation_info","手术信息");
        operation.put("operation_records","手术记录");
    }

    /**
     * 入院 记录 - 出院记录 - 首次病程记录
     */
    static {
        sortMap.put("admissions_records","RECORD_DATE");
        sortMap.put("discharge_records","HOSPITAL_DISCHARGE_DATE");
        sortMap.put("first_course_record","RECORD_DATE");
        sortMap.put("attending_physician_rounds_records","RECORD_DATE");
        sortMap.put("course_record","RECORD_DATE");
        sortMap.put("post_course_record","RECORD_DATE");

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

        sortMap.put("ultrasonic_diagnosis_reports","REPORT_DATE");
        sortMap.put("xray_image_reports","REPORT_DATE");
        sortMap.put("ct_reports","REPORT_DATE");
        sortMap.put("ect_reports","REPORT_DATE");
        sortMap.put("mr_reports","REPORT_DATE");
        sortMap.put("pet_ct_reports","REPORT_DATE");
        sortMap.put("pet_mr_reports","REPORT_DATE");
        sortMap.put("microscopic_exam_reports","REPORT_DATE");
        sortMap.put("lung_functional_exam","REPORT_DATE");
        sortMap.put("other_imaging_exam_diagnosis_reports","REPORT_DATE");
        sortMap.put("electrocardiogram_reports","REPORT_DATE");

        sortMap.put("operation_pre_summary","RECORD_DATE");
        sortMap.put("operation_info","OPERATION_START_TIME");
        sortMap.put("operation_records","OPERATION_DATE");
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
//        examResultName.put("electrocardiogram_reports","other");
    }

    public static JsonObject disposeExamResult(JsonObject examObj){
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

    public static JsonObject disposeMedicalCourse(JsonObject examObj){
        return disposeForOnlyBySort(examObj, medicalCourse);
    }

    public static JsonObject disposeOperator(JsonObject examObj){
        return disposeForOnlyBySort(examObj, operation);
    }

    private static JsonObject disposeForOnlyBySort(JsonObject examObj, Map<String, String> operation) {
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

    public static JsonObject disposeCourseRecord(JsonObject obj) {
        JsonObject result = new JsonObject();
        List<JsonObject> resultList = new LinkedList<>();
        List<JsonObject> nameList = new ArrayList<>();
        List<String> sortList = new ArrayList<>();
        //处理入院记录
        for (Map.Entry<String,JsonElement> entry : obj.entrySet()){
            String key = entry.getKey();
            if("admissions_records".equals(key) || "discharge_records".equals(key) || "first_course_record".equals(key)){
                continue;
            }
            JsonArray value = entry.getValue().getAsJsonArray();
            int titleNum = 0;
            transforValuesArray(resultList, nameList, sortList, key, value, titleNum, courseRecords);
        }
       Integer num1 = addCourseRecordByKey(obj,resultList,nameList,"admissions_records",0);
       Integer num2 =  addCourseRecordByKey(obj,resultList,nameList,"discharge_records",num1);
        addCourseRecordByKey(obj,resultList,nameList,"first_course_record",num2);
        result.add("data",JsonAttrUtil.toJsonTree(resultList));
        result.add("catalogue",JsonAttrUtil.toJsonTree(nameList));

        return result;
    }

    private static void transforValuesArray(List<JsonObject> resultList, List<JsonObject> nameList, List<String> sortList, String key, JsonArray value, int titleNum, Map<String, String> courseRecords) {
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

    private static Integer addCourseRecordByKey(JsonObject obj, List<JsonObject> resultList, List<JsonObject> nameList, String key, int num) {
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

    private static void transforTime(List<JsonObject> resultList, List<JsonObject> nameList, List<String> sortList, JsonObject eleObj, JsonObject nameObj, String time,boolean rank) {
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


    public static void transforTimeByJsonObject(List<JsonObject> resultList, List<String> sortList, JsonObject eleObj, String time) {
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

    public static int getSortListNum(List<String> sortList, String time) {

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

    public static int binSearch(List<String> sortList, int start, int end, String key) {
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

    public static int getSortListNumDesc(List<String> sortList, String time) {

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

    public static int binSearchDesc(List<String> sortList, int start, int end, String key) {
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
