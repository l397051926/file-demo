package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.gennlife.fs.configurations.model.Model.emrModel;

/**
 * 患者基本信息
 */
public class PatientInfo extends PatientDetailService {
    private Logger logger = LoggerFactory.getLogger(PatientDetailService.class);
    private static String PATIENTINFOPARAM = "{\"UserId\":\"PatientDetailsInfo\", \"Uuids\":[\"PATIENTUUID\"],\"Source\":[\"CountryName\", \"CountryID\", \"Uuid\",\"IDCard\",\"PatiName\",\"Sex\",\"NativePlace\",\"Nationality\",\"BirthDate\",\"IDCardType\",\"OccupationName\",\"RegiLoc\",\"EducationLevel\",\"Married\",\"EntTeleNum\",\"ABO\",\"RH\",\"EntAddr\",\"PatiTelNum\",\"FLTeleNum\",\"BirthLoc\",\"EducationLevel\",\"CTRoleID\"]}";

    public String getPatientInfo(String param) {
        String patient_sn = null;
        String unumber = null;
        boolean isAdmin = false;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null)
            return ResponseMsgFactory.buildFailStr(" not json");
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        } else
            return ResponseMsgFactory.buildFailStr("no patients_sn");

        if (param_json.has("unumber")) {
            unumber = param_json.get("unumber").getAsString();
        } else
            return ResponseMsgFactory.buildFailStr("no unumber");

        if (param_json.has("isAdmin")) {
            isAdmin = param_json.get("isAdmin").getAsBoolean();
        } else
            return ResponseMsgFactory.buildFailStr("no isAdmin");

        JsonAttrUtil.toJsonElement("");

        String quereyParam = PATIENTINFOPARAM.replace("PATIENTUUID", patient_sn);

        JsonElement jsonElement = HttpRequestUtils.httpJsonPost(BeansContextUtil.getUrlBean().getEmpiServiceUri(), quereyParam);
        JsonObject object = new JsonObject();
        if (jsonElement == null || !JsonAttrUtil.has_key((JsonObject) jsonElement, "Results")) {
            return ResponseMsgFactory.buildFailStr("empi 返回数据错误");
        }
        JsonElement element1 = ((JsonObject) jsonElement).get("Results");//JsonAttrUtil.get("Results", (JsonObject)jsonElement);
        if (element1 instanceof JsonArray) {
            JsonArray arrr = (JsonArray) element1;
            if (arrr != null && arrr.size() > 0) {
                JsonObject element = arrr.get(0).getAsJsonObject();
                JsonElement Nationality = element.get("Nationality");
                JsonElement CountryName = element.get("CountryName");
                JsonElement IDCardType = element.get("IDCardType");
                JsonElement RegiLoc = element.get("RegiLoc");
                JsonElement BirthDate = element.get("BirthDate");
                JsonElement Sex = element.get("Sex");
                JsonElement PatiName = element.get("PatiName");
                JsonElement IDCard = element.get("IDCard");
                JsonElement NativePlace = element.get("NativePlace");
                JsonElement OccupationName = element.get("OccupationName");
                JsonElement EducationLevel = element.get("EducationLevel");
                JsonElement Married = element.get("Married");
                JsonElement CTRoleID = element.get("CTRoleID");
                JsonElement RH = element.get("RH");
                JsonElement ABO = element.get("ABO");
                JsonElement EntAddr = element.get("EntAddr");
                JsonElement FLTeleNum = element.get("FLTeleNum");
                JsonElement PatiTelNum = element.get("PatiTelNum");
                object.add("ETHNIC", Nationality);
                object.add("NATIONALITY", CountryName);
                object.add("ID_TYPE", IDCardType);
                object.add("HOME_ADDRSS", RegiLoc);
                object.add("GENDER", Sex);
                try {
                    object.add("BIRTH_DATE", (BirthDate == null || BirthDate.getAsString().length() < 10) ? BirthDate : new JsonPrimitive(BirthDate.getAsString().substring(0, 10)));
                } catch (Exception e) {
                }
                object.add("PATIENT_NAME", PatiName);
                object.add("IDENTITY", IDCard);
                object.add("PATIENT_SN", new JsonPrimitive(patient_sn));
                object.add("NATIVE_PLACE", NativePlace);
                object.add("OCCUPATION", OccupationName);
                object.add("MARITAL_STATUS", Married);
                object.add("EDUCATION_DEGREE", EducationLevel);
                object.add("CONTACT_PHONE", CTRoleID);
                object.add("BLOOD_RH", RH);
                object.add("BLOOD_ABO", ABO);
                object.add("WORK_ADDRSS", EntAddr);
                object.add("HOME_PHONE", FLTeleNum);
                object.add("PHONE_NUM", PatiTelNum);
                try {
                    object.add("BIRTH_DATE_NUM", (BirthDate == null || BirthDate.getAsString().length() < 4) ? BirthDate : new JsonPrimitive(BirthDate.getAsString().substring(0, 4)));
                } catch (Exception e) {
                }
            }
        } else {
            return ResponseMsgFactory.buildFailStr("empi 返回数据格式错误");
        }
        /**获取样本信息 start*/
        QueryParam queryParam = new QueryParam();
        String patientuuid = null;
        if (!isAdmin){
            patientuuid = "(([患者基本信息.患者编号] 包含 " + patient_sn + ") AND [标本信息.标本负责人] 包含 " + unumber + ")";
        } else {
            patientuuid = "(([患者基本信息.患者编号] 包含 " + patient_sn + ") AND [标本信息.标本负责人] BBSYR)";
        }
        queryParam.setQuery(patientuuid);
        queryParam.setSize(1);
        queryParam.setIndexName(BeansContextUtil.getUrlBean().getSearchIndexName());
        queryParam.setHospitalID("public");
        queryParam.addsource("specimen_info");
        logger.info("empi查询条件：" + JsonAttrUtil.toJsonObject(queryParam).toString());
        JsonElement element = HttpRequestUtils.httpJsonPost(BeansContextUtil.getUrlBean().getSearch_service_uri(), JsonAttrUtil.toJsonObject(queryParam).toString());
        JsonArray sampleInfo = new JsonArray();
        JsonObject result = new JsonObject();
        if (element instanceof JsonObject) {
            JsonObject sample = (JsonObject) element;
            getSampleInfo(sample, "specimen_info", sampleInfo);
            result.add("specimen_info", sampleInfo);
        }
        /**获取样本信息 end*/
        result.add("patient_info", object);
        return ResponseMsgFactory.buildSuccessStr(result);
    }

    private boolean getSampleInfo(JsonObject result, String key, JsonArray sampleInfo) {

        JsonObject hits = result.getAsJsonObject("hits");
        boolean falg = false;
        if (hits != null && hits.has("hits")) {
            JsonArray hits1 = hits.getAsJsonArray("hits");
            int size = hits1.size();
            if (hits1 != null && size > 0) {
                falg = true;
                for (int i = 0; i < size; i++) {
                    JsonObject jsonObject = hits1.get(i).getAsJsonObject();
                    JsonObject source = jsonObject.getAsJsonObject("_source");
                    wrapperResult(source, key, sampleInfo);
                }
            }
        }
        return falg;
    }
    private JsonObject getSampleInfo(JsonObject result, String key) {

        JsonObject hits = result.getAsJsonObject("hits");
        boolean falg = false;
        if (hits != null && hits.has("hits")) {
            JsonArray hits1 = hits.getAsJsonArray("hits");
            int size = hits1.size();
            if (hits1 != null && size > 0) {
                falg = true;
                for (int i = 0; i < size; i++) {
                    JsonObject jsonObject = hits1.get(i).getAsJsonObject();
                    JsonObject source = jsonObject.getAsJsonObject("_source");
                    return source.getAsJsonArray(key).get(0).getAsJsonObject();
                }
            }
        }
        return new JsonObject();
    }


    public void wrapperResult(JsonObject source, String key, JsonArray sampleInfo) {

        JsonArray specimen_info = source.getAsJsonArray(key);
        int size = specimen_info == null ? 0 : specimen_info.size();
        if (size == 1) {
            sampleInfo.addAll(specimen_info);
        }
        if (size > 1){
            getSortSampleInfo(sampleInfo, specimen_info);
        }
    }

    private void getSortSampleInfo(JsonArray sampleInfo, JsonArray specimen_info) {
        List<JsonObject> list = new ArrayList();
        for (int i = 0; i < specimen_info.size(); i++){
            list.add(specimen_info.get(i).getAsJsonObject());
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Collections.sort(list, new Comparator<JsonObject>(){
            public int compare(JsonObject o1, JsonObject o2) {
                Date o1_sample_time = null;
                Date o2_sample_time = null;
                try {
                    o1_sample_time = sdf.parse(o1.get("SAMPLING_TIME").getAsString());
                    o2_sample_time = sdf.parse(o2.get("SAMPLING_TIME").getAsString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if(o1_sample_time.getTime() < o2_sample_time.getTime()){
                    return 0;
                }
                if(o1_sample_time.getTime() == o2_sample_time.getTime()){
                    return 1;
                }
                return -1;
            }
        });
        sampleInfo.add(new JsonParser().parse(list.toString()).getAsJsonArray());
    }

    public String getManyPatientInfo (String param){
        JsonArray patient_sn_array = new JsonArray();
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if(param_json==null) return ResponseMsgFactory.buildFailStr(" not json");
        if (param_json.has("patient_sn")) {
            patient_sn_array = param_json.get("patient_sn").getAsJsonArray();
        }
        else
            return ResponseMsgFactory.buildFailStr("no patients_sn");
        JsonAttrUtil.toJsonElement("");

        JsonArray patInfos = new JsonArray();
        for(int i = 0; i < patient_sn_array.size(); i++){
            String quereyParam = PATIENTINFOPARAM.replace("PATIENTUUID", patient_sn_array.get(i).getAsString());
            String url = BeansContextUtil.getUrlBean().getEmpiServiceUri();
            JsonElement jsonElement = HttpRequestUtils.httpJsonPost(url, quereyParam);
            JsonObject object =new JsonObject();
            if(jsonElement==null|| !JsonAttrUtil.has_key((JsonObject) jsonElement,"Results")){
                return ResponseMsgFactory.buildFailStr("empi 返回数据错误");
            }
            JsonElement element1 = ((JsonObject) jsonElement).get("Results");//JsonAttrUtil.get("Results", (JsonObject)jsonElement);

            if(element1 instanceof JsonArray){
                JsonArray arrr = (JsonArray) element1;
                if (arrr!=null&&arrr.size()>0) {
                    JsonObject element = arrr.get(0).getAsJsonObject();
                    JsonElement IDCardType = element.get("IDCardType");
                    JsonElement Sex = element.get("Sex");
                    JsonElement PatiName = element.get("PatiName");
                    JsonElement IDCard = element.get("IDCard");

                    object.add("ID_TYPE",IDCardType);//证件类型
                    object.add("GENDER",Sex);//性别
                    object.add("PATIENT_NAME",PatiName);//病人姓名
                    object.add("IDENTITY",IDCard);//证件号码
                    object.add("PATIENT_SN",new JsonPrimitive(patient_sn_array.get(i).getAsString()));//pat_sn
                }
            }else{
                return ResponseMsgFactory.buildFailStr("empi 返回数据格式错误");
            }
            patInfos.add(object);
        }

        JsonObject result = new JsonObject();
        result.add("patient_info", patInfos);
        return ResponseMsgFactory.buildSuccessStr(result);
    }

    public String getPatientInfoDetail(String param) {
        String patient_sn = null;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (param_json == null)
            return ResponseMsgFactory.buildFailStr(" not json");
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        } else
            return ResponseMsgFactory.buildFailStr("no patients_sn");
        QueryParam queryParam = new QueryParam();
        String patientuuid = null;
        patientuuid = "[患者基本信息.患者编号] 包含 " + patient_sn ;
        queryParam.setQuery(patientuuid);
        queryParam.setSize(1);
        queryParam.setIndexName(BeansContextUtil.getUrlBean().getSearchIndexName());
        queryParam.setHospitalID("public");
        queryParam.addsource("patient_info");
        JsonElement element = HttpRequestUtils.httpJsonPost(BeansContextUtil.getUrlBean().getSearch_service_uri(), JsonAttrUtil.toJsonObject(queryParam).toString());
        JsonArray sampleInfo = new JsonArray();
        JsonObject result = new JsonObject();
        JsonObject object = null;
        if (element instanceof JsonObject) {
            JsonObject sample = (JsonObject) element;
            object =  getSampleInfo(sample, "patient_info");
        }else {
            object = new JsonObject();
        }
        /**获取样本信息 end*/
        result.add("patient_info", object);
        return ResponseMsgFactory.buildSuccessStr(result);
    }
}

