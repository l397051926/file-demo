package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.fs.common.enums.TripleTestPartitionEnum;
import com.gennlife.fs.common.response.*;
import com.gennlife.fs.common.utils.DateUtil;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.service.patientsdetail.dataoperator.TripleTestTableOperator;
import com.gennlife.fs.service.patientsdetail.dataoperator.impl.TripleTestTableSort;
import com.gennlife.fs.service.patientsdetail.dataoperator.interfaces.IDataSortOperate;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;

public class TripleTestTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TripleTestTable.class);

    public String getTripleTestTable(String param) {
        VisitSNResponse visitSN = new VisitSNResponse("triple_test_table");
        SortResponse vt=new SortResponse(visitSN);
        HashMap<String,IDataSortOperate> operateHashMap=new HashMap<>();
        operateHashMap.put("triple_test_table",new TripleTestTableSort());
        vt.setOpeatormap(operateHashMap);
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        if (paramJson == null) return ResponseMsgFactory.buildFailStr("参数不是json");
        vt.execute(JsonAttrUtil.toJsonObject(paramJson));
        JsonObject result = vt.get_result();
        if (result == null) return ResponseMsgFactory.buildFailStr(vt.get_error());
        JsonArray triple_test_tables = result.get("triple_test_table").getAsJsonArray();
        new TripleTestTableOperator().transform(triple_test_tables.iterator());
        ResponseInterface response = new PaginationMemoryResponse(new DirectResponse(result), "triple_test_table");
        return ResponseMsgFactory.getResponseStr(response, paramJson);

    }

    public String getNewsTripleTestTable(String param) throws Exception {
        Integer num = 0;
        VisitSNResponse visitSN = new VisitSNResponse("triple_test_table");
        SortResponse vt=new SortResponse(visitSN);
        HashMap<String,IDataSortOperate> operateHashMap=new HashMap<>();
        operateHashMap.put("triple_test_table",new TripleTestTableSort());
        vt.setOpeatormap(operateHashMap);
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        if (paramJson == null) return ResponseMsgFactory.buildFailStr("参数不是json");
        if(paramJson.has("num")){
            num = paramJson.get("num").getAsInt();
            paramJson.remove("num");
        }
        vt.execute(JsonAttrUtil.toJsonObject(paramJson));
        JsonObject result = vt.get_result();
        if (cfg.patientDetailModelConversionEnabled) {
            KeyPath path = KeyPath.compile("visits[0]");
            JSONObject original = new JSONObject();
            path.assign(original, result);
            JSONObject converted = cfg.patientDetailModelConverter.convert(original);
            JSONObject convertedResult = path.resolveAsJSONObject(converted);
            result = JsonAttrUtil.toJsonObject(convertedResult);
        }
        if (result == null) return ResponseMsgFactory.buildFailStr(vt.get_error());
        JsonArray triple_test_tables = result.get("triple_test_table").getAsJsonArray();
        Map<String,JsonObject> map = new HashMap<>();
        Set<String> visSn = new HashSet<>();
        JsonArray tripleArray = new JsonArray();
        JsonArray hospitalDischargeDate = null;
        JsonArray hospitalAdmissionDate = null;
        JsonArray operationDate = null;
        String patientSn = paramJson.get("patient_sn").getAsString();
        LOGGER.debug("开始处理数据 --- data");
        for (JsonElement element : triple_test_tables){
            try {
                JsonObject obj = element.getAsJsonObject();

                String temprature = JsonAttrUtil.getValByJsonObject(obj,"TEMPRATURE");
                String pulse = JsonAttrUtil.getValByJsonObject(obj,"PULSE");
                String breath = JsonAttrUtil.getValByJsonObject(obj,"BREATH");
                String diastolic = JsonAttrUtil.getValByJsonObject(obj,"DIASTOLIC");
                String systolic = JsonAttrUtil.getValByJsonObject(obj,"SYSTOLIC");
                String heartRate = JsonAttrUtil.getValByJsonObject(obj,"HEART_RATE");
                String visitSn = JsonAttrUtil.getValByJsonObject(obj,"VISIT_SN");
                String bloodPressure = JsonAttrUtil.getValByJsonObject(obj,"BLOOD_PRESSURE");


                if(!visSn.contains(visitSn)){
                    JsonObject query = new JsonObject();
                    //构造条件
                    String indexName = BeansContextUtil.getUrlBean().getVisitIndexName();
                    query.addProperty("patientSn",patientSn );
                    query.addProperty("indexName", indexName);
                    query.addProperty("visitSn", visitSn);
                    //获取 gernomics
                    JsonArray source = new JsonArray();
                    if (cfg.patientDetailModelVersion.compareTo("4") >= 0) {
                        source.add("visits.visit_info.VISIT_SN");
                        source.add("visits.visit_info.ADMISSION_DATE");
                        source.add("visits.visit_info.REGISTERED_DATE");
                        source.add("visits.discharge_record.VISIT_SN");
                        source.add("visits.discharge_record.DISCHARGE_DATE");
                        source.add("visits.operation_record.VISIT_SN");
                        source.add("visits.operation_record.OPERATION_DATE");
                    } else {
                        source.add("visits.visit_info.VISIT_SN");
                        source.add("visits.visit_info.ADMISSION_DATE");
                        source.add("visits.visit_info.REGISTERED_DATE");
                        source.add("visits.discharge_records.VISIT_SN");
                        source.add("visits.discharge_records.HOSPITAL_DISCHARGE_DATE");
                        source.add("visits.operation_records.VISIT_SN");
                        source.add("visits.operation_records.OPERATION_DATE");
                    }
                    query.add("source", source);
                    LOGGER.debug("获取住院时间  出院时间  就诊编号 visitSN"+visitSn);
                    String data = HttpRequestUtils.getSearchEmr(new Gson().toJson(query));
                    LOGGER.debug("获取数据结束  end");
                    JsonObject dataJson = JsonAttrUtil.toJsonObject(data);
                    hospitalDischargeDate = new JsonArray();
                    hospitalAdmissionDate = new JsonArray();
                    operationDate = new JsonArray();
                    addAdmissionDataParam(dataJson, hospitalAdmissionDate,"visit_info");
                    addParamJsonData(dataJson, hospitalDischargeDate,
                        cfg.patientDetailModelVersion.compareTo("4") >= 0 ?
                            "discharge_record" :
                            "discharge_records");
                    addParamJsonData(dataJson, operationDate,
                        cfg.patientDetailModelVersion.compareTo("4") >= 0 ?
                            "operation_record" :
                            "operation_records");
                }
                visSn.add(visitSn);
//            String bloodPressure = "";
                if(StringUtil.isNotEmptyStr(diastolic) || StringUtil.isNotEmptyStr(systolic)){
                    try {
                        Integer sys = Double.valueOf(systolic).intValue();
                        Integer dia = Double.valueOf(diastolic).intValue();
                        bloodPressure = sys + "/" + dia;
                    }catch (NumberFormatException e){
                        bloodPressure = systolic + "/" + diastolic;
                    }
                }

                String examTime = JsonAttrUtil.getValByJsonObject(obj,"EXAM_TIME");
                if(StringUtil.isEmptyStr(examTime)) continue;
                String time = DateUtil.formatDayTime(examTime);
                Integer houer = DateUtil.getHouer(examTime);
                if(map.containsKey(time)){
                    JsonObject tmpObj = map.get(time);
                    JsonAttrUtil.addProperyJsonObject(tmpObj,obj);
                    setTripleTestList(tmpObj.getAsJsonArray("TEMPRATURE"),num,houer,temprature);
                    setTripleTestList(tmpObj.getAsJsonArray("PULSE"),num,houer,pulse);
                    setTripleTestList(tmpObj.getAsJsonArray("BREATH"),num,houer,breath);
                    setTripleTestList(tmpObj.getAsJsonArray("DIASTOLIC"),num,houer,diastolic);
                    setTripleTestList(tmpObj.getAsJsonArray("SYSTOLIC"),num,houer,systolic);
                    setTripleTestList(tmpObj.getAsJsonArray("HEART_RATE"),num,houer,heartRate);
                    setTripleTestList(tmpObj.getAsJsonArray("BLOOD_PRESSURE"),num,houer,bloodPressure);
                    tmpObj.getAsJsonArray("EXAM_TIME_HOUR").add(houer);
//                if(StringUtil.isNotEmptyStr(bloodPressure)){
//                    if(tmpObj.has("BLOOD_PRESSURE")){
//                        setTripleTestList(tmpObj.getAsJsonArray("BLOOD_PRESSURE"),num,houer,bloodPressure);
//                    }else {
//                        tmpObj.add("BLOOD_PRESSURE",setTripleTestList(getTripleTestList(),num,houer,bloodPressure));
//                    }
//                }
                    continue;
                }else {
                    num = houer % 4 ;
                    obj.addProperty("EXAM_TIME",time);
                    //体温
                    obj.add("TEMPRATURE",setTripleTestList(getTripleTestList(),num,houer,temprature));
                    //脉搏
                    obj.add("PULSE",setTripleTestList(getTripleTestList(),num,houer,pulse));
                    //呼吸
                    obj.add("BREATH",setTripleTestList(getTripleTestList(),num,houer,breath));
                    //舒张压
                    obj.add("DIASTOLIC",setTripleTestList(getTripleTestList(),num,houer,diastolic));
                    //收缩压
                    obj.add("SYSTOLIC",setTripleTestList(getTripleTestList(),num,houer,systolic));
                    //心率
                    obj.add("HEART_RATE",setTripleTestList(getTripleTestList(),num,houer,heartRate));
                    //血压
                    obj.add("BLOOD_PRESSURE",setTripleTestList(getTripleTestList(),num,houer,bloodPressure));

                    obj.add("HOSPITAL_DISCHARGE_DATE",hospitalDischargeDate);
                    obj.add("HOSPITAL_ADMISSION_DATE",hospitalAdmissionDate);
                    obj.add("OPERATION_DATE",operationDate);
                    obj.add("EXAM_TIME_HOUR", JsonAttrUtil.getSingleValJsonArrray(houer));
//                if(StringUtil.isNotEmptyStr(bloodPressure)){
//                    obj.add("BLOOD_PRESSURE",setTripleTestList(getTripleTestList(),num,houer,bloodPressure));
//                }
                    map.put(time,obj);
                }
                tripleArray.add(obj);
            }catch (Exception e){
                continue;
            }
        }
        LOGGER.debug("处理完数据");
        if(tripleArray.size()==0){
            return ResponseMsgFactory.buildFailStr("no data");
        }
        result.add("triple_test_table",tripleArray);
        ResponseInterface response = new PaginationMemoryResponse(new DirectResponse(result), "triple_test_table");
        return ResponseMsgFactory.getResponseStr(response, paramJson);
    }

    private void addParamJsonData(JsonObject obj1, JsonArray array, String key) {
        JsonArray tmpArray = obj1.getAsJsonArray(key);
        if(tmpArray == null ){
            return;
        }
        for (JsonElement element :tmpArray){
            JsonObject obj = element.getAsJsonObject();
            if (obj == null ){
                continue;
            }
            for (Map.Entry<String,JsonElement> entry : obj.entrySet()){
                if("VISIT_SN".equals(entry.getKey())) continue;
                array.add(entry.getValue());
            }
        }
    }

    private void addAdmissionDataParam(JsonObject dataJson, JsonArray data, String key) {
        JsonArray tmpArray = dataJson.getAsJsonArray(key);
        if(tmpArray == null) return;
        for (JsonElement element1 : tmpArray){
            JsonObject object = element1.getAsJsonObject();
            String admission = JsonAttrUtil.getValByJsonObject(object,"ADMISSION_DATE");
            if(StringUtil.isNotEmptyStr(admission)){
                data.add(admission);
            }else{
                String registered = JsonAttrUtil.getValByJsonObject(object,"REGISTERED_DATE");
                data.add(registered);
            }
        }
    }

    private void addAdmissionDataParamJsonArray(JsonArray visits, JsonArray hospitalAdmissionDate, String key, String visitSn) {
        if(visits == null ) return;
        for (JsonElement element : visits){
            JsonObject visObj = element.getAsJsonObject();
            JsonArray tmpArray = visObj.getAsJsonArray(key);
            if(tmpArray == null) return;
            for (JsonElement element1 : tmpArray){
                JsonObject object = element1.getAsJsonObject();
                String visSn = JsonAttrUtil.getValByJsonObject(object,"VISIT_SN");
                if(!visitSn.equals(visSn)) continue;
                String admission = JsonAttrUtil.getValByJsonObject(object,"ADMISSION_DATE");
                if(StringUtil.isNotEmptyStr(admission)){
                    hospitalAdmissionDate.add(admission);
                }else{
                    String registered = JsonAttrUtil.getValByJsonObject(object,"REGISTERED_DATE");
                    hospitalAdmissionDate.add(registered);
                }
            }
        }

    }

    private void addParamJsonArray(JsonArray visits, JsonArray array,String key,String visitSn) {
        if(visits == null) return;
        for (JsonElement element1 :visits){
            if(element1 == null) return;
            JsonObject obj1 = element1.getAsJsonObject();
            if(obj1 == null) return;
            JsonArray tmpArray = obj1.getAsJsonArray(key);
            if(tmpArray == null ){
                return;
            }
            for (JsonElement element :tmpArray){
                JsonObject obj = element.getAsJsonObject();
                if (obj == null ){
                    continue;
                }
                String visSn = obj.get("VISIT_SN").getAsString();
                if(!visSn.equals(visitSn)){
                    continue;
                }
                for (Map.Entry<String,JsonElement> entry : obj.entrySet()){
                    if("VISIT_SN".equals(entry.getKey())) continue;
                    array.add(entry.getValue());
                }
            }
        }
    }

    public static boolean isTriple(String key, Set<String> list) {
        return list.contains(key);
    }

    public JsonArray getTripleTestList(){
        JsonArray array = new JsonArray();
        for (int i = 0; i < 6; i++) {
            array.add("");
        }
        return array;
    }
    public JsonArray setTripleTestList(JsonArray array ,int num,int hour,String val){
        int i = TripleTestPartitionEnum.getEnumKey(num,hour).getNum();
        if(StringUtil.isNotEmptyStr(array.get(i).getAsString())){
            return array;
        }
        array.set(i,new Gson().toJsonTree(val));
        return array;
    }

    private GeneralConfiguration cfg = getBean(GeneralConfiguration.class);

}
