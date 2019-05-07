package com.gennlife.fs.common.model;

import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.cache.OnePatAccessCache;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.gennlife.fs.system.bean.UrlBean;
import com.gennlife.fs.system.config.DiseasesKeysConfig;
import com.gennlife.fs.system.config.EventStatusUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * Created by Chenjinfeng on 2016/10/14.
 */
public class QueryParam {
    protected String query;
    protected String indexName;
    protected String hospitalID;
    protected int size;
    protected int page;
    protected long time_out = 0;
    protected boolean isIgnore = false;
    protected TreeSet<String> source;
    protected JsonArray roles = null;
    protected JsonArray groups = null;
    private UrlBean urlBean = null;
    protected JsonObject power;
    private boolean searchOnePat = false;
    private String patientSn;
    public static final String ERRORROLES = "<QueryParam没有权限>";
    public static final String ERRORQUERY = "<query is empty>";
    private static Logger logger = LoggerFactory.getLogger(QueryParam.class);
    private String scrollId;
    private static OnePatAccessCache cache = OnePatAccessCache.getInstance();
    private boolean hasError = false;

    public QueryParam(JsonObject json, String patient_sn, String... source) {
        init();
        this.load_param(json);
        this.query_patient_sn(patient_sn);
        this.addsource(source);
    }

    public static boolean checkParamIsError(String searchquery) {
        if (searchquery.contains(ERRORROLES)) return true;
        if (searchquery.contains(ERRORQUERY)) return true;
        return false;
    }

    public QueryParam(JsonObject json) {
        init();
        this.load_param(json);
    }

    public QueryParam(String patient_sn, JsonObject json, String... source) {
        init();
        this.load_param(json);
        this.query_patient_sn(patient_sn);
        this.addsource(source);
    }

    public QueryParam() {
        init();
    }

    public void init() {
        hospitalID = "public";
        page = 1;
        size = 2147483647;//全部病人
        source = new TreeSet();
        scrollId = null;
        try {
            urlBean = BeansContextUtil.getUrlBean();
            if (urlBean != null) {
                indexName = urlBean.getSearchIndexName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void useVisitIndex() {
        if (urlBean != null) {
            indexName = urlBean.getVisitIndexName();
        }
    }

    public void load_param(JsonObject param) {
        if (param == null) return;
        try {
            if (param.has("roles"))
                setRoles(param.getAsJsonArray("roles"));
            if (param.has("groups"))
                setGroups(param.getAsJsonArray("groups"));
            if (param.has("power"))
                this.power = param.getAsJsonObject("power");
        } catch (Exception e) {
        }
    }

    public JsonObject getJson() {
        JsonObject json = new JsonObject();
        json.addProperty("indexName", indexName);
        if (!StringUtil.isEmptyStr(query)) json.addProperty("query", query);
        else {
            json.addProperty("e", ERRORQUERY);
            return json;
        }
        if (!StringUtil.isEmptyStr(scrollId)) {
            json.addProperty("_scroll_id", scrollId);
            if (time_out >= 60000) json.addProperty("_time_out", time_out);
        } else {
            json.addProperty("hospitalID", hospitalID);
            json.addProperty("size", size);
            json.addProperty("page", page);
            json.add("source", JsonAttrUtil.toJsonTree(source));
            if (isIgnore) return json;
            if (roles == null && power == null && groups == null) return json;
            if (searchOnePat) {
                boolean access = cache.hasAccess(patientSn, this);
                if (access) {
                    return json;
                } else {
                    json.addProperty("query", ERRORROLES);
                    return json;
                }
            }

            setRolesInJson(json);
        }

        return json;
    }

    private void setRolesInJson(JsonObject json) {
        json.add("roles", roles);
        if (power != null) json.add("power", power);
        if (groups != null) json.add("groups", groups);
    }

    public QueryParam initFromJson(JsonObject json) {
        if (json == null) return this;
        if (json.has("indexName") && json.get("indexName").isJsonPrimitive())
            this.indexName = json.get("indexName").getAsString();
        if (json.has("query") && json.get("query").isJsonPrimitive()) this.query = json.get("query").getAsString();
        if (json.has("source") && json.get("source").isJsonArray()) {
            this.source = (TreeSet<String>) JsonAttrUtil.fromJson(json.get("source").getAsJsonArray(),
                    new TypeToken<TreeSet<String>>() {
                    }.getType());
        }
        if (json.has("hospitalID")) this.hospitalID = json.get("hospitalID").getAsString();
        if (json.has("size")) setSize(json.get("size").getAsInt());
        if (json.has("page")) this.page = json.get("page").getAsInt();
        if (json.has("roles")) this.setRoles(json.get("roles").getAsJsonArray());
        if (json.has("groups")) this.setGroups(json.get("groups").getAsJsonArray());
        if (json.has("power"))
            this.power = json.getAsJsonObject("power");
        if (this.roles == null || this.roles.size() == 0) this.roles = null;
        setScrollId(JsonAttrUtil.getStringValue("_scroll_id", json));
        try {
            setTime_out(Long.valueOf(JsonAttrUtil.getStringValue("_time_out", json)));
        } catch (Exception e) {
        }
        return this;
    }

    /***
     * @param patient_sn 限制病人数量：  size=patient_sn.split(",").length;
     */
    public QueryParam query_patient_sn(String patient_sn) {
        if (StringUtil.isEmptyStr(patient_sn)) {
            logger.error("patient_sn is empty");
            hasError = true;
            query = null;
        } else {
            size = patient_sn.split(",").length;
            query = "[患者基本信息.患者编号] 包含 " + patient_sn;
            if (size == 1) {
                searchOnePat = true;
                this.patientSn = patient_sn;
            }
        }
        return this;
    }

    public QueryParam query_inpatient_sn(String inpatient_sn) {
        if (StringUtil.isEmptyStr(inpatient_sn)) {
            logger.error("inpatient_sn is empty");
            query = null;
        } else
            query = "[组学信息.基因检测结果.住院号] 包含 " + inpatient_sn;
        return this;
    }

    //Somatic
    public HashMap search_genomics(String inpatient_sns) {
        this.addsource(
                "genomics.detection_result.GENE_SYMBOL",
                "genomics.detection_result.VARTITION_TYPE",
                "genomics.detection_result.CONSEQUENCES_TYPE",
                "genomics.detection_result.EXONICFUNC_REFGENE",
                "genomics.detection_result.INPATIENT_SN",
                "genomics.detection_result.VARIATION_SOURCE"
        );
        this.query_inpatient_sn(inpatient_sns);
        this.roles = new JsonArray();
        JsonArray sources = HttpRequestUtils.search(this).getDatas();
        if (sources != null && sources.size() > 0) {
            HashMap<String, JsonArray> datamap = new HashMap<String, JsonArray>();

            for (JsonElement source : sources) {
                JsonArray items = source.getAsJsonObject().get("_source").getAsJsonObject()
                        .getAsJsonArray("genomics")
                        .get(0).getAsJsonObject()
                        .getAsJsonArray("detection_result");
                LinkedList<JsonElement> data = new LinkedList<JsonElement>();
                for (JsonElement item : items) {
                    try {
                        if (item.getAsJsonObject().get("VARIATION_SOURCE").getAsString().equalsIgnoreCase("Somatic")) {
                            item.getAsJsonObject().remove("VARIATION_SOURCE");
                            data.add(item);
                        }
                    } catch (NullPointerException e) {

                    }
                }
                if (data.size() > 0)
                    datamap.put(data.get(0).getAsJsonObject().get("INPATIENT_SN").getAsString(),
                            JsonAttrUtil.toJsonTree(data).getAsJsonArray());
            }

            return datamap;
        }
        return null;

    }
  /*  protected JsonObject constructRequest(String patient_sn, JsonArray role_obj) {
        JsonObject request_json = new JsonObject();
        String query;
        query = "[患者基本信息.患者编号] 包含 " + patient_sn;

        request_json.addProperty("indexName", searchIndex);
        request_json.addProperty("query", query);
        request_json.addProperty("hospitalID", "public");
        request_json.addProperty("size", 2147483647);
        request_json.addProperty("page", 1);
        request_json.add("roles", role_obj);
        JsonArray source = new JsonArray();
        request_json.add("source", source);

        return request_json;
    }*/

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        if (hasError) return;
        this.query = query;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getHospitalID() {
        return hospitalID;
    }

    public void setHospitalID(String hospitalID) {
        this.hospitalID = hospitalID;
    }

    public int getSize() {
        return size;
    }

    public QueryParam setSize(int size) {
        if (size <= 0) this.size = 1;
        this.size = size;
        return this;
    }

    public QueryParam setSize(double size) {
        this.size = (int) Math.ceil(size);
        return this;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public JsonArray getSource() {
        return JsonAttrUtil.toJsonTree(source).getAsJsonArray();
    }

    public int getSourceLength() {
        if (source == null) return 0;
        return source.size();
    }

    public QueryParam addsource(String... sources) {
        if (sources == null || sources.length == 0) return this;
        if (source == null) source = new TreeSet<>();
        for (String item : sources)
            source.add(item);
        return this;
    }

    public QueryParam addsource(Collection<String> sources) {
        if (sources == null || sources.size() == 0) return this;
        if (source == null) source = new TreeSet<>();
        for (String item : sources)
            source.add(item);
        return this;
    }

    public QueryParam addsource(String source) {
        if (StringUtil.isEmptyStr(source)) return this;
        if (this.source == null) this.source = new TreeSet<>();
        this.source.add(source);
        return this;
    }

    public QueryParam addDiseasesSource(String source) {
        if (StringUtil.isEmptyStr(source)) return this;
        this.source.add(source);
        for (String key : DiseasesKeysConfig.getKeys()) {
            this.source.add(key + "." + source);
        }
        return this;
    }

    public QueryParam addEventSource() {
        addsource(EventStatusUtil.getTimeSource());
        EventStatusUtil.addEventSource(source);
        return this;
    }

    public QueryParam addsource(JsonArray sources) {
        if (sources == null || sources.size() == 0) return this;
        if (source == null) source = new TreeSet<>();
        for (JsonElement item : sources)
            source.add(item.getAsString());
        return this;
    }

    public JsonArray getRoles() {
        return roles;
    }

    public void setRoles(JsonArray roles) {
        this.roles = getJsonArray(roles);
    }

    private JsonArray getJsonArray(JsonArray array) {
        if (array == null) return null;
        else if (array.size() == 0) return null;
        return array;
    }

    public boolean checkSource() {
        if (source == null) return false;
        if (source.size() == 0) return false;
        return true;
    }

    public void ignoreRoles() {
        isIgnore = true;
    }

    public void setGroups(JsonArray groups) {
        this.groups = getJsonArray(groups);
    }

    public boolean findsource(String target) {
        if (this.source == null || StringUtil.isEmptyStr(target)) return false;
        for (String str : this.source) {
            if (str.startsWith(target)) return true;
        }
        return false;
    }

    public boolean findsourceWithEquals(String[] target) {

        if (this.source == null || target == null || target.length == 0) return false;
        for (String str : this.source) {
            for (String key : target)
                if (str.equals(key)) return true;
        }
        return false;
    }

    public void cleanSource() {
        this.source = null;
    }

    public String getUniqueRole() {
        StringBuffer result = new StringBuffer();
        addIfNotEmpty(result, roles);
        addIfNotEmpty(result, groups);
        return result.toString();
    }

    private void addIfNotEmpty(StringBuffer buffer, JsonArray array) {
        if (array != null && array.size() == 0) {
            buffer.append(array.toString());
        }
    }

    public QueryParam changeSourceToDiseasesSource() {
        TreeSet<String> source = new TreeSet<>();
        if (this.source == null || this.source.size() == 0) return this;
        for (String key : DiseasesKeysConfig.getKeys()) {
            for (String item : this.source)
                source.add(key + "." + item);
        }
        source.addAll(this.source);
        this.source = source;
        return this;
    }

    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }

    public void setTime_out(long time_out) {
        this.time_out = time_out;
    }


    public void ignoreSearchOnePat() {
        this.searchOnePat = false;
    }

    public void setSearchOnePat() {
        this.searchOnePat = true;
    }

    public String getRoleInfoStr() {
        JsonObject json = new JsonObject();
        setRolesInJson(json);
        return JsonAttrUtil.toJsonStr(json);
    }
}
