package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;


/*********************************************
 * @author helios
 *  病案首页
 *******************************************/

public class MedicalRecord {
    public String getMedicalRecord (String param){
        VisitSNResponse vt=new VisitSNResponse("medical_record_home_page","medical_record");
        return ResponseMsgFactory.getResponseStr(vt,param);
    }

}
