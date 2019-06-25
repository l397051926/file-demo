package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.TimerShaftSort;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonObject;

import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;
import static com.gennlife.fs.configurations.model.Model.emrModel;

/**
 * Created by xuhui on 2016/7/26.
 *  分类详情-手术记录
 * 术前小结、手术记录、术后病程
 */
public class OperationRecords {

    public String getOperationRecords (String param){
        String operation_pre_summary = "operation_pre_summary";
        String operation_record = "operation_records";
        String operation_info = "operation_info";
        String operation_pre_conference_record = "operation_pre_conference_record";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            operation_pre_summary = "operation_pre_summary";
            operation_record = "operation_record";
            operation_info = "operation_info";
            operation_pre_conference_record = "operation_pre_conference_record";
        }
        VisitSNResponse vt=new VisitSNResponse(
            operation_pre_summary,
            operation_record,
            operation_info);
        return ResponseMsgFactory.getResponseStr(vt,param);

    }

    public String getNewOperationRecords(String param) {
        String operation_pre_summary = "operation_pre_summary";
        String operation_record = "operation_records";
        String operation_info = "operation_info";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
             operation_pre_summary = "operation_pre_summary";
             operation_record = "operation_record";
             operation_info = "operation_pre_conference_record";
        }
        VisitSNResponse vt=new VisitSNResponse(
            operation_pre_summary,
            operation_record,
            operation_info
        );
        vt.execute(JsonAttrUtil.toJsonObject( JsonAttrUtil.toJsonObject(param)));
        JsonObject obj = vt.get_result();
        if(obj == null){
            return ResponseMsgFactory.buildFailStr("no data");
        }
        JsonObject data = TimerShaftSort.getInstance().disposeOperator(obj);
        return ResponseMsgFactory.buildResponseStr(data,vt.get_error());
    }
}