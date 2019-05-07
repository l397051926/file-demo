package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.cache.MergeRequestModelInterface;
import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.gennlife.fs.service.patientsdetail.model.GeneCompator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeSet;

/**
 * Created by zhangshijian on 2016/7/18.
 */
public class MolecularDetection extends PatientDetailService implements MergeRequestModelInterface {
    private static final Logger logger = LoggerFactory.getLogger(MolecularDetection.class);
    private JsonObject param;

    public MolecularDetection(JsonObject param) {
        this.param = param;
    }

    public JsonObject getMolecularDetection(JsonObject param_json) {
        JsonArray genomic_data = null;
        logger.info("getMolecularDetection(): REQUEST molecular_detection \t " + param);
        String patient_sn = null;
        String visit_sn = null;
        if (param_json == null) return ResponseMsgFactory.buildFailJson(" not json");
        JsonObject result = new JsonObject();
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        } else {
            return ResponseMsgFactory.buildFailJson("NO patient_sn");
        }
        QueryParam qp = new QueryParam(param_json);
        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
            if (StringUtil.isEmptyStr(visit_sn))
                return ResponseMsgFactory.buildFailJson("Empty visit_sn");
            qp.query_patient_sn(patient_sn);
            qp.addDiseasesSource("First_surgery.SURGERY_DATE");
            String target = JsonAttrUtil.getStringValue("First_surgery.SURGERY_DATE", getCoverMergedata(qp));
            if (StringUtil.isEmptyStr(target)) return ResponseMsgFactory.buildFailJson("Empty SURGERY_DATE");
            qp.init();
            qp.useVisitIndex();
            qp.setQuery("{[就诊.就诊基本信息.就诊编号] 包含 " + visit_sn
                    + " AND [就诊.手术.手术记录.手术日期] 从 " + target + " 00:00:00 到 " + target + " 23:59:59}"
                    + " AND [患者基本信息.患者编号] 包含 " + patient_sn);

            /*else
            {
                qp.setQuery("[就诊.就诊基本信息.就诊编号] 包含 " + visit_sn
                        + " AND [就诊.手术.手术记录.手术日期] 从 " + target + " 00:00:00 到 " + target + " 23:59:59"
                        + " AND [患者基本信息.患者编号] 包含 " + patient_sn);
            }*/
            qp.setSize(1);
            qp.setPage(1);
        } else {
            qp.query_patient_sn(patient_sn);
        }
        qp.addsource("genomics.detection_result");
        JsonObject searchresult = get_data(qp);
        genomic_data = JsonAttrUtil.getJsonArrayValue("genomics", searchresult);
        JsonArray germline = JsonAttrUtil.getJsonArrayValue("germline", searchresult);
        if (genomic_data == null && germline == null) {
            return ResponseMsgFactory.buildFailJson("NO_RESULT");
        }
        JsonArray gdata = null;
        String key = "";
        if (germline == null) {
            gdata = genomic_data;
            key = "genomic";
        } else {
            gdata = germline;
            key = "germline";
        }
        TreeSet<JsonObject> detection_result = new TreeSet<>(new GeneCompator());
        for (JsonElement gen_elem : gdata) {
            JsonObject gen_obj = gen_elem.getAsJsonObject();

            for (JsonElement elem : getJsonArrayValue(gen_obj, "detection_result")) {
                genomics_filter(visit_sn, detection_result, elem);
            }
        }

        result.add("detection_result", JsonAttrUtil.toJsonTree(detection_result));
        result.addProperty("disease", getPatientDisease(detection_result));
        result.addProperty("detect_method", "二代测序");
        result.addProperty("specimen_source", "新鲜组织");
        result.addProperty("detect_unit", "天津肿瘤医院");
        result.addProperty("detect_platform", "Illumina HiSeq");
        result.addProperty("key", key);
        return ResponseMsgFactory.buildSuccessJson(result);
    }


    /****************************
     * 根据分子检测结果获取疾病名称
     ***************************/
    public String getPatientDisease(TreeSet<JsonObject> detection_result) {
        JsonObject detection = null;
        if (detection_result.size() > 0) {
            detection = detection_result.first();
        } else {
            logger.debug("getPatientDisease(): detection_result size is 0 \t");
            return "none";
        }
        String disease = null;
        if (detection.has("SITE")) {
            String site = detection.get("SITE").getAsString();
            logger.debug("getPatientDisease(): disease \t" + disease);
            if (site.equals("Liver")) {
                disease = "hepatobiliary cancer";
            } else if (site.equals("Lung")) {
                disease = "non-small cell lung cancer";
            } else if (site.equals("Kidney")) {
                disease = "kidney cancer";
            } else {
                disease = "none";
            }
        } else {
            disease = "none";
        }
        logger.debug("getPatientDisease(): disease \t" + disease);

        return disease;
    }

    @Override
    public String getKey() {
        return "getMolecularDetection" + StringUtil.bytesToMD5(param.toString().getBytes());
    }

    @Override
    public JsonObject getValue() {
        return getMolecularDetection(param);
    }
}
