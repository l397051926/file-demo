package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;


/*********************************************
 * @author helios
 *  病案首页
 *******************************************/

public class MedicalRecord {
    private static List<String> CONFIG_LIST = new ArrayList<>();
    static {
        CONFIG_LIST.add("dis_main_diag");
        CONFIG_LIST.add("operation");
        CONFIG_LIST.add("dis_main_diag");
    }
    public String getMedicalRecord (String param){
        VisitSNResponse vt=new VisitSNResponse("medical_record_home_page","medical_record");
        return ResponseMsgFactory.getResponseStr(vt,param);
    }

    public String getNewMedicalRecord(String param) {
        VisitSNResponse vt=new VisitSNResponse("medical_record_home_page","medical_record_home_page");
        vt.execute(JsonAttrUtil.toJsonObject(param));
        JsonObject mdicalResult = vt.get_result();
        JsonObject result = transforMdicalResult(mdicalResult,"medical_record_home_page");
        return ResponseMsgFactory.buildResponseStr(result, vt.get_error());
    }

    private JsonObject transforMdicalResult(JsonObject mdicalResult,String key) {
        JsonObject result = new JsonObject();
        JsonArray mdicalArray  = mdicalResult.getAsJsonArray(key);
        for (JsonElement element : mdicalArray){
            JsonObject obj = element.getAsJsonObject();
            for (String config :CONFIG_LIST){
                result.add(config,obj.get(config));
                obj.remove(config);
            }
        }
        result.add(key,mdicalArray);
        return result;
    }
}
