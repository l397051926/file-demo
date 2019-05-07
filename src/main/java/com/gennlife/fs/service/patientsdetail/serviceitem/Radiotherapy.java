package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonObject;

/**
 * Created by Xuhui on 2016/7/26.
 * 放疗
 * 无放疗模块则显示放疗医嘱
 * {
 "radiotherapy_orders":[],
 "radiotherapy":[],
 "success":true|false
 }
 */
@Deprecated
public class Radiotherapy extends PatientDetailService {
    public String getRadiotherapy(String param){
        if(true) return ResponseMsgFactory.buildDeprecatedStr();
        VisitSNResponse vt=new VisitSNResponse("radiotherapy");
        JsonObject result= ResponseMsgFactory.getResponseJson(vt,param);
        vt=null;
        if(ResponseMsgFactory.isSuccess(result)&&result.getAsJsonArray("radiotherapy").size()>0)
        {
            return result.toString();
        }
        else
        {
            vt=new VisitSNResponse("radiotherapy_orders");
            return ResponseMsgFactory.getResponseStr(vt,param);
        }
    }

}
