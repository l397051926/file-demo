package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.service.patientsdetail.model.EachVisitResponse;

/**
 * Created by Chenjinfeng on 2018/1/16.
 * 护理记录
 */
public class NurseRecord {
    public String getData(String param) {
        EachVisitResponse vt = new EachVisitResponse(new String[]{
                "operation_nursing_record",
                "general_nursing_record"
        },
                new String[]{
                        "operation_nursing_record",//手术护理记录
                        "general_nursing_record" //普通护理记录
                }
        );

        return ResponseMsgFactory.getResponseStr(vt, param);
    }
}
