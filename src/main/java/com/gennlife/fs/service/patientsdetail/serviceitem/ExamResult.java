package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.ImageResponseUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.TimerShaftSort;
import com.google.gson.JsonObject;

/**
 * Created by zhangshijian on 2016/7/19.
 * 分类详情-检验检查-检查
 */
public class ExamResult {

    public String getExamResult(String param) {
        String[] keys = new String[]{
                "ultrasonic_diagnosis_reports",//超声检查
                "xray_image_reports",//X线影像诊断
                "ct_reports",//ct检查
                "ect_reports",//ect检查
                "mr_reports",//mr检查
                "pet_ct_reports",//pet_ct检查
                "pet_mr_reports",//pet_mr检查
                "microscopic_exam_reports",//镜像检查
                "lung_functional_exam",//肺功能检查
        };
        ResponseInterface template = ImageResponseUtils.getImageResponseInterface(keys);
        template.execute(JsonAttrUtil.toJsonObject(param));
        JsonObject result = template.get_result();
        JsonObject json = null;
        if (result != null) {
            json = new JsonObject();
            json.add("exam_result", result);
        }
        return ResponseMsgFactory.buildResponseStr(json, template.get_error());
    }
    //bone_marrow_blood_tests_reports 骨髓瘤数据
    public String getNewExamResult(String param) {
        String[] keys = new String[]{
            "ultrasonic_diagnosis_reports",//超声检查
            "xray_image_reports",//X线影像诊断
            "ct_reports",//ct检查
            "ect_reports",//ect检查
            "mr_reports",//mr检查
            "pet_ct_reports",//pet_ct检查
            "pet_mr_reports",//pet_mr检查
            "microscopic_exam_reports",//镜像检查
            "lung_functional_exam",//肺功能检查
            "other_imaging_exam_diagnosis_reports",
            "electrocardiogram_reports",
            "other_imaging_exam_diagnosis_reports",
            "electrocardiogram_reports"
        };
        ResponseInterface template = ImageResponseUtils.getImageResponseInterface(keys);
        template.execute(JsonAttrUtil.toJsonObject(param));
        JsonObject result = template.get_result();
        JsonObject json = null;
        if (result != null) {
            json = new JsonObject();
            json.add("exam_result", result);
        }else {
            return ResponseMsgFactory.buildFailStr("no data");
        }
        JsonObject data = TimerShaftSort.disposeExamResult(json);
        return ResponseMsgFactory.buildResponseStr(data, template.get_error());
    }

}

