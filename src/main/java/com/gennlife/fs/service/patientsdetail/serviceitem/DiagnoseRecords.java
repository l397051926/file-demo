package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.common.utils.TimerShaftSort;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by xuhui on 2016/7/18.
 * 获取全部诊断记录
 * 输入：patient_sn，visit_sn
 * 输出：diagnosis
 */
public class DiagnoseRecords extends PatientDetailService {
	private static final Logger logger = LoggerFactory.getLogger(DiagnoseRecords.class);
    public String getDiagnoseRecords (String param){

        String patient_sn = null;
        String visit_sn = null;
        JsonObject result = null;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (JsonAttrUtil.has_key(param_json,"patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();            
        }
        else {
        	return ResponseMsgFactory.buildFailStr("no patient_sn");
        }

        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }
        else {
            return ResponseMsgFactory.buildFailStr("no visit_sn");
        }

        QueryParam qp=new QueryParam(param_json);
        qp.addsource("visits.diagnose");
        VisitSNResponse snResponse = new VisitSNResponse("diagnose");
        snResponse.execute(param_json);
        JsonObject visit= snResponse.get_result();//get_visit(qp,visit_sn,patient_sn);

        if (visit != null&&visit.has("diagnose")) {
            JsonArray array=visit.get("diagnose").getAsJsonArray();
           /* for(JsonElement json:array)
            {
                Visit.processNormal(json.getAsJsonObject());
            }*/
            result=new JsonObject();
            result.add("diagnose",array);
        }       
        return ResponseMsgFactory.buildResponseStr(result,"no data");
    }

    public String getNewDiagnoseRecords (String param){

        String patient_sn = null;
        String visit_sn = null;
        JsonObject result = null;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (JsonAttrUtil.has_key(param_json,"patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        else {
            return ResponseMsgFactory.buildFailStr("no patient_sn");
        }

        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }
        else {
            return ResponseMsgFactory.buildFailStr("no visit_sn");
        }

        QueryParam qp=new QueryParam(param_json);
        qp.addsource("visits.diagnose");
        VisitSNResponse snResponse = new VisitSNResponse("diagnose");
        snResponse.execute(param_json);
        JsonObject visit= snResponse.get_result();

        if (visit != null && visit.has("diagnose")) {
            JsonArray array=visit.get("diagnose").getAsJsonArray();
            JsonArray data = diagnoseSort(array);
            result=new JsonObject();
            result.add("diagnose",data);
        }
        return ResponseMsgFactory.buildResponseStr(result,"no data");
    }

    private JsonArray diagnoseSort(JsonArray array) {
        List<List<JsonObject>> result = new ArrayList<>();
        List<String> sortList = new ArrayList<>();
        List<JsonObject> nullList = new LinkedList<>();
        for (JsonElement element : array){
            JsonObject obj = element.getAsJsonObject();
            String date = JsonAttrUtil.getStringValue("DIAGNOSTIC_DATE",obj);
            if(StringUtil.isEmptyStr(date)){
                addMainDiagNosisList(nullList,obj);
                continue;
            }
            if(sortList.contains(date)){
                int size = sortList.indexOf(date);
                List<JsonObject> tmpList = result.get(size);
                addMainDiagNosisList(tmpList,obj);
                continue;
            }else {
                List<JsonObject> tmpList = new ArrayList<>();
                tmpList.add(obj);
                int size = TimerShaftSort.getInstance().getSortListNum(sortList,date);
                sortList.add(size,date);
                result.add(size,tmpList);
            }
        }
        JsonArray resultArray = getResultArray(result);
        resultArray.addAll(JsonAttrUtil.toJsonTree(nullList).getAsJsonArray());
        return resultArray;
    }

    private JsonArray getResultArray(List<List<JsonObject>> result) {
        JsonArray resultArray = new JsonArray();
        for (List<JsonObject> list : result){
            for (JsonObject obj : list){
                resultArray.add(obj);
            }
        }
        return resultArray;
    }

    private void addMainDiagNosisList(List<JsonObject> nullList, JsonObject obj) {
        String isTrue = JsonAttrUtil.getStringValue("MAIN_DIAGNOSIS_FLAG",obj);
        if("true".equals(isTrue)){
            nullList.add(0,obj);
        }else {
            nullList.add(obj);
        }
    }
}