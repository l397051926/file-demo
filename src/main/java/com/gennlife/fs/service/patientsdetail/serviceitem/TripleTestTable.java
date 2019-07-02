package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.fs.common.enums.TripleTestPartitionEnum;
import com.gennlife.fs.common.response.*;
import com.gennlife.fs.common.utils.DateUtil;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.configurations.model.conversion.ModelConverter;
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

import java.util.*;

import static com.gennlife.fs.configurations.model.Model.emrModel;

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
        if (result == null) return ResponseMsgFactory.buildFailStr(vt.get_error());
        final ModelConverter cvt = emrModel().converter();
        if (cvt != null) {
            KeyPath path = KeyPath.compile("visits[0]");
            JSONObject original = new JSONObject();
            path.assign(original, JSON.parseObject(JsonAttrUtil.toJsonStr(result)));
            JSONObject converted = cvt.convert(original);
            JSONObject convertedResult = path.resolveAsJSONObject(converted);
            result = JsonAttrUtil.toJsonObject(convertedResult);
        }
        JsonArray triple_test_tables = result.get("triple_test_table").getAsJsonArray();
        Map<String,JsonObject> map = new HashMap<>();
        Set<String> visSn = new HashSet<>();
        JsonArray tripleArray = new JsonArray();
        JsonArray hospitalDischargeDate = null;
        JsonArray hospitalAdmissionDate = null;
        JsonArray operationDate = null;
        String patientSn = paramJson.get("patient_sn").getAsString();
        LOGGER.debug("开始处理数据 --- data");
        if(triple_test_tables == null || triple_test_tables.size()<1 ){
            return ResponseMsgFactory.buildFailStr("no data");
        }
        List<JsonElement> tripleList = JsonAttrUtil.jsonArrayToList(triple_test_tables);
        tripleList.sort(Comparator.comparing(o -> JsonAttrUtil.getStringValue("EXAM_TIME", o.getAsJsonObject())));
        String secondTime = null;
        if(tripleList.size()>1){
            for (int i = 1; i < tripleList.size(); i++) {
                secondTime = JsonAttrUtil.getStringValue("EXAM_TIME",triple_test_tables.get(i).getAsJsonObject());
                if(StringUtil.isNotEmptyStr(secondTime)){
                    break;
                }
            }
        }else {
            secondTime = JsonAttrUtil.getStringValue("EXAM_TIME",triple_test_tables.get(0).getAsJsonObject());
        }
        for (JsonElement element : tripleList){
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
                    if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
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
                        emrModel().version().mainVersion().isHigherThanOrEqualTo(4) ?
                            "discharge_record" :
                            "discharge_records");
                    addParamJsonData(dataJson, operationDate,
                        emrModel().version().mainVersion().isHigherThanOrEqualTo(4) ?
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
                Integer targetHouer = DateUtil.getHouer(secondTime);
                if(map.containsKey(time)){
                    JsonObject tmpObj = map.get(time);
                    JsonAttrUtil.addProperyJsonObject(tmpObj,obj);
                    setTripleTestListByJson(tmpObj.getAsJsonArray("TEMPRATURE"),num,houer,temprature,examTime);
                    setTripleTestListByJson(tmpObj.getAsJsonArray("PULSE"),num,houer,pulse,examTime);
                    setTripleTestListByJson(tmpObj.getAsJsonArray("BREATH"),num,houer,breath,examTime);

                    setTripleTestList(tmpObj.getAsJsonArray("DIASTOLIC"),num,houer,diastolic);
                    setTripleTestList(tmpObj.getAsJsonArray("SYSTOLIC"),num,houer,systolic);
                    setTripleTestListByJson(tmpObj.getAsJsonArray("HEART_RATE"),num,houer,heartRate,examTime);
                    setTripleTestList(tmpObj.getAsJsonArray("BLOOD_PRESSURE"),num,houer,bloodPressure);
                    tmpObj.getAsJsonArray("EXAM_TIME_HOUR").add(houer);
                    continue;
                }else {
                    num = targetHouer % 4 ;
                    obj.addProperty("EXAM_TIME",time);
                    //体温
                    obj.add("TEMPRATURE",setTripleTestListByJson(getTripleTestList(),num,houer,temprature,examTime));
                    //脉搏
                    obj.add("PULSE",setTripleTestListByJson(getTripleTestList(),num,houer,pulse,examTime));
                    //呼吸
                    obj.add("BREATH",setTripleTestListByJson(getTripleTestList(),num,houer,breath,examTime));
                    //舒张压
                    obj.add("DIASTOLIC",setTripleTestList(getTripleTestList(),num,houer,diastolic));
                    //收缩压
                    obj.add("SYSTOLIC",setTripleTestList(getTripleTestList(),num,houer,systolic));
                    //心率
                    obj.add("HEART_RATE",setTripleTestListByJson(getTripleTestList(),num,houer,heartRate,examTime));
                    //血压
                    obj.add("BLOOD_PRESSURE",setTripleTestList(getTripleTestList(),num,houer,bloodPressure));

                    obj.add("HOSPITAL_DISCHARGE_DATE",hospitalDischargeDate);
                    obj.add("HOSPITAL_ADMISSION_DATE",hospitalAdmissionDate);
                    obj.add("OPERATION_DATE",operationDate);
                    obj.add("EXAM_TIME_HOUR", JsonAttrUtil.getSingleValJsonArrray(houer));
                    obj.addProperty("targetTime",targetHouer);
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

    private JsonElement setTripleTestListByJson(JsonArray array, Integer num, Integer houer, String val, String examTime) {
        TripleTestPartitionEnum partitionEnum = TripleTestPartitionEnum.getEnumKey(num,houer);
        if(partitionEnum == null ){
            return array;
        }
        int i = partitionEnum.getNum();
        if(StringUtil.isNotEmptyStr(array.get(i).getAsString())){
            return array;
        }
        if(StringUtil.isEmptyStr(val)){
            return array;
        }
        JsonObject data = new JsonObject();
        data.addProperty("time",examTime);
        data.addProperty("value",val);
        array.set(i,data);
        return array;
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
        TripleTestPartitionEnum partitionEnum = TripleTestPartitionEnum.getEnumKey(num,hour);
        if(partitionEnum == null ){
            return array;
        }
        int i = partitionEnum.getNum();
        if(StringUtil.isNotEmptyStr(array.get(i).getAsString())){
            return array;
        }
        array.set(i,new Gson().toJsonTree(val));
        return array;
    }

}
