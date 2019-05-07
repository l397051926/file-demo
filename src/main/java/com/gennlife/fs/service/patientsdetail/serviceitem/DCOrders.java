package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;

/**
 * Created by Chenjinfeng on 2016/11/3.
 */
public class DCOrders {
    @Deprecated
    public String getDC_orders(String param)
    {
        if(true)return ResponseMsgFactory.buildDeprecatedStr();
        VisitSNResponse visitSNTemplate=new VisitSNResponse("DC_orders");
        return ResponseMsgFactory.getResponseStr(visitSNTemplate,param);
    }
}
