package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;

/**
 * Created by Xuhui on 2016/7/26.
 * 分类详情-化疗
 */
public class Chemotherapy {
    public String getChemotherapy(String param){
        VisitSNResponse vt=new VisitSNResponse("chemotherapy");
        return ResponseMsgFactory.getResponseStr(vt,param);
    }

}