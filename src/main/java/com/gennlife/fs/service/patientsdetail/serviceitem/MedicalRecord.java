package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/*********************************************
 * @author helios
 *  病案首页
 *******************************************/

public class MedicalRecord {
    private static final String FEE = "fee";
    private static final String OPERATION = "operation";
    private static List<String> CONFIG_LIST = new ArrayList<>();
    private static List<String> CONFIG_SORT_LIST = new ArrayList<>();
    static {
        CONFIG_LIST.add("diag");
        CONFIG_LIST.add("operation");
        CONFIG_LIST.add("fee");

        CONFIG_SORT_LIST.add("总费用");
        CONFIG_SORT_LIST.add("综合医疗服务类");
        CONFIG_SORT_LIST.add("诊断类");
        CONFIG_SORT_LIST.add("治疗类");
        CONFIG_SORT_LIST.add("康复类");
        CONFIG_SORT_LIST.add("中医类");
        CONFIG_SORT_LIST.add("西药类");
        CONFIG_SORT_LIST.add("血液和血液制品类");
        CONFIG_SORT_LIST.add("耗材类");
        CONFIG_SORT_LIST.add("其他类");
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
        result.addProperty("code",1);
        result.addProperty("configSchema","medical_record_home_page");
        return ResponseMsgFactory.buildResponseStr(result, vt.get_error());
    }

    private JsonObject transforMdicalResult(JsonObject mdicalResult,String key) {
        JsonObject result = new JsonObject();
        JsonArray mdicalArray  = mdicalResult.getAsJsonArray(key);
        for (JsonElement element : mdicalArray){
            JsonObject obj = element.getAsJsonObject();
            for (String config :CONFIG_LIST){
                if(FEE.equals(config)){
                    JsonArray array = obj.get(config).getAsJsonArray();
                    List<JsonElement> list = new Gson().fromJson(array,new TypeToken<List<JsonElement>>(){}.getType());
                    list.sort(Comparator.comparingInt(o -> getSortIndex(JsonAttrUtil.getStringValue("ITEM_CLASS",o.getAsJsonObject()))));
                    result.add(config,JsonAttrUtil.toJsonTree(list));
                }else{
                    result.add(config,obj.get(config));
                }
                if(OPERATION.equals(config)){
                    JsonArray array = obj.get(config).getAsJsonArray();
                    List<JsonElement> list = new Gson().fromJson(array,new TypeToken<List<JsonElement>>(){}.getType());
                    list.sort(Comparator.comparing(o -> JsonAttrUtil.getStringValue("OPERATION_DATE",o.getAsJsonObject())));
                    result.add(config,JsonAttrUtil.toJsonTree(list));
                }
                obj.remove(config);
            }
        }
        result.add(key,mdicalArray);
        return result;
    }
    public int getSortIndex(String str){
        if(StringUtil.isEmptyStr(str)){
            CONFIG_SORT_LIST.add(str);
        }
        if(CONFIG_SORT_LIST.indexOf(str) == -1){
            CONFIG_SORT_LIST.add(str);
        }
        return CONFIG_SORT_LIST.indexOf(str);
    }
}
