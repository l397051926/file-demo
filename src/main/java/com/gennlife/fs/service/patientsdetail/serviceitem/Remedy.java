package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;

/**
 * Created by Chenjinfeng on 2018/1/16.
 */
public class Remedy {
    public String getData(String param) {
        String[] s = new String[]{
                "forsz_study",//放疗检查
                "forsz_visit"//放疗疗程
        };
        VisitSNResponse vt = new VisitSNResponse(s, s);
        ResponseInterface sort = new SortResponse(vt, QueryResult.subMap(s), true);
        return ResponseMsgFactory.getResponseStr(sort, param);
    }
}
