package com.gennlife.fs.common.response;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Created by Chenjinfeng on 2016/10/28.
 */
public interface ResponseInterface {
    public String get_error();
    public JsonObject get_result();
    public void setResult(JsonObject result);
    public void setError(String error);
    public void execute(JsonObject param_json);
    public default void removeEmpty(String... resultkeys)
    {
        int size = resultkeys.length;
        int emptySize = 0;
        JsonObject result = this.get_result();
        for (String tmpKey : resultkeys) {
            JsonArray array = JsonAttrUtil.getJsonArrayValue(tmpKey, result);
            if (array == null || array.size() == 0) emptySize++;
        }
        if (emptySize == size) {
            setResult(null);
            setError("no data");
        }
    }
}
