package com.gennlife.fs.service.patientsdetail.base;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.patientsdetail.model.Visit;
import com.gennlife.fs.service.patientsdetail.serviceitem.PatientBasicTimeAxis;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.gennlife.fs.system.config.GenDictConfig;
import com.gennlife.fs.system.config.GroupVisitSearch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;


/**
 * Created by zhangshijian on 2016/7/15.
 */
@Service
public class PatientDetailService {
    public static final int IMAGE_TYPE_PATHOLOGY = 1;
    public static final int IMAGE_TYPE_IMAGING_EXAM = 2;
    public static final int IMAGE_TYPE_MICROSCOPIC_EXAM = 3;
    private static final Logger logger = LoggerFactory.getLogger(PatientDetailService.class);
    protected String knowledge_service_uri;
    //patient_sn,data
    public static HashSet<String> gen_dict = GenDictConfig.get_GenDict();

    public PatientDetailService() {
        knowledge_service_uri = BeansContextUtil.getUrlBean().getKnowledge_service_uri();
    }


    /**************************************************************
     * 获取全部病人信息
     *********************************************************/
    protected JsonObject get_data(final QueryParam qp) {
        return HttpRequestUtils.search(qp).getDataDetail();
    }

    /**
     * 单病种数据
     */
    protected JsonObject getCoverMergedata(QueryParam qp) {
        return HttpRequestUtils.search(BeansContextUtil.getUrlBean().getSearch_service_uri(), qp).getDataDetailCoverMerge();
    }


    protected JsonArray get_visits(QueryParam qp) {
        return get_data_array(qp, "visits");
    }
    
    protected JsonArray get_data_array(QueryParam qp, String key) {
        try {
            return get_data(qp).getAsJsonArray(key);
        } catch (NullPointerException e) {

            return null;
        }
    }

    private JsonObject find_visit_sn(String visit_sn, JsonArray visits, String key) {
        JsonObject tmp = null;
        for (JsonElement elem : visits) {
            JsonObject visit = elem.getAsJsonObject();
            if (visit.has(key)) {
                tmp = visit.get(key).getAsJsonArray().get(0).getAsJsonObject();
                if (tmp.has("VISIT_SN") && visit_sn.equals(tmp.get("VISIT_SN").getAsString())) {
                    return visit;
                }
            }
        }
        return null;
    }


    // 分页请求的数据
    public JsonObject generatePagingResult(JsonArray total_array, int current_page, int page_size) {
        int offset = 0;
        if (current_page <= 0) current_page = 1;
        offset = (current_page - 1) * page_size;
        JsonArray sub_array = new JsonArray();
        if (offset < total_array.size()) {
            Iterator<JsonElement> iter = total_array.iterator();
            while (iter.hasNext() && offset > 0) {
                iter.next();
                offset--;
            }
            int record_count = page_size;
            while (iter.hasNext() && record_count > 0) {
                sub_array.add(iter.next());
                record_count--;
            }
        }
        JsonObject result = new JsonObject();
        result.addProperty("code", 1);
        result.add("info", new JsonObject());
        JsonObject info = result.getAsJsonObject("info");
        info.addProperty("count", total_array.size());
        info.addProperty("current_page", current_page);
        info.addProperty("page_size", page_size);
        result.add("data", sub_array);
        return result;
    }

    /**
     * 过滤
     */
    public JsonArray filterPatientVisitsJsonArray(QueryParam qp, JsonObject param_json) {
        JsonArray disease_type = null;
        JsonArray visit_type = null;
        JsonArray department = null;
        JsonArray visit_year = null;
        if (param_json.has("disease_type")) {
            disease_type = param_json.get("disease_type").getAsJsonArray();
        }
        if (param_json.has("visit_type")) {
            visit_type = param_json.get("visit_type").getAsJsonArray();
        }
        if (param_json.has("department")) {
            department = param_json.get("department").getAsJsonArray();
        }
        if (param_json.has("visit_year")) {
            visit_year = param_json.get("visit_year").getAsJsonArray();
        }
        JsonArray event = null;
        if (param_json.has("event")) {
            event = param_json.get("event").getAsJsonArray();
        }
        JsonArray visits = null;
        if ((disease_type != null && disease_type.size() != 0) ||
                (visit_type != null && visit_type.size() != 0) ||
                (department != null && department.size() != 0)
                || (visit_year != null && visit_year.size() != 0)
                || (event != null && event.size() != 0)) {
            qp.addsource(new String[]{
                    "visits.visit_info", "visits.diagnose"
            });
            visits = get_visits(qp);
            if (visits == null) return null;
        } else {
            return get_visits(qp);
        }
        JsonArray result = new JsonArray();
        Type collection_type = new TypeToken<LinkedList<String>>() {
        }.getType();
        LinkedList<String> visit_type_list = null;//(LinkedList<String>) JsonAttrUtil.fromJson(visit_type, collection_type);
        LinkedList<String> visit_year_list = null;//(LinkedList<String>) JsonAttrUtil.fromJson(visit_year, collection_type);
        LinkedList<String> disease_type_list = null;//(LinkedList<String>) JsonAttrUtil.fromJson(disease_type, collection_type);
        LinkedList<String> department_list = null;//(LinkedList<String>) JsonAttrUtil.fromJson(department, collection_type);
        LinkedList<String> event_list = null;//(LinkedList<String>) JsonAttrUtil.fromJson(event, collection_type);
        if (visit_year_list != null && visit_year_list.size() > 0) {
            //未知时间处理
            if (visit_year_list.contains(PatientBasicTimeAxis.EMPTY)) visit_year_list.add(Visit.TIME_EMPTY);
        }
        for (JsonElement elem : visits) {
            JsonObject visit = elem.getAsJsonObject();
            Visit visit_data = new Visit(visit);
            boolean hit_visit_type = false;
            boolean hit_admission_date = false;
            boolean hit_diag_dept = false;
            boolean hit_disease_type = false;
            boolean hit_event_type = false;
            if (null == event_list || event_list.isEmpty()) {
                hit_event_type = true;
            } else {
                for (int i = 0; i < visit_data.get_visit_event().size(); i++) {
                    String value = visit_data.get_visit_event_item(i);
                    if (!"".equals(value) && event_list.contains(value)) {
                        hit_event_type = true;
                        break;
                    }
                }
            }
            if (!hit_event_type) continue;
            if (null == visit_type_list || visit_type_list.isEmpty()
                    || visit_type_list.contains(visit_data.get_visit_type())) {
                hit_visit_type = true;
            }
            if (!hit_visit_type) continue;
            if (null == visit_year_list || visit_year_list.isEmpty()) {
                hit_admission_date = true;
            } else {
                if (visit_year_list.contains(visit_data.get_visit_date_year())) {
                    hit_admission_date = true;
                }
            }
            if (!hit_admission_date) continue;

            if (null == department_list || department_list.isEmpty()
                    || department_list.contains(visit_data.get_visit_dept())) {
                hit_diag_dept = true;
            }
            if (!hit_diag_dept) continue;
            if (null == disease_type_list || disease_type_list.isEmpty()
                    || disease_type_list.contains(visit_data.get_disease())) {
                hit_disease_type = true;
            }
            if (!hit_disease_type) continue;
            result.add(visit);
        }
        logger.debug("isFind size " + result.size());
        return result;
    }


    public JsonArray getJsonArrayValue(JsonObject obj, String key) {
        if (obj.has(key)) {
            return obj.getAsJsonArray(key);
        } else {
            return new JsonArray();
        }
    }

    /***
     * 基因过滤
     **/
    public void genomics_filter(String visit_sn, TreeSet<JsonObject> detection_result, JsonElement elem) {
        JsonObject genom_obj = elem.getAsJsonObject();

        if (visit_sn != null && genom_obj.has("VISIT_SN")
                && !genom_obj.get("VISIT_SN").getAsString().equals(visit_sn)) {
            return;
        }

        if (genom_obj.has("VARIATION_SOURCE")
                && !genom_obj.get("VARIATION_SOURCE").getAsString().equalsIgnoreCase("Somatic")) {
            return;
        }

        if (genom_obj.has("GENE_SYMBOL") && gen_dict.contains(genom_obj.get("GENE_SYMBOL").getAsString())
                && (!genom_obj.has("EXONICFUNC_REFGENE")
                || !genom_obj.get("EXONICFUNC_REFGENE").getAsString().equalsIgnoreCase("synonymous_SNV"))) {

            detection_result.add(genom_obj);
        }
    }

    public JsonObject get_visit(QueryParam qp, String visit_sn, String patient_sn) {
        GroupVisitSearch groupVisitSearch = new GroupVisitSearch(BeansContextUtil.getUrlBean().getVisitIndexName(),patient_sn,visit_sn);
        groupVisitSearch.addSource(qp.getSource());
        String vis = HttpRequestUtils.getSearchEmr(JsonAttrUtil.toJsonStr(groupVisitSearch));
        JsonObject visits = JsonAttrUtil.toJsonObject(vis);//get_visits(newqp);
        if (visits == null) {
            return null;
        }
        return visits;
    }

}
