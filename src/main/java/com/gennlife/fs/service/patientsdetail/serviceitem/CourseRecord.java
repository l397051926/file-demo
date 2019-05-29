package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.common.utils.TimerShaftSort;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonObject;

import java.util.HashMap;

import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;

/**
 * Created by Xuhui on 2016/7/26.
 * 分类详情-病程记录
 */
public class CourseRecord {
    private GeneralConfiguration cfg = getBean(GeneralConfiguration.class);
    public String getCourseRecord(String param) {
        String[] s1 = new String[]{
                "admissions_records",
                "course_records",
                "first_course_records",
                "discharge_records",
                "attending_physician_rounds_records",
                "operation_post_course_records",

        };
        String[] s2 = new String[]{
                "admissions_records",//入院记录
                "course_record",//日常病程记录
                "first_course_record",//首次病程记录
                "discharge_records",//出院记录
                "attending_physician_rounds_records",//上级医师查房记录
                "post_course_record",//术后病程记录

        };
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        if (paramJson != null) {
            if (!paramJson.has("visit_sn")) {
                s1 = StringUtil.addIntoStringArray(s1, "clinic_medical_records");
                s2 = StringUtil.addIntoStringArray(s2, "clinic_medical_records");
            }
        }
        VisitSNResponse vt = new VisitSNResponse(s1, s2);
        HashMap<String, String> map = QueryResult.subMap(s1, s2);
        ResponseInterface sort = new SortResponse(vt, map, true);
        return ResponseMsgFactory.getResponseStr(sort, paramJson);
    }

    public String getNewCourseRecord(String param) {
        String admissions_record = "admissions_records";
        String course_record = "course_records";
        String first_course_record = "first_course_records";
        String discharge_record = "discharge_records";
        String attending_physician_rounds_record = "attending_physician_rounds_records";
        String operation_post_course_record = "operation_post_course_records";
        if (cfg.patientDetailModelVersion.compareTo("4") >= 0) {
             admissions_record = "admissions_record";
             course_record = "course_record";
             first_course_record = "first_course_record";
             discharge_record = "discharge_record";
             attending_physician_rounds_record = "attending_physician_rounds_record";
             operation_post_course_record = "operation_post_course_record";
        }
        String[] s1 = new String[]{
            admissions_record,
            course_record,
            first_course_record,
            discharge_record,
            attending_physician_rounds_record,
            operation_post_course_record,
        };
        String[] s2 = new String[]{
            admissions_record,//入院记录
            course_record,//日常病程记录
            first_course_record,//首次病程记录
            discharge_record,//出院记录
            attending_physician_rounds_record,//上级医师查房记录
            operation_post_course_record,//术后病程记录
        };
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        VisitSNResponse vt = new VisitSNResponse(s1, s2);
        HashMap<String, String> map = QueryResult.subMap(s1, s2);
        ResponseInterface template = new SortResponse(vt, map, true);
        template.execute(JsonAttrUtil.toJsonObject(paramJson));
        JsonObject obj = template.get_result();
        if(obj == null){
            return ResponseMsgFactory.buildFailStr("no data");
        }
        JsonObject data = TimerShaftSort.getInstance().disposeCourseRecord(obj);
        return ResponseMsgFactory.buildResponseStr(data,template.get_error());
    }


}