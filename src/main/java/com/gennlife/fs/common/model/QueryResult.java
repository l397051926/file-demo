package com.gennlife.fs.common.model;

import com.gennlife.fs.common.utils.DateUtil;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.NumberUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.base.IsMatch;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.gennlife.fs.system.config.DiseasesKeysConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.*;

/**
 * Created by Chenjinfeng on 2016/10/14.
 */
public class QueryResult {
    private long strToJsonTime;
    private JsonObject data = null;
    private int took = READ_TIME_OUT;
    private long total = 0;
    public static final int READ_TIME_OUT = -1;
    public static final int SEARCH_TOOK_ERROR = -2;
    private String mergeKey = null;
    private JsonArray tmpdata;
    public static final JsonArray EMPTYJSONARRAY = new JsonArray();
    private static final Logger logger = LoggerFactory.getLogger(QueryResult.class);
    private String scroll_id;
    private boolean hasError = false;

    public QueryResult(String dataStr) {
        this(JsonAttrUtil.toJsonObject(dataStr));
    }

    public QueryResult(JsonElement dataElem) {
        long s = System.currentTimeMillis();
        try {
            if (dataElem != null && dataElem.isJsonObject()) this.data = dataElem.getAsJsonObject();
            if (data != null) {
                try {
                    took = data.get("took").getAsInt();
                } catch (Exception e) {
                    took = SEARCH_TOOK_ERROR;
                }
                if (data.has("hits")) {
                    total = data.get("hits").getAsJsonObject().get("total").getAsLong();
                }
                if (data.has("_scroll_id")) {
                    scroll_id = JsonAttrUtil.getStringValue("_scroll_id", data);
                }
            } else {
                hasError = true;
                logger.error("search empty");
                return;
            }

        } catch (Exception e) {
            logger.error("search data error ", e);
        }
        long el = System.currentTimeMillis();
        this.strToJsonTime = el - s;
        if (total > 0 && (strToJsonTime > 100)) {
            logger.info("strToJsonTime " + strToJsonTime + " ms");
            logger.info("free memory " + NumberUtil.countSize(Runtime.getRuntime().freeMemory()));
        }
    }

    public boolean isHasError() {
        return hasError;
    }

    public JsonArray getDatas() {
        try {
            JsonArray datas = data.get("hits").getAsJsonObject()
                    .get("hits").getAsJsonArray();
            return datas;

        } catch (NullPointerException e) {
            return EMPTYJSONARRAY;
        } catch (Exception e) {
            logger.error("error ", e);
            return EMPTYJSONARRAY;
        }
    }

    private JsonObject changeStruct(JsonObject json) {

        if (json == null) return json;
        JsonArray visits = JsonAttrUtil.getJsonArrayValue("visits", json);
        if (visits == null) return json;
        visits.forEach(visitItem -> {
            JsonObject visit = visitItem.getAsJsonObject();
            mergeOperatorInfo(visit);
            //changeOperatorName(visit);
            //changefirst_course_record(visit);

        });
        return json;

    }

    private void changefirst_course_record(JsonObject visit) {
        LinkedList<JsonElement> records = JsonAttrUtil.getJsonArrayAllValue("first_course_record", visit);
        if (records != null && records.size() > 1) {
            JsonObject dataItem = null;
            Date time = null;
            for (JsonElement record : records) {
                JsonObject recordJson = record.getAsJsonObject();
                String tmp = JsonAttrUtil.getStringValue("RECORD_DATE", recordJson);
                if (StringUtil.isEmptyStr(tmp)) continue;
                Date tmpDate = DateUtil.getDate(tmp);
                if (tmpDate == null) continue;
                if (time == null || tmpDate.compareTo(time) < 0) {
                    time = tmpDate;
                    dataItem = recordJson;
                }

            }
            JsonArray dataArray = new JsonArray();
            if (dataItem != null) dataArray.add(dataItem);
            visit.add("first_course_record", dataArray);

        }
    }

    private void changeOperatorName(JsonObject visit) {
        LinkedList<JsonElement> records = JsonAttrUtil.getJsonArrayAllValue("operation.records", visit);
        if (records == null || records.size() == 0) return;
        records.forEach(item -> {
            JsonObject itemJson = item.getAsJsonObject();
            if (itemJson.has("OPERATING_NAME")) {
                itemJson.add("OPERATION", itemJson.get("OPERATING_NAME"));
            }
        });
    }

    @Deprecated
    private void mergeOperatorInfo(JsonObject visit) {
        LinkedList<JsonElement> list = JsonAttrUtil.getJsonArrayAllValue("operation.info", visit);
        if (list != null && list.size() > 0) {
            JsonObject operation = JsonAttrUtil.getJsonObjectValue("operation", visit);
            if (!operation.has("records") || !operation.get("records").isJsonArray())
                operation.add("records", new JsonArray());
            JsonArray records = JsonAttrUtil.getJsonArrayValue("operation.records", visit);
            String version = BeansContextUtil.getCompatible().getOperation();
            if ("zhongnan".equalsIgnoreCase(version)) {
                JsonObject result = new JsonObject();
                list.forEach(item -> {
                    for (Map.Entry<String, JsonElement> entryitem : item.getAsJsonObject().entrySet()) {
                        if (result.has(entryitem.getKey())) {
                            if (!result.get(entryitem.getKey()).getAsString().equals(entryitem.getValue().getAsString()))
                                result.addProperty(entryitem.getKey(), (entryitem.getValue() + " + " + result.get(entryitem.getKey())).replace("\"", ""));
                        } else
                            result.add(entryitem.getKey(), entryitem.getValue());
                    }
                });
                if (records.size() == 0) records.add(new JsonObject());
                JsonObject first = records.get(0).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : result.entrySet()) {
                    first.add(entry.getKey(), entry.getValue());
                }
            } else {
                list.forEach(item -> {
                    records.add(item);
                });
            }
        }
    }
/*    public JsonArray getDatasWithOutSource() {
        try {
            LinkedList<JsonElement> result = new LinkedList<>();
            JsonArray array = data.get("hits").getAsJsonObject()
                    .get("hits").getAsJsonArray();
            for (JsonElement elem : array) {
                result.add(JsonAttrUtil.getJsonObjectValue("_source", elem.getAsJsonObject()));
            }
            return JsonAttrUtil.toJsonTree(result).getAsJsonArray();

        } catch (NullPointerException e) {
            logger.error("search data null");
            return EMPTYJSONARRAY;
        } catch (Exception e) {
            logger.error("error ", e);
            return EMPTYJSONARRAY;
        }
    }*/

    public JsonObject getDataDetailCoverMerge() {
        return coverMergeDisease(getDataDetail());
    }

    public JsonObject getDataDetail() {
        JsonObject result_object = null;

        if (data == null) {
            logger.warn("data is null");
            return null;
        }
        if (data.has("hits")) {
            JsonObject hit_obj = data.get("hits").getAsJsonObject();
            if (hit_obj.has("hits")) {
                JsonArray search_result = hit_obj.get("hits").getAsJsonArray();
                if (search_result.size() > 0) {
                    JsonObject patient_obj = search_result.get(0).getAsJsonObject();
                    if (patient_obj.has("_source")) {
                        result_object = patient_obj.get("_source").getAsJsonObject();

                    }
                }
            }
        }
        if (result_object == null) {
            logger.warn("search is no result !");
            return null;
        }
        operator(result_object);
        return result_object;
    }


    private static final Map<String, String> sortMap = new HashMap<>();

    static {
        sortMap.put("operation_nursing_record", "NURSE_TIME");
        sortMap.put("general_nursing_record", "NURSE_TIME");
        sortMap.put("pain_nursing_record", "NURSE_TIME");
        sortMap.put("discharge_records", "HOSPITAL_DISCHARGE_DATE");
        sortMap.put("admissions_records", "HOSPITAL_ADMISSION_DATE");
        sortMap.put("medicine_order", "ORDER_SN");
        sortMap.put("course_records", "RECORD_DATE");
        sortMap.put("attending_physician_rounds_records", "RECORD_DATE");
        sortMap.put("first_course_records", "RECORD_DATE");
        sortMap.put("operation_post_course_records", "RECORD_DATE");
        sortMap.put("triple_test_table", "EXAM_TIME");
        sortMap.put("operation_records", "OPERATION_DATE");
        sortMap.put("operation_pre_summary", "RECORD_DATE");
        sortMap.put("diagnose", "DIAGNOSTIC_DATE");
        sortMap.put("ct_reports", "REPORT_DATE");
        sortMap.put("ect_reports", "REPORT_DATE");
        sortMap.put("xray_image_reports", "REPORT_DATE");
        sortMap.put("mr_reports", "REPORT_DATE");
        sortMap.put("microscopic_exam_reports", "REPORT_DATE");
        sortMap.put("pet_ct_reports", "REPORT_DATE");
        sortMap.put("other_imaging_exam_diagnosis_reports", "REPORT_DATE");
        sortMap.put("pathology_reports", "REPORT_DATE");
        sortMap.put("ultrasonic_diagnosis_reports", "REPORT_DATE");
        sortMap.put("lung_functional_exam", "REPORT_DATE");
        sortMap.put("electrocardiogram_reports", "REPORT_DATE");
        sortMap.put("electrocardiographic_reports", "REPORT_DATE");
        sortMap.put("electroencephalogram_reports", "REPORT_DATE");
        sortMap.put("forsz_visit", "RADIO_START_DATE");
        sortMap.put("bone_marrow_blood_tests_reports", "REPORT_DATE");
        sortMap.put("discharge_summary", "RECORD_DATE");
        sortMap.put("death_records", "RECORD_DATE");
        sortMap.put("death_summary", "RECORD_DATE");
        sortMap.put("rescue_records", "RECORD_DATE");
        sortMap.put("clinic_medical_records", "VISIT_TIME");
        sortMap.put("forsz_study", "SD_APPLY_TIME");
        sortMap.put("inspection_reports","REPORT_TIME");
        sortMap.put("orders","ORDER_START_TIME");
        sortMap.put("non_drug_orders","ORDER_START_TIME");
    }

    public static HashMap<String, String> subMap(String... key) {
        if (key == null || key.length == 0) return null;
        HashMap<String, String> map = new HashMap<>();
        for (String item : key) {
            if (sortMap.containsKey(item)) map.put(item, sortMap.get(item));
        }
        return map;
    }

    public static HashMap<String, String> subMap(String[] s1, String[] s2) {
        if (s1 == null || s1.length == 0) return null;
        if (s2 == null || s2.length == 0) return null;
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < s1.length; i++) {
            if (sortMap.containsKey(s1[i])) map.put(s2[i], sortMap.get(s1[i]));
        }
        return map;
    }

    public static String getSortKey(String key) {
        return sortMap.get(key);
    }

    private static final IsMatch boneSubMatchOp = new IsMatch() {
        @Override
        public boolean isMatch(JsonElement value) {
            if (value == null || !value.isJsonObject()) return false;
            JsonObject json = value.getAsJsonObject();
            String getValue = JsonAttrUtil.getStringValue("CELL_CN", json);
            if (StringUtil.isEmptyStr(getValue)) return false;
            getValue = getValue.replace("（", "(").replace("）", ")").replaceAll("\\s", "");
            boolean isMatch = getValue.equals("计数(个)");
            if (isMatch) json.addProperty("CELL_CN", "计数(个)");
            return isMatch;
        }
    };
    /***
     * 数据清洗 包括数据排序，数据归一化，部分字段回填，用于应对上游的数据混乱
     * */
    public void operator(JsonObject data) {
        if (data == null) return;
        JsonArray visits = JsonAttrUtil.getJsonArrayValue("visits", data);
        if (visits == null || visits.size() == 0) return;
        for (JsonElement visitItem : visits) {
            if (visitItem.isJsonObject()) {
                JsonObject visit = visitItem.getAsJsonObject();
                for (Map.Entry<String, String> item : sortMap.entrySet()) {
                    sort(item.getKey(), item.getValue(), visit);
                }
                if (visit.has("bone_marrow_blood_tests_reports")) {
                    JsonArray array = JsonAttrUtil.getJsonArrayValue("bone_marrow_blood_tests_reports", visit);
                    if (array != null) {
                        array.forEach(item -> {
                            JsonObject itemJson = item.getAsJsonObject();
                            JsonArray sub_test = JsonAttrUtil.getJsonArrayValue("sub_test", itemJson);
                            if (sub_test != null && sub_test.size() > 0) {
                                JsonObject match = searchFirstJsonItem(sub_test, boneSubMatchOp);
                                if (match != null) {
                                    countSubTest(sub_test, match, "BLOOD_SLIDES", "BLOOD_SLIDES", "BLOOD_GRANULOCYTE_RBC_ROTIO");
                                    countSubTest(sub_test, match, "MARROW_SLIDES", "MARROW_SLIDES", "MARROW_GRANULOCYTE_RBC_ROTIO");
                                }
                            }
                        });
                    }
                }
                inspectOperator(visit);

            }
        }

    }

    private static final String[] INSPECT_GROUP = new String[]{
            "ct_reports",
            "ect_reports",
            "mr_reports",
            "pet_ct_reports",
            "pet_mr_reports"
    };

    /**
     * 检查相关的数据处理工作
     */
    private void inspectOperator(JsonObject visit) {
        if (visit == null) return;
        for (String group : INSPECT_GROUP) {
            if (visit.has(group)) {
                JsonArray array = visit.getAsJsonArray(group);
                LinkedHashMap<String, JsonObject> map = new LinkedHashMap<>();
                for (JsonElement element : array) {
                    JsonObject dataJson = element.getAsJsonObject();
                    String imageSn = JsonAttrUtil.getStringValue("IMAGE_SN", dataJson);
                    if (imageSn == null) imageSn = "";
                    if (!map.containsKey(imageSn)) {
                        map.put(imageSn, dataJson);
                        continue;
                    }
                    String examinationItem = JsonAttrUtil.getStringValue("EXAMINATION_ITEM", dataJson);
                    if (StringUtil.isEmptyStr(examinationItem)) continue;
                    JsonObject mergeJson = map.get(imageSn);
                    String mergeExaminationItem = JsonAttrUtil.getStringValue("EXAMINATION_ITEM", mergeJson);
                    mergeExaminationItem = StringUtil.isEmptyStr(mergeExaminationItem) ? examinationItem : mergeExaminationItem + "," + examinationItem;
                    mergeJson.addProperty("EXAMINATION_ITEM", mergeExaminationItem);
                }
                JsonArray result = new JsonArray();
                for (Map.Entry<String, JsonObject> item : map.entrySet()) {
                    result.add(item.getValue());
                }
                visit.add(group,result);
            }
        }
    }

    private void countSubTest(JsonArray sub_test, JsonObject match,
                              String totalKey,
                              String itemValueKey,
                              String itemSetKey) {
        try {
            NumberFormat ddf1 = NumberFormat.getNumberInstance();
            ddf1.setMaximumFractionDigits(4);
            if (match == null || !match.has(totalKey)) return;
            int total = match.get(totalKey).getAsInt();
            if (total > 0) {
                sub_test.forEach(subItem -> {
                    JsonObject subJson = subItem.getAsJsonObject();
                    if (subJson == match) return;
                    String value = JsonAttrUtil.getStringValue(itemValueKey, subJson);
                    if (StringUtil.isEmptyStr(value)) return;
                    String setValue = JsonAttrUtil.getStringValue(itemSetKey, subJson);
                    if (!StringUtil.isEmptyStr(setValue)) return;
                    try {
                        int valueInt = Integer.parseInt(value);
                        float ratio = valueInt * 1.0f / total * 100;
                        subJson.addProperty(itemSetKey, ddf1.format(ratio));
                    } catch (Exception e) {

                    }
                });
            }

        } catch (Exception e) {

        }
    }

    public static JsonObject searchFirstJsonItem(JsonArray array, IsMatch isMatch) {
        if (array == null || array.size() == 0) return null;
        if (isMatch == null) throw new NullPointerException();
        for (JsonElement element : array) {
            JsonObject data = element.getAsJsonObject();
            if (isMatch.isMatch(data)) return data;
        }
        return null;
    }

    public static void sort(String group, String key, JsonObject data) {
        if (data == null || !data.has(group)) return;
        JsonArray array = JsonAttrUtil.getJsonArrayValue(group, data);
        if (array == null) return;
        if (array.size() == 1) return;
        LinkedList<JsonElement> empty = new LinkedList<>();
        LinkedList<SortItem> hasValue = new LinkedList<>();
        LinkedList<JsonElement> result = new LinkedList<>();
        for (JsonElement element : array) {
            String sortKey = JsonAttrUtil.getStringValue(key, element.getAsJsonObject());
            if (StringUtil.isEmptyStr(sortKey)) {
                empty.add(element);
            } else
                hasValue.add(new SortItem(sortKey, element));
        }
        Collections.sort(hasValue, new Comparator<SortItem>() {
            @Override
            public int compare(SortItem o1, SortItem o2) {
                return o1.key.compareTo(o2.key);
            }
        });
        for (SortItem item : hasValue) {
            result.add(item.element);
        }
        result.addAll(empty);
        data.add(group, JsonAttrUtil.toJsonTree(result));

    }

    static class SortItem {
        private String key;
        private JsonElement element;

        public SortItem(String key, JsonElement element) {
            this.key = key;
            this.element = element;
        }
    }

    public int getTook() {
        return took;
    }

    public long getTotal() {
        return total;
    }

    public void setTmpdata(JsonArray tmpdata) {
        this.tmpdata = tmpdata;
    }

    /**
     * 单病种覆盖型
     */
    public JsonObject coverMergeDisease(JsonObject json) {
        if (json == null) return json;
        for (String key : DiseasesKeysConfig.getKeys()) {
            if (JsonAttrUtil.isNotEmptyKey(json, key)) {
                JsonArray array = json.get(key).getAsJsonArray();
                if (array.isJsonNull() || array.size() == 0) continue;
                JsonObject tmp = array.get(0).getAsJsonObject();
                for (Map.Entry<String, JsonElement> item : tmp.entrySet()) {
                    json.add(item.getKey(), item.getValue());
                }
                mergeKey = key;
                break;
            }
        }
        return json;
    }

    public String getMergeKey() {
        if (StringUtil.isEmptyStr(mergeKey))
            return "DEFAULT";
        else
            return mergeKey;
    }

    public long getStrToJsonTime() {
        return strToJsonTime;
    }

    public String getScroll_id() {
        return scroll_id;
    }

    public JsonObject createSearchResult() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("took", took);
        jsonObject.addProperty("total", total);
        JsonObject hits = new JsonObject();
        jsonObject.add("hits", hits);
        hits.add("hits", this.tmpdata);
        if (!StringUtil.isEmptyStr(scroll_id)) {
            jsonObject.addProperty("_scroll_id", scroll_id);
        }
        return jsonObject;
    }

}
