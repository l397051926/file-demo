package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;

/**
 * Created by Xuhui on 2016/7/26.
 * 分类详情-入院记录
 */
public class AdmissionRecords {
    public String getAdmissionRecords(String param) {
        VisitSNResponse vt = new VisitSNResponse("admissions_records");
        return ResponseMsgFactory.getResponseStr(new SortResponse(vt, "admissions_records", QueryResult.getSortKey("admissions_records"), true), param);
    }

}
