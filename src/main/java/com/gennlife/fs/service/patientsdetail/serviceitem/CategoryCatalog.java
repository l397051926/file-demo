package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.MapUtility;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.gennlife.fs.service.patientsdetail.model.DiagnosisSub;
import com.gennlife.fs.service.patientsdetail.model.Visit;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by zhangshijian on 2016/7/19.
 * 分类详情-诊断结果
 * 只取主要诊断
 */
public class CategoryCatalog extends PatientDetailService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryCatalog.class);
    public String getCategoryCatalog(String param){

        logger.debug("getCategoryCatalog(): REQUEST category_catalog...");
        logger.debug("getCategoryCatalog(): param\t" + param);
        String patient_sn = null;

        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if (JsonAttrUtil.has_key(param_json,"patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }

        QueryParam qp=new QueryParam(param_json);
        qp.query_patient_sn(patient_sn);
        qp.addsource(new String[]{
               "visits.visit_info",
                "visits.diagnose"
        });

        JsonArray visits = filterPatientVisitsJsonArray(qp, param_json);
        if(visits == null) {
            return ResponseMsgFactory.buildFailStr("no visits");
        }
        Map<String, Long> map = new HashMap();
        



        for (JsonElement elem : visits) {
        	Visit vst = new Visit(elem.getAsJsonObject());
        	DiagnosisSub diagsub= new DiagnosisSub(vst);

        	MapUtility.increaseMapKeyCount(map, JsonAttrUtil.toJsonStr(diagsub));
        }

        JsonArray array = new JsonArray();
        for (Entry<String, Long> entry: map.entrySet()) {
            String diasub_json = entry.getKey();
            JsonObject diasub_obj = JsonAttrUtil.toJsonObject(diasub_json);
            diasub_obj.addProperty("visit_count", entry.getValue());
            array.add(diasub_obj);
        }

        JsonObject result = new JsonObject();
        result.add("diagnosis", array);
        return ResponseMsgFactory.buildSuccessStr(result);
    }
}
