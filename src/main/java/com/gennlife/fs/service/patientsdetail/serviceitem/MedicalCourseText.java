package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.TimerShaftSort;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonObject;


/*********************************************
 * 
 * @author helios
 * 
 *  病历文书 ：分类详情、单次就诊详情
 *  包括：出院小结、抢救记录、死亡小结、死亡记录、死亡病例讨论记录
 *  
 *
 *******************************************/

public class MedicalCourseText {

    public String getMedicalCourseText (String param){
        VisitSNResponse vt=new VisitSNResponse(
            new String[]{
                    "discharge_summary",
                    "rescue_records",
                    "death_summary",
                    "death_records",
                    "death_discuss_records"
            },new String[]{
                "discharge_summary",//出院小结
                "rescue_record",//抢救记录
                "death_summary",//死亡小结
                "death_records",//死亡记录
                "death_discuss_records"//死亡病例讨论记录
        }
        );
        return ResponseMsgFactory.getResponseStr(vt,param);
    }

    public String getNewMedicalCourseText(String param) {
        String difficulty_case_records = "difficulty_case_records";
        String stage_summary = "stage_summary";
        String discharge_summary = "discharge_summary";
        String transferred_in_records = "transferred_in_records";
        String transferred_out_records = "transferred_out_records";
        String rescue_records = "rescue_records";
        String death_summary = "death_summary";
        String death_records = "death_records";
        String death_discuss_records = "death_discuss_records";
        String handover_record = "handover_record";
        VisitSNResponse template=new VisitSNResponse(
         new String[]{
             difficulty_case_records,//疑难病例讨论记录
             stage_summary,//阶段小结
             discharge_summary,//出院小结
             transferred_in_records,//转入记录
             transferred_out_records,//转出记录
             rescue_records,//抢救记录
             death_summary,//死亡小结
             death_records,//死亡记录
             death_discuss_records,//死亡病例讨论记录
             handover_record//交接班记录
        }
        );

        template.execute(JsonAttrUtil.toJsonObject( JsonAttrUtil.toJsonObject(param)));
        JsonObject obj = template.get_result();
        if(obj == null){
            return ResponseMsgFactory.buildFailStr("no data");
        }
        JsonObject data = TimerShaftSort.getInstance().disposeMedicalCourse(obj);
       return ResponseMsgFactory.buildResponseStr(data,template.get_error());

    }
}
