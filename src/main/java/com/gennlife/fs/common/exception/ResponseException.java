package com.gennlife.fs.common.exception;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.val;
import lombok.var;

import static com.gennlife.darren.collection.string.Matching.isEmpty;

public abstract class ResponseException extends RuntimeException {

    public ResponseException() {
        super();
    }

    public ResponseException(String message) {
        super(message);
    }

    public ResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResponseException(Throwable cause) {
        super(cause);
    }

    public JSONObject toJSONObject() {
        return toJSONObject(toDetailJSONArray(this));
    }

    public JSONObject toJSONObject(JSON detail) {
        val code = code();
        val msg = getLocalizedMessage();
        return new JSONObject()
            .fluentPut("error", new JSONObject()
                .fluentPut("msg", !isEmpty(msg) ? msg : code.getDescription())
                .fluentPut("detail", detail))
            .fluentPut("errorCode", code.getValue());
    }

    public static JSONArray toDetailJSONArray(Throwable e) {
        if (e == null) {
            return null;
        }
        val ret = new JSONArray();
        {
            var x = e;
            while (x != null) {
                ret.add(new JSONObject()
                    .fluentPut("class", x.getClass().getName())
                    .fluentPut("message", x.getLocalizedMessage()));
                x = x.getCause();
            }
        }
        return ret;
    }

    abstract protected ResponseCode code();

}
