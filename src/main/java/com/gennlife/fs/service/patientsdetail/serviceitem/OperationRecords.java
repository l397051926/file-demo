package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.TimerShaftSort;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonObject;

/**
 * Created by xuhui on 2016/7/26.
 *  分类详情-手术记录
 * 术前小结、手术记录、术后病程
 */
public class OperationRecords {
    public String getOperationRecords (String param){
        VisitSNResponse vt=new VisitSNResponse(
                new String[]{"operation_pre_summary",
                        "operation_records","operation_info"},
                new String[]{"operation_pre_summary","operation_records","operation_info"}
        );
        return ResponseMsgFactory.getResponseStr(vt,param);
       /* vt.execute(JsonAttrUtil.toJsonObject(param));
        if(!StringUtil.isEmptyStr(vt.get_error())) return ResponseMsgFactory.buildFailStr(vt.get_error());
        JsonObject result=vt.get_result();
        JsonArray records=JsonAttrUtil.getJsonArrayValue("operation_records",result);
        JsonArray operation_info=JsonAttrUtil.getJsonArrayValue("operation_info",result);
        if(records!=null&&records.size()>0&&operation_info!=null&&operation_info.size()>0)
        {
            for(int i=0;i<records.size();i++)
            {
                if(operation_info.size()<=i) break;
                JsonObject info=operation_info.get(i).getAsJsonObject();
                JsonObject record=records.get(i).getAsJsonObject();
                record.add("DURATION",info.get("DURATION"));
                record.add("START_TIME",info.get("START_TIME"));
                record.add("END_TIME",info.get("END_TIME"));
            }
        }
        return ResponseMsgFactory.buildSuccessStr(result);*/
    }

    public String getNewOperationRecords(String param) {
        VisitSNResponse vt=new VisitSNResponse(
            new String[]{"operation_pre_summary",
                "operation_records","operation_info"},
            new String[]{"operation_pre_summary","operation_records","operation_info"}
        );
        vt.execute(JsonAttrUtil.toJsonObject( JsonAttrUtil.toJsonObject(param)));
        JsonObject obj = vt.get_result();
        if(obj == null){
            return ResponseMsgFactory.buildFailStr("no data");
        }
        JsonObject data = TimerShaftSort.disposeOperator(obj);
        return ResponseMsgFactory.buildResponseStr(data,vt.get_error());
    }
}