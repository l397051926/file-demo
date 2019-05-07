package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by zhangshijian on 2016/7/18.
 * 分类详情-生物标本
 */
//do
public class SpecimenInfo extends PatientDetailService {
    public String getSpecimenInfo(String param){
        String patient_sn = null;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if(param_json==null) return ResponseMsgFactory.buildFailStr(" not json");
        patient_sn= JsonAttrUtil.getStringValue("filter.patient_sn",param_json);
        if(StringUtil.isEmptyStr(patient_sn))
            return ResponseMsgFactory.buildFailStr("patient_sn empty");
        QueryParam qp=new QueryParam(param_json,patient_sn,"specimen_info.tissue_collection_info");
        JsonObject result = new JsonObject();
        result.add("specimen_info",new JsonArray());
        JsonArray Data = result.getAsJsonArray("specimen_info");
        JsonObject genomic_obj = get_data(qp);

        if(genomic_obj == null) {        		
    		return ResponseMsgFactory.buildFailStr("no data");
    	}
        if(genomic_obj.has("specimen_info")) {
            JsonArray specimen_info = genomic_obj.getAsJsonArray("specimen_info");
            for(JsonElement elem :specimen_info) {
                JsonObject a_specimen_info = elem.getAsJsonObject();
                JsonArray tissue_collection_info = a_specimen_info.getAsJsonArray("tissue_collection_info");
                for(JsonElement tissue_elem : tissue_collection_info) {
                    Data.add(tissue_elem.getAsJsonObject());
                }
            }
        }

        if(Data.size()==0)
            return ResponseMsgFactory.buildFailStr("no data");
        return ResponseMsgFactory.buildSuccessStr(result);
    }
}
