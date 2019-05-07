package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;

import java.util.HashMap;

/**
 * Created by Chenjinfeng on 2018/1/16.
 */
public class ClinicMedicalRecords {

    public String getData(String param) {
        VisitSNResponse vt = new VisitSNResponse(new String[]{
                "clinic_medical_records"
        },
                new String[]{
                        "clinic_medical_records"//门诊病历
                }
        );
        HashMap<String, String> map = QueryResult.subMap("clinic_medical_records");
        ResponseInterface sort = new SortResponse(vt, map, true);
        return ResponseMsgFactory.getResponseStr(sort, param);
    }
}
