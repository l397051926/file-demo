/**
 * copyRight
 */
package com.gennlife.fs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.collection.keypath.KeyPathSet;
import com.gennlife.fs.common.dao.JedisClusterDao;
import com.gennlife.fs.common.exception.FormatCorruptedException;
import com.gennlife.fs.common.exception.NotFoundException;
import com.gennlife.fs.common.exception.TransferFailedException;
import com.gennlife.fs.common.exception.UnexpectedException;
import com.gennlife.fs.common.utils.HttpClient;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.KeyPathUtil;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Builder;
import lombok.ToString;
import lombok.val;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.gennlife.darren.controlflow.exception.Try.try_;
import static com.gennlife.fs.common.utils.HttpRequestUtil.postData;
import static java.util.stream.Collectors.toCollection;

@Service
public class EmpiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmpiService.class);

    @Builder
    @ToString
    public static class FetchPatientDataParameters {
        String patientSn;
        KeyPathSet fields;
    }

    public JSONObject fetchPatientInfo(FetchPatientDataParameters params) {
        val patientInfoFields = params.fields.subSet(PATIENT_INFO);
        val request = try_(
            () -> new JSONObject()
                .fluentPut("UserId", "FileService")
                .fluentPut("Uuids", new JSONArray()
                    .fluentAdd(params.patientSn))
                .fluentPut("Source", patientInfoFields
                    .stream()
                    .map(KeyPathUtil::toPathString)
                    .collect(toCollection(JSONArray::new))))
            .orElse(null);
        if (request == null) {
            throw new UnexpectedException("Malformed parameters: " + params);
        }
        val response = try_(
            () -> postData(cfg.empiServiceEndpoint, cfg.empiServicePatientInfoApi, request))
            .orElse(null);
        if (response == null) {
            throw new TransferFailedException();
        }
        val x = try_(
            () -> JSON.parseObject(response))
            .orElse(null);
        if (x == null) {
            throw new FormatCorruptedException("Response is not a valid JSON object: " + response);
        }
        val o = try_(
            () -> PATIENT_INFO_SOURCE_PATH.resolveAsJSONObject(x))
            .orElse(null);
        if (o == null) {
            throw new NotFoundException("Patient " + params.patientSn + " may not exist. Response: " + response);
        }
        val ret = new JSONObject();
        PATIENT_INFO_TARGET_PATH.assign(ret, o);
        return ret;
    }

    @Autowired
    private GeneralConfiguration cfg;

    private static final String PATIENT_INFO = "patient_info";
    private static final KeyPath PATIENT_INFO_SOURCE_PATH = KeyPath.compile("Results[0]");
    private static final KeyPath PATIENT_INFO_TARGET_PATH = new KeyPath(PATIENT_INFO, 0);


    // @TODO: Rewrite ↓

    public String getEMPIInfo(String param) {
        LOGGER.info("url = {},param={}",EMPIServerUrl,param);
        try {
            JsonObject json = JsonAttrUtil.toJsonElement(param).getAsJsonObject();
            String inPatientSn = JsonAttrUtil.getStringValue("InPatientSn", json);
            String idCard = JsonAttrUtil.getStringValue("IDCard",json);
            try {
                if (!inPatientSn.trim().equals("") || !idCard.trim().equals("")){
                    JsonObject oldObj = new JsonParser().parse(param).getAsJsonObject();
                    JsonObject newObj = new JsonObject();
                    newObj.addProperty("IDCard",oldObj.has("IDCard") ? oldObj.get("IDCard").getAsString() : "");
                    newObj.addProperty("InPatientSn",oldObj.has("InPatientSn") ? oldObj.get("InPatientSn").getAsString() : "");
                    param = new Gson().toJson(newObj);
                    LOGGER.info("调取增量服务：" + param);
                    long begin = System.currentTimeMillis();
                    httpClient.executPostWithBody(writeServiceUrl, param, 30000);
                    long end = System.currentTimeMillis();
                    LOGGER.info("etl实时写入ss的时间为： "+ (begin-end));
                }
            } catch (Exception e) {
                LOGGER.info("/gennlife/temp 接口访问不通");
            }
            return httpClient.executPostWithBody(EMPIServerUrl, genParam(inPatientSn,idCard), 30000);
        } catch (Exception e) {
            LOGGER.error("error,{}",e);
        }
        return null;
    }


    public String patientSN(String empiInfo,String propertyName) {
        JsonObject result = new JsonObject();
        if(StringUtils.isEmpty(empiInfo)){
            result.addProperty("success",false);
            result.addProperty("RESPONSE_ERROR","can not search patient");
            return result.getAsJsonObject().toString();
        }
        JsonObject info = JsonAttrUtil.toJsonElement(empiInfo).getAsJsonObject();
        JsonArray uuids = JsonAttrUtil.getJsonArrayValue(propertyName, info);
        int size = uuids == null ? 0 : uuids.size();

        Gson gson = new Gson();
        if(size >= 0){
            result.addProperty("success",true);
            result.add("pat_sn",uuids);
            return result.getAsJsonObject().toString();
        }else{
            result.addProperty("success",false);
            result.addProperty("RESPONSE_ERROR","can not search  pat_sn");
            return result.getAsJsonObject().toString();
        }
    }

    private String genParam(String inpatient_sn, String patient_id) {
        JsonObject param = new JsonObject();
        param.addProperty("UserId", "fs");
        JsonObject query = new JsonObject();
        if (StringUtils.isNotEmpty(inpatient_sn)) {
            byte[] bytes = Base64.decodeBase64(inpatient_sn);
            inpatient_sn = new String(bytes);
            query.addProperty("InPatientSn", inpatient_sn);
        }
        if (StringUtils.isNotEmpty(patient_id)) {
            byte[] bytes = Base64.decodeBase64(patient_id);
            patient_id = new String(bytes);
            query.addProperty("IDCard", patient_id);
        }
        param.add("Query", query);
        String params = param.getAsJsonObject().toString();
        LOGGER.info("根据住院号和身份证号组装的参数={}", params);
        return params;
    }

    @Value("${fs.empi.server.url}")
    private String EMPIServerUrl;
    @Value("${fs.writeServiceUrl}")
    private String writeServiceUrl;
    @Autowired
    private HttpClient httpClient;

    private static JedisClusterDao jedisClusterDao = JedisClusterDao.getRedisDao();
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);

}
