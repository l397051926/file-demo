package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.comparator.JsonComparatorASCByKey;
import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gennlife.fs.configurations.model.Model.emrModel;

/**
 * Created by mohaowen on 2016/7/19.
 */
public class PatientBasicTimeAxis extends PatientDetailService {

    private static final Logger logger = LoggerFactory.getLogger(PatientBasicTimeAxis.class);
    public static final String EMPTY = "未知";

    public String getVisitTimeline(String param) {
        param = param.replaceAll("2B%","+");
        String patient_sn = null;//from param   病人ID
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null) {
            return ResponseMsgFactory.buildFailStr(" not json");
        }
        boolean isDesc = false;
        if (param_json.has("isDesc")) {
            try {
                isDesc = Boolean.valueOf(JsonAttrUtil.getStringValue("isDesc", param_json));
            } catch (Exception e) {
                return ResponseMsgFactory.buildFailStr("参数 isDesc 不是 bool");
            }
        }
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        QueryParam qp=new QueryParam(param_json);
        qp.query_patient_sn(patient_sn);
        qp.addsource(new String[]{
            "visits.visit_info",
            "visits.diagnose"
        });
        JsonArray diagnose = get_visits(qp);
        Map<String,JsonObject> diagnoseMap =  new HashMap<>();
        for (JsonElement element : diagnose){
            JsonObject object = element.getAsJsonObject();
            transForDiagnoseMap(diagnoseMap,object,"diagnose","visit_info");
        }
        String res = HttpRequestUtils.getStatistics(patient_sn,isDesc);
        JsonElement result = JsonAttrUtil.toJsonElement(res);
        JsonObject resu = (JsonObject) result;
        filter(resu, param_json,diagnoseMap);
        return ResponseMsgFactory.buildSuccessStr(resu);
    }

    private void transForDiagnoseMap(Map<String, JsonObject> diagnoseMap, JsonObject object, String diagnose, String visit_info) {
         transForDiagnoseName(diagnoseMap,object,diagnose);
         transforVisitInfo(diagnoseMap,object,visit_info);
    }


    private void transForDiagnoseName(Map<String, JsonObject> diagnoseMap, JsonObject object, String diagnose) {
        if(object.has(diagnose)){
            JsonArray diagnoseArray = object.get(diagnose).getAsJsonArray();
            List<JsonElement> list = JsonAttrUtil.jsonArrayToList(diagnoseArray);
            String DIAGNOSTIC_DATE = "DIAGNOSTIC_DATE";
            if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
                DIAGNOSTIC_DATE = "DIAGNOSIS_DATE";
            }
            list = JsonAttrUtil.sort(list, new JsonComparatorASCByKey(DIAGNOSTIC_DATE));
            for (JsonElement element : list){
                JsonObject obj = element.getAsJsonObject();
                String isTrue = JsonAttrUtil.getStringValue("MAIN_DIAGNOSIS_FLAG",obj);
                String name = JsonAttrUtil.getStringValue("DIAGNOSIS",obj);
                String visitSn = JsonAttrUtil.getStringValue("VISIT_SN",obj);
                if("是".equals(isTrue)){
                    if(!diagnoseMap.containsKey(visitSn)){
                        diagnoseMap.put(visitSn,new JsonObject());
                    }
                    diagnoseMap.get(visitSn).addProperty("major_diagnostic_names",name);
                    return;
                }
            }
        }
    }
    private void transforVisitInfo(Map<String, JsonObject> diagnoseMap, JsonObject object, String visit_info) {
        if(object.has(visit_info)){
            JsonArray visitArray = object.get(visit_info).getAsJsonArray();
            for (JsonElement element : visitArray){
                JsonObject obj = element.getAsJsonObject();
                String visitSn = JsonAttrUtil.getStringValue("VISIT_SN",obj);
                String admissionDate = JsonAttrUtil.getStringValue("ADMISSION_DATE",obj);
                String dischargeDate = JsonAttrUtil.getStringValue("DISCHARGE_DATE",obj);
                String registred = JsonAttrUtil.getStringValue("REGISTERED_DATE",obj);
                String docterName = JsonAttrUtil.getStringValue("ADMISSION_PHYSICIAN",obj);
                String visitType = JsonAttrUtil.getStringValue("VISIT_TYPE",obj);
                if(!diagnoseMap.containsKey(visitSn)){
                    diagnoseMap.put(visitSn,new JsonObject());
                }
                JsonObject tmpObj = diagnoseMap.get(visitSn);
                if("1".equals(visitType) && StringUtil.isNotEmptyStr(dischargeDate)){
                    tmpObj.addProperty("discharge_time",dischargeDate);
                }else {
                    tmpObj.addProperty("discharge_time","");
                }
                tmpObj.addProperty("attending_doctor",docterName);

            }
        }
    }

    public void filter(JsonObject source, JsonObject param,Map<String,JsonObject> diagnoseMap) {
        JsonArray visits_per_year = JsonAttrUtil.getJsonArrayValue("visits_per_year", source);
        int size = visits_per_year == null ? 0 : visits_per_year.size();
        JsonArray diseaseName = param.has("disease_type") ? param.getAsJsonArray("disease_type") : null;
        JsonArray visitType = param.has("visit_type") ? param.getAsJsonArray("visit_type") : null;
        JsonArray department = param.has("department") ? param.getAsJsonArray("department") : null;
        JsonArray visitYear = param.has("visit_year")? param.getAsJsonArray("visit_year"):null;
        if(diseaseName!=null&&diseaseName.size()>0 || visitType!=null&&visitType.size()>0 || department!=null&&department.size()>0 || visitYear!=null&&visitYear.size()>0){
            JsonArray newPerYear = new JsonArray();
            int total = 0;
            for (int i = 0; i < size; i++) {
                JsonObject asJsonObject = visits_per_year.get(i).getAsJsonObject();
                if (visitYear!=null&&visitYear.size()>0&&!JsonAttrUtil.arrayContain(visitYear, asJsonObject.get("visit_year").getAsString())) {
                    continue;
                }
                JsonArray visits = asJsonObject.getAsJsonArray("visits");
                if (diseaseName!=null&&diseaseName.size()>0 || visitType!=null&&visitType.size()>0 || department!=null&&department.size()>0 ){
                    JsonArray newVisits = new JsonArray();
                    int visitsSize = visits == null ? 0 : visits.size();
                    for (int j = 0; j < visitsSize; j++) {
                        JsonObject value = visits.get(j).getAsJsonObject();
                        //20196-04-24 新详情页增加  主治医生 就诊时间 以及诊断名称功能
                        {
                            String visSn = JsonAttrUtil.getStringValue("visit_sn",value);
                            addMajorAndDischargeTimeAndAttendingDocker(diagnoseMap, value, visSn);
                        }
                        boolean result = filter(diseaseName, value, "diseaseName") && filter(visitType, value, "visitType") && filter(department, value, "department");
                        if (result) {
                            ++total;
                            newVisits.add(value);
                        }
                    }
                    asJsonObject.remove("visits");
                    if(newVisits!=null&&newVisits.size()>0){
                        asJsonObject.add("visits", newVisits);
                        newPerYear.add(asJsonObject);
                    }
                }else{
                    total = total+visits.size();
                    newPerYear.add(asJsonObject);
                }
            }
            source.remove("visits_per_year");
            source.add("total",new JsonPrimitive(total));
            source.add("visits_per_year", newPerYear);
        }else {  //20196-04-24 新详情页增加  主治医生 就诊时间 以及诊断名称功能
            for (JsonElement element : visits_per_year){
                JsonObject object = element.getAsJsonObject();
                if(object.has("visits")){
                    JsonArray datas = object.get("visits").getAsJsonArray();
                    for (JsonElement data : datas){
                        JsonObject dataObj = data.getAsJsonObject();
                        {
                            String visSn = JsonAttrUtil.getStringValue("visit_sn",dataObj);
                            addMajorAndDischargeTimeAndAttendingDocker(diagnoseMap, dataObj, visSn);
                        }
                    }
                }
            }
        }
    }

    private void addMajorAndDischargeTimeAndAttendingDocker(Map<String, JsonObject> diagnoseMap, JsonObject dataObj, String visSn) {
        if(StringUtil.isNotEmptyStr(visSn)){
            JsonObject obj = diagnoseMap.get(visSn);
            dataObj.addProperty("major_diagnostic_names", JsonAttrUtil.getStringValue("major_diagnostic_names",obj));
            dataObj.addProperty("discharge_time", JsonAttrUtil.getStringValue("discharge_time",obj));
            dataObj.addProperty("attending_doctor", JsonAttrUtil.getStringValue("attending_doctor",obj));
        }
    }

    public boolean filter(JsonArray condition, JsonObject value, String type) {
        if(condition == null || condition.size()==0){
            return true;
        }
        JsonElement element = null;
        if ("diseaseName".equals(type)) {
            element = value.get("disease_name");
        } else if ("visitType".equals(type)) {
            element = value.get("visit_type");
        } else if("department".equals(type)){
            element = value.get("visit_dept");
        }
        if(element == null){
            return true;
        }
        return JsonAttrUtil.arrayContain(condition, element.getAsString());
    }
}

