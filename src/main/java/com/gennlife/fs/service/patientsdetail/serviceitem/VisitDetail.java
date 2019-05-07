package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.gennlife.fs.system.config.GroupVisitSearch;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Created by xuhui on 2016/7/18.
 * 获取就诊基本信息
 * 输入：patient_sn，visit_sn
 * 输出：visit_info
 */
public class VisitDetail extends PatientDetailService {

    public String getVisitDetail (String param){

        String patient_sn = null;
        String visit_sn = null;
        JsonObject result = new JsonObject();
        JsonArray data = new JsonArray();
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (JsonAttrUtil.has_key(param_json,"patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();            
        }
        else {
        	return ResponseMsgFactory.buildFailStr("missing patient_sn");
        }
        
        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }
        else {
            return ResponseMsgFactory.buildFailStr("missing visit_sn");
        }
        GroupVisitSearch groupVisitSearch = new GroupVisitSearch(BeansContextUtil.getUrlBean().getVisitIndexName(),patient_sn,visit_sn);
        groupVisitSearch.addSource("visits.visit_info");
        String str = HttpRequestUtils.getSearchEmr(JsonAttrUtil.toJsonStr(groupVisitSearch));
        JsonObject visitObj = JsonAttrUtil.toJsonObject(str);
        if(visitObj != null && !visitObj.has("error")){
            JsonObject  data_obj = visitObj.getAsJsonArray("visit_info").get(0).getAsJsonObject();
            if(!data_obj.has("PATIENT_SN")) {
                data_obj.addProperty("PATIENT_SN",patient_sn);
            }
            if(data_obj.has("VISIT_TYPE_NAME")) {
                data_obj.add("VISIT_TYPE",data_obj.get("VISIT_TYPE_NAME"));
            }
            result.add("visit_info", data_obj);
            return ResponseMsgFactory.buildSuccessStr(result);
        }else {
            return ResponseMsgFactory.buildFailStr("no data");
        }
//        String rowKey = patient_sn+"-"+visit_sn;
//        Connection instance = HbaseConnections.instance.getInstance(BeansContextUtil.getZkConfig().getZkUrls(), BeansContextUtil.getZkConfig().getZkPort());
//        HbaseVisistsInfoTableConfigBean hbaseVisistsInfoConfig = BeansContextUtil.getHbaseVisistsInfoConfig();
//        String[] visitsInfo = new String[]{hbaseVisistsInfoConfig.getVisistsInfoVisitInfo()};
//        Result visitsInfoResult = HbaseUtil.getDataByRowKey(instance, hbaseVisistsInfoConfig.getVisistsInfoTableName(), rowKey, hbaseVisistsInfoConfig.getVisistsInfoCFName(), visitsInfo);
//        byte[] visitInfo = visitsInfoResult.getValue(Bytes.toBytes(hbaseVisistsInfoConfig.getVisistsInfoCFName()), Bytes.toBytes(hbaseVisistsInfoConfig.getVisistsInfoVisitInfo()));
//        byte[] uncompress = new byte[]{};
//        try {
//            uncompress = Snappy.uncompress(visitInfo);
//        }catch (Exception e){
//        }
//        if (uncompress != null&&uncompress.length>0) {
//            String str = Bytes.toString(uncompress);
//            JsonArray visitsInfos = (JsonArray)JsonAttrUtil.toJsonElement(str);
//            JsonObject data_obj = new JsonObject();
//            if(visitsInfos!=null&&visitsInfos.size()>0){
//                data_obj = visitsInfos.get(0).getAsJsonObject();
//            }
//            if(!data_obj.has("PATIENT_SN"))
//            {
//                data_obj.addProperty("PATIENT_SN",patient_sn);
//            }
//            if(data_obj.has("VISIT_TYPE_NAME"))
//            {
//                data_obj.add("VISIT_TYPE",data_obj.get("VISIT_TYPE_NAME"));
//            }
//        	result.add("visit_info", data_obj);
//        	return ResponseMsgFactory.buildSuccessStr(result);
//
//        } else {
//            return ResponseMsgFactory.buildFailStr("no data");
//        }


    }
}
