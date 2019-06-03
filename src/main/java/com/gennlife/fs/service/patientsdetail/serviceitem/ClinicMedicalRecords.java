package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;

import java.util.HashMap;

import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;
import static com.gennlife.fs.configurations.model.Model.emrModel;

/**
 * Created by Chenjinfeng on 2018/1/16.
 */
public class ClinicMedicalRecords {

    public String getData(String param) {
        String clinic_medical_record = "clinic_medical_records";
        if(emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            clinic_medical_record = "clinic_medical_record";
        }
        VisitSNResponse vt = new VisitSNResponse(new String[]{
            clinic_medical_record
        },
                new String[]{
                    clinic_medical_record//门诊病历
                }
        );
        HashMap<String, String> map = QueryResult.subMap(clinic_medical_record);
        ResponseInterface sort = new SortResponse(vt, map, true);
        return ResponseMsgFactory.getResponseStr(sort, param);
    }
}
