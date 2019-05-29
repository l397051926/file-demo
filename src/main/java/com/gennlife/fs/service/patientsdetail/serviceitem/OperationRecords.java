package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.TimerShaftSort;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonObject;

import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;

/**
 * Created by xuhui on 2016/7/26.
 *  分类详情-手术记录
 * 术前小结、手术记录、术后病程
 */
public class OperationRecords {
    private GeneralConfiguration cfg = getBean(GeneralConfiguration.class);

    public String getOperationRecords (String param){
        String operation_pre_summary = "operation_pre_summary";
        String operation_record = "operation_records";
        String operation_info = "operation_info";
        if (cfg.patientDetailModelVersion.compareTo("4") >= 0) {
            operation_pre_summary = "operation_pre_summary";
            operation_record = "operation_record";
            operation_info = "operation_info";
        }
        VisitSNResponse vt=new VisitSNResponse(
            new String[]{
                operation_pre_summary,
                operation_record,
                operation_info
            }
        );
        return ResponseMsgFactory.getResponseStr(vt,param);

    }

    public String getNewOperationRecords(String param) {
        String operation_pre_summary = "operation_pre_summary";
        String operation_record = "operation_records";
        String operation_info = "operation_info";
        if (cfg.patientDetailModelVersion.compareTo("4") >= 0) {
             operation_pre_summary = "operation_pre_summary";
             operation_record = "operation_record";
             operation_info = "operation_info";
        }
        VisitSNResponse vt=new VisitSNResponse(
            new String[]{
                operation_pre_summary,
                operation_record,
                operation_info
            }
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