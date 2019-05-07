package com.gennlife.fs.common.response;

import com.gennlife.fs.common.comparator.JsonComparatorASCByKey;
import com.gennlife.fs.common.comparator.JsonComparatorDESCByKey;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.dataoperator.interfaces.IDataSortOperate;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Chenjinfeng on 2016/11/3.
 * 基于内存的分页
 */
public class SortResponse implements ResponseInterface {
    private final ResponseInterface vt;
    private String error;
    private JsonObject result;
    private Map<String, IDataSortOperate> opeatormap;
    private Map<String, String> map = null;
    private boolean isAsc = true;

    public SortResponse(ResponseInterface vt, String key, String sortKey, boolean isAsc) {
        this.vt = vt;
        map = new HashMap<>();
        map.put(key, sortKey);
        this.isAsc = isAsc;
    }

    public SortResponse(ResponseInterface vt) {
        this.vt = vt;
    }

    public void setOpeatormap(Map<String, IDataSortOperate> opeatormap) {
        this.opeatormap = opeatormap;
    }

    public SortResponse(ResponseInterface vt, Map<String, String> map, boolean isAsc) {
        this.vt = vt;
        this.map = map;
        this.isAsc = isAsc;
    }

    @Override
    public String get_error() {
        return error;
    }

    @Override
    public JsonObject get_result() {
        return result;
    }

    @Override
    public void setResult(JsonObject result) {
        this.result = result;
    }

    @Override
    public void setError(String error) {
        this.error = error;
    }

    @Override
    public void execute(JsonObject param) {
        vt.execute(param);
        JsonObject result = vt.get_result();
        if (result == null) {
            error = vt.get_error();
            return;
        }
        this.result = new JsonObject();
        List<String> keys = new LinkedList<>();
        if (map != null) {
            for (String key : map.keySet()) {
                if (!result.has(key)) continue;
                keys.add(key);
                JsonArray array = result.get(key).getAsJsonArray();
                String sortkey = map.get(key);
                if (StringUtil.isEmptyStr(sortkey)) {
                    this.result.add(key, array);
                } else {
                    List<JsonElement> list = JsonAttrUtil.jsonArrayToList(array);
                    if (isAsc) list = JsonAttrUtil.sort(list, new JsonComparatorASCByKey(sortkey));
                    else list = JsonAttrUtil.sort(list, new JsonComparatorDESCByKey(sortkey));
                    this.result.add(key, JsonAttrUtil.toJsonTree(list));
                }
            }
        }
        if (opeatormap != null) {
            for (String key : opeatormap.keySet()) {
                if (!result.has(key)) continue;
                keys.add(key);
                JsonArray array = result.get(key).getAsJsonArray();
                IDataSortOperate operate = opeatormap.get(key);
                this.result.add(key, JsonAttrUtil.toJsonTree(operate.sort(array)));
            }
        }
        if (keys.size() != 0) {
            removeEmpty(keys.toArray(new String[keys.size()]));
        }


    }
}
