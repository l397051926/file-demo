package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;

/**
 * Created by Xuhui on 2016/7/26.
 * 分类详情-出院记录
 */
public class DischargeRecords {
    public String getDischargeRecords(String param) {
        VisitSNResponse vt=new VisitSNResponse("discharge_records");
        return ResponseMsgFactory.getResponseStr(new SortResponse(vt,"discharge_records", QueryResult.getSortKey("discharge_records"),true),param);
    }

}
