package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.searchentity.GenomicsInfo;
import com.gennlife.fs.common.searchentity.GenomicsSNVINDEL;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author
 * @create 2018 09 14:12
 * @desc
 **/
public class GennomicsList {

    private static final Logger logger = LoggerFactory.getLogger(GennomicsList.class);

    public String getGennomicsList(String param) {
        logger.info("param " + param);
        String patientSn = null;
        int size = 0;
        int page = 0;
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        if (paramJson == null) return ResponseMsgFactory.buildFailStr(" not json");
        if (paramJson.has("query")) {
            patientSn = paramJson.get("query").getAsString();
        }
        String uid = paramJson.get("uid").getAsString();
        size = paramJson.get("size").getAsInt();
        page = paramJson.get("page").getAsInt();
        JsonObject query = getQuery(patientSn, size, page, uid);
        String url = BeansContextUtil.getUrlBean().getSearch_service_uri();
//        String url = "http://10.0.2.175:8989/search-server/search";
        String data = HttpRequestUtils.httpPost(url, new Gson().toJson(query));

        JsonObject dataJson = JsonAttrUtil.toJsonObject(data);
        JsonObject value = getValue(dataJson);
        JsonArray visits = null;
        if (value.has("visits")) {
            visits = value.getAsJsonArray("visits");
        }

        if (value.has("genomics")) {
            JsonObject genomics = getJsonObjectByZ(value.getAsJsonArray("genomics"));

            JsonObject snv = null;
            JsonObject info = null;
            JsonArray gennomicsData = null;
            if (genomics.has("snv_indel")) {
                snv = getJsonObjectByZ(genomics.getAsJsonArray("snv_indel"));
                gennomicsData = snv.getAsJsonArray("data");
            }
            if (genomics.has("info")) {
                info = getJsonObjectByZ(genomics.getAsJsonArray("info"));
            }
            //获取结果 值
            JsonObject result = getResult(info, gennomicsData, size, page, visits);
            result.addProperty("success", true);
            return new Gson().toJson(result);
        } else {
            return ResponseMsgFactory.buildFailStr("no data");
        }

    }

    private JsonObject getResult(JsonObject genomicsInfo, JsonArray gennomicsData, int size, int page, JsonArray visits) {
        JsonObject result = new JsonObject();
        JsonObject infoObj = null;
        JsonArray dataArray = null;
        //处理 info数据
        infoObj = getGenInfo(genomicsInfo, visits);
        if (gennomicsData != null) {
            dataArray = getGenDataArray(gennomicsData, size, page);
        }
        int count = gennomicsData == null ? 0 : gennomicsData.size();
        int total = count;
        result.addProperty("limit", page + "," + size);
        result.add("genomics_info", infoObj);
        result.add("genomics_snv_indel", dataArray);
        result.addProperty("total", total);
        return result;
    }

    private JsonArray getGenDataArray(JsonArray gennomicsData, int size, int page) {
        int count = gennomicsData == null ? 0 : gennomicsData.size();
        int total = count % size == 0 ? count / size : count / size + 1;
        int before = (page - 1) * size;
        size = page < total ? size :
                count % size == 0 ? size : count % size;
        JsonArray result = new JsonArray();
        for (int i = before; i < size + before; i++) {
            if (gennomicsData.get(i).isJsonObject()) {
                JsonObject tmpObj = (JsonObject) gennomicsData.get(i);
                if (tmpObj == null) {
                    return result;
                }
                if (tmpObj.has("CHROMOSOMENAME") && tmpObj.has("POS") && tmpObj.has("REFERENCEALLELE") && tmpObj.has("ALTERNATEALLELE")) {
                    tmpObj.addProperty("DNAALter", tmpObj.get("CHROMOSOMENAME").getAsString() + ":" + tmpObj.get("POS").getAsString() +
                            tmpObj.get("REFERENCEALLELE").getAsString() + ">" +
                            tmpObj.get("ALTERNATEALLELE").getAsString());
                    result.add(tmpObj);

                } else {
                    tmpObj.addProperty("DNAALter", "-");
                    result.add(tmpObj);
                }


            }
        }

        return result;
    }

    private JsonObject getGenInfo(JsonObject genomicsInfo, JsonArray visits) {
        JsonObject infoObj = new JsonObject();
        String examina = null;
        String parhology = null;
        String diagnosis = null;
        String patho = null;
        infoObj.addProperty("detectionMethod", "Whole-exome Sequencing");
        infoObj.addProperty("detectingPlatform", "Illumina HiSeq X Ten");
        infoObj.addProperty("inspectionDepartment", "天津肿瘤医院");

        if (genomicsInfo != null) {
            infoObj.addProperty("DETECT_DISEASE", genomicsInfo.get("DETECT_DISEASE").getAsString());
            //处理病例诊断
            if (genomicsInfo.has("PATHOLOGY_NO") && visits != null) {
                String pathologyNo = genomicsInfo.get("PATHOLOGY_NO").getAsString();
                for (JsonElement element : visits) {
                    if (element.isJsonObject()) {
                        JsonObject pathology = (JsonObject) element;
                        JsonArray pathologyarr = pathology.getAsJsonArray("pathology_reports");
                        for (JsonElement tmp : pathologyarr) {
                            if (tmp.isJsonObject()) {
                                /*
                                先根据genomics.info.PATHOLOGY_NO去相应的病理报告中取
                                （与visits.pathology_reports.PARHOLOGY_NUMBER匹配，
                                匹配上之后从该病例检查报告下的
                                visits.pathology_reports.PATHOLOGY_DIAGNOSIS取值），
                                如果没有数据则再从genomics.info.PATHOLOGICAL_DIAGNOSIS中取
                                 */
                                JsonObject tmpObj = tmp.getAsJsonObject();
                                examina = tmpObj.get("EXAMINATION_SN").getAsString();//检查类型
                                parhology = tmpObj.get("PARHOLOGY_NUMBER").getAsString();//检查号
                                if (StringUtil.isNotEmptyStr(examina) && examina.equals(pathologyNo) && tmpObj.has("PATHOLOGY_DIAGNOSIS") && tmpObj.has("CLINICAL_DIAGNOSIS") ) {
                                    diagnosis = tmpObj.get("PATHOLOGY_DIAGNOSIS").getAsString();
                                    patho = tmpObj.get("CLINICAL_DIAGNOSIS").getAsString();
                                    break;
                                } else if (StringUtil.isNotEmptyStr(parhology) && parhology.equals(pathologyNo)  && tmpObj.has("PATHOLOGY_DIAGNOSIS") && tmpObj.has("CLINICAL_DIAGNOSIS")) {
                                    diagnosis = tmpObj.get("PATHOLOGY_DIAGNOSIS").getAsString();
                                    patho = tmpObj.get("CLINICAL_DIAGNOSIS").getAsString();
                                    break;
                                }

                            }
                        }
                    }
                }
                infoObj.addProperty("pathologicDiagnosis", diagnosis);
            } else if (genomicsInfo.has("PATHOLOGICAL_DIAGNOSIS")) {
                infoObj.addProperty("pathologicDiagnosis", genomicsInfo.get("PATHOLOGICAL_DIAGNOSIS").getAsString());
            } else {
                infoObj.addProperty("pathologicDiagnosis", "");
            }
            infoObj.addProperty("CONTROL_SAMPLE_SOURCE", "癌组织" + "+" + genomicsInfo.get("CONTROL_SAMPLE_SOURCE").getAsString());
            if (StringUtil.isEmptyStr(patho)) {
                infoObj.addProperty("PATHOLOGY_NO", "");
            } else {
                infoObj.addProperty("PATHOLOGY_NO", patho);
            }
        }

        return infoObj;
    }

    //组装query
    public JsonObject getQuery(String patientSn, int size, int page, String uid) {
        JsonObject query = new JsonObject();
        //构造条件
        query.addProperty("uid", uid);
//        String indexName = "tianjin_humor_hospital_clinical_patients";
        String indexName = BeansContextUtil.getUrlBean().getVisitIndexName();
        query.addProperty("query", patientSn);
        query.addProperty("indexName", indexName);
        query.addProperty("hospitalID", "public");
        //获取 gernomics
        JsonArray source = new JsonArray();
        source.add(GenomicsSNVINDEL.fieldType);
        source.add(GenomicsInfo.fieldType);
        source.add("visits.pathology_reports");
        query.add("source", source);
        return query;
    }

    //获取 es 指定data
    public JsonObject getValue(JsonObject data) {
        JsonObject hits = data.getAsJsonObject("hits");
        JsonArray hits2 = hits.getAsJsonArray("hits");
        JsonObject source = ((JsonObject) hits2.get(0)).getAsJsonObject("_source");
        return source;
    }

    //获取 json 第0位置
    public JsonObject getJsonObjectByZ(JsonArray array) {
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                return (JsonObject) element;
            }
        }
        return null;
    }

    //获取 json 第0为数组
    public JsonArray getJsonArrayByZ(JsonArray array) {
        for (JsonElement element : array) {
            if (element.isJsonArray()) {
                return (JsonArray) element;
            }
        }
        return null;
    }


}
