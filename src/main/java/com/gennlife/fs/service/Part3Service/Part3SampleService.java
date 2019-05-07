package com.gennlife.fs.service.Part3Service;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liuzhen
 *         Created by liuzhen.
 *         Date: 2018/10/10
 *         Time: 11:13
 */
public interface Part3SampleService {
    JsonObject ouath();
    String applyOutGoing(String token, String param);
    void getSampleNumber(String param, ConcurrentHashMap<String,JSONObject> result);
    String encryptPWD(String param);
}
