package com.gennlife.fs.common.response;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Created by Chenjinfeng on 2016/11/3.
 * 基于内存的分页
 */
public class PaginationMemoryResponse implements ResponseInterface {
    private final ResponseInterface vt;
    private String error;
    private JsonObject result;
    private String key;
    public static final String PAGE_SIZE = "page_size";
    public static final String CURRENT_PAGE = "currentPage";
    public static final String TOTAL_PAGE = "totalPage";

    public PaginationMemoryResponse(ResponseInterface vt, String key) {
        this.vt = vt;
        this.key = key;
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
        if (StringUtil.isEmptyStr(key)) {
            error = "System ERROR : key is empty";
        }
        if (!check(param, new String[]{PAGE_SIZE, CURRENT_PAGE})) return;
        int page_size = 0;
        int currentPage = 0;
        try {
            page_size = param.get(PAGE_SIZE).getAsInt();
            currentPage = param.get(CURRENT_PAGE).getAsInt();
            if (page_size <= 0) {
                error = "invalid page_size";
                return;
            }
            if (currentPage <= 0) {
                error = "invalid currentPage";
                return;
            }
        } catch (Exception e) {
            error = " page_size or currentPage must be number";
            return;
        }
        vt.execute(param);
        result = vt.get_result();
        if (result == null) {
            error = vt.get_error();
            return;
        }
        JsonArray array = JsonAttrUtil.getJsonArrayValue(key, result);
        result = getPaginationResult(key, page_size, currentPage, array);
        if (result == null) {
            error = "no data";
            result = null;
            return;
        }
        removeEmpty(key);
    }

    public static JsonObject getPaginationResult(String key, int page_size, int currentPage, JsonArray array) {
        if (array == null || array.size() == 0) return null;
        List<JsonElement> list = JsonAttrUtil.getPagingResult(JsonAttrUtil.jsonArrayToList(array), page_size, currentPage);
        JsonObject result = new JsonObject();
        result.add(key, JsonAttrUtil.toJsonTree(list));
        result.addProperty(PAGE_SIZE, page_size);
        result.addProperty(CURRENT_PAGE, currentPage);
        result.addProperty(TOTAL_PAGE, (int) Math.ceil(array.size() * 1.0 / page_size));
        return result;
    }


    private boolean check(JsonObject json, String[] keys) {
        for (String key : keys) {
            if (!json.has(key)) {
                error = "Can't find " + key;
                return false;
            }
        }
        return true;
    }
}
