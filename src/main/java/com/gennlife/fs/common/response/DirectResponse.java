package com.gennlife.fs.common.response;

import com.gennlife.fs.common.utils.StringUtil;
import com.google.gson.JsonObject;

/**
 * Created by Chenjinfeng on 2017/6/21.
 */
public class DirectResponse implements ResponseInterface {
    private JsonObject result;
    private String error;
    public DirectResponse(JsonObject result,String error) {
        this.result = result;
        this.error=error;
    }
    public DirectResponse(JsonObject result) {
        this.result = result;

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
        this.result=result;
    }

    @Override
    public void setError(String error) {
        this.error=error;
    }

    @Override
    public void execute(JsonObject param_json) {
        if(this.result==null&& StringUtil.isEmptyStr(error))error="no data";
    }
}
