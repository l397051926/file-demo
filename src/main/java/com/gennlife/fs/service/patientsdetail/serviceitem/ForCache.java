package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonObject;

/**
 * Created by Chenjinfeng on 2018/3/12.
 */
public class ForCache {
    public String getData(String param) {
        VisitSNResponse vt = new VisitSNResponse("patient_info");
        vt.setStaticKey(new String[]{"patient_info.PATIENT_SN"});
        ResponseMsgFactory.getResponseStr(vt, param);
        return ResponseMsgFactory.buildSuccessStr(new JsonObject());
    }
}
