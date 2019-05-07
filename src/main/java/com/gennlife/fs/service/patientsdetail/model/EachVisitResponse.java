package com.gennlife.fs.service.patientsdetail.model;

import com.gennlife.fs.common.response.PaginationMemoryResponse;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.gennlife.fs.common.response.PaginationMemoryResponse.getPaginationResult;

/**
 * Created by Chenjinfeng on 2018/1/26.
 */
public class EachVisitResponse extends VisitSNResponse {
    private JsonArray data = new JsonArray();
    private static final String DATA_KEY = "data";
    private int pageSize = 0;
    private int currentPage = 1;
    private String VISIT_SN_KEY = "VISIT_SN";
    private static final Logger logger = LoggerFactory.getLogger(EachVisitResponse.class);

    public EachVisitResponse(String[] keys, String[] resultkeys) {
        super(keys, resultkeys);
    }

    public EachVisitResponse(String... keys) {
        super(keys);
    }

    public EachVisitResponse(String key, String resultkey) {
        super(key, resultkey);
    }


    /**
     * 只支持单层
     */
    @Override
    protected void load(JsonObject visit) {
        for (int i = 0; i < resultkeys.length; i++) {
            String valueKey = keys[i];
            if (visit.has(valueKey) && !JsonAttrUtil.isEmptyJsonElement(visit.get(valueKey))) {
                JsonElement element = visit.remove(valueKey);
                if (JsonAttrUtil.isEmptyJsonElement(element)) continue;
                if (element.isJsonArray() && pageSize > 0) {
                    visit = getPaginationResult(resultkeys[i], pageSize, currentPage, element.getAsJsonArray());
                } else {
                    visit.add(resultkeys[i], element);
                }
                visit.addProperty(VISIT_SN_KEY, getVisitSn(element));
                data.add(visit);
            }
            if (!result.has(DATA_KEY))
                result.add(DATA_KEY, data);
        }

    }

    private String getVisitSn(JsonElement element) {
        String value = "";
        if (element == null) {
            return value;
        }
        if (element.isJsonObject()) {
            value = JsonAttrUtil.getStringValue(VISIT_SN_KEY, element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            try {
                value = JsonAttrUtil.getStringValue(VISIT_SN_KEY, element.getAsJsonArray().get(0).getAsJsonObject());
            } catch (Exception e) {
            }
        }
        return value == null ? "" : value;
    }

    @Override
    public void removeEmpty(String... resultkeys) {
        super.removeEmpty(DATA_KEY);
    }

    @Override
    public void execute(JsonObject param_json) {
        if (param_json != null) {
            if (param_json.has(PaginationMemoryResponse.PAGE_SIZE)) {
                try {
                    pageSize = param_json.get(PaginationMemoryResponse.PAGE_SIZE).getAsInt();
                } catch (Exception e) {
                    error = "非法的" + PaginationMemoryResponse.PAGE_SIZE;
                    return;
                }
            }
            if (param_json.has(PaginationMemoryResponse.CURRENT_PAGE)) {
                try {
                    currentPage = param_json.get(PaginationMemoryResponse.CURRENT_PAGE).getAsInt();
                    if (currentPage <= 0) currentPage = 1;
                } catch (Exception e) {
                    error = "非法的" + PaginationMemoryResponse.CURRENT_PAGE;
                    return;
                }
            }
        }
        super.execute(param_json);
    }
}
