package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;

/**
 * Created by Xuhui on 2016/7/26.
 * 分类详情-出院记录
 */
public class FirstCourseRecord {

    public String getFirstCourseRecord(String param) {
        VisitSNResponse vt = new VisitSNResponse("first_course_records", "first_course");
        return ResponseMsgFactory.getResponseStr(new SortResponse(vt, "first_course", QueryResult.getSortKey("first_course_records"), true), param);
    }

}