package com.gennlife.fs.service.patientsdetail.model;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Chenjinfeng on 2016/10/28.
 * 详情模板
 */
public class VisitSNResponse extends PatientDetailService implements ResponseInterface {
    private static Logger logger = LoggerFactory.getLogger(VisitSNResponse.class);
    protected String error = "";
    protected String[] keys = null;
    protected String[] staticKey = null;
    protected String[] resultkeys = null;
    protected JsonObject result = new JsonObject();

    public VisitSNResponse(String[] keys, String[] resultkeys) {
        this.keys = keys;
        this.resultkeys = resultkeys;
    }

    public VisitSNResponse(String... keys) {
        this.keys = keys;
        this.resultkeys = keys;
    }

    public VisitSNResponse(String key, String resultkey) {
        this.keys = new String[]{key};
        this.resultkeys = new String[]{resultkey};
    }

    public String[] getStaticKey() {
        return staticKey;
    }

    public void setStaticKey(String[] staticKey) {
        this.staticKey = staticKey;
    }

    public String get_error() {
        return error;
    }

    public JsonObject get_result() {
        if (StringUtil.isEmptyStr(error) && result.entrySet().size() > 0) {
            return result;
        }
        if (StringUtil.isEmptyStr(error)) {
            error = "no data";
        }
        return null;
    }

    @Override
    public void setResult(JsonObject result) {
        this.result = result;
    }

    @Override
    public void setError(String error) {
        this.error = error;
    }

    public void execute(JsonObject param_json) {
        if (param_json == null) {
            error = "empty json";
            return;
        }
        for (String key : keys) {
            if (StringUtil.isEmptyStr(key)) {
                error = "system error empty key";
                return;
            }
            if (key.contains("visits.")) {
                error = "system error key " + key;
                return;
            }
            if (key.split("\\.").length > 2) {
                error = "support nonsupport key " + key;
                return;
            }

        }
//        logger.debug("param => " + param_json);
        String patient_sn = null;
        String visit_sn = null;
        try {
            patient_sn = param_json.get("patient_sn").getAsString();
        } catch (Exception e) {
            error = "missing patient_sn";
            return;
        }
        if (param_json.has("visit_sn")) {
            visit_sn = param_json.get("visit_sn").getAsString();
        }


        JsonArray filtered_visits = null;
        QueryParam qp = new QueryParam(param_json);
        qp.query_patient_sn(patient_sn);
        if (staticKey == null || staticKey.length == 0) {
            for (String key : keys) {
                qp.addsource("visits." + key);
            }
        } else {
            for (String key : staticKey) {
                qp.addsource(key);
            }
        }
        if (null == visit_sn) {
            filtered_visits = filterPatientVisitsJsonArray(qp, param_json);
            if (filtered_visits == null) {
                error = "no data";
                return;
            }
            for (JsonElement vst : filtered_visits) {
                load(vst.getAsJsonObject());
            }
        } else {
            JsonObject visit = new JsonObject();
            JsonArray array = new JsonArray();
//            HbaseVisistsInfoTableConfigBean hbaseVisistsInfoConfig = BeansContextUtil.getHbaseVisistsInfoConfig();
//            array.add(hbaseVisistsInfoConfig.getVisistsInfoVisitInfo());
            array.add("visit_info");
            if (staticKey == null || staticKey.length == 0) {
                for (String key : keys) {
                    array.add(key);
                }
            } else {
                for (String key : staticKey) {
                    array.add(key);
                }
            }
            qp.cleanSource();
            qp.addsource(array);
            visit = get_visit(qp, visit_sn, patient_sn);
            if(visit == null){
                return;
            }
            load(visit);
        }

        result = get_result();
        removeEmpty(resultkeys);
    }

//    private boolean getData(JsonObject visit, String rowKey, Connection instance, HbaseVisistsInfoTableConfigBean hbaseVisistsInfoConfig, String[] dataColumns) {
//
//        Result dataByRowKey = HbaseUtil.getDataByRowKey(instance, hbaseVisistsInfoConfig.getVisistsInfoTableName(), rowKey, hbaseVisistsInfoConfig.getVisistsInfoCFName(), dataColumns);
//        for (String key:dataColumns) {
//            byte[] dataValue = dataByRowKey.getValue(Bytes.toBytes(hbaseVisistsInfoConfig.getVisistsInfoCFName()), Bytes.toBytes(key));
//            if (dataValue == null || dataValue.length == 0) {
//                visit.add(key, new JsonArray());
//            } else {
//                byte[] uncompress;
//                try {
//                    uncompress = Snappy.uncompress(dataValue);
//                    visit.add(key, JsonAttrUtil.toJsonElement(Bytes.toString(uncompress)));
//                } catch (Exception e) {
//                }
//            }
//        }
//        return false;
//    }
//
//    private boolean validateColumn(String rowKey, Connection instance, HbaseVisistsInfoTableConfigBean hbaseVisistsInfoConfig,String validateColumns) {
//
//        String[] visitsInfo = new String[]{validateColumns};
//        Result visitsInfoResult = HbaseUtil.getDataByRowKey(instance, hbaseVisistsInfoConfig.getVisistsInfoTableName(), rowKey, hbaseVisistsInfoConfig.getVisistsInfoCFName(), visitsInfo);
//        byte[] visitInfo = visitsInfoResult.getValue(Bytes.toBytes(hbaseVisistsInfoConfig.getVisistsInfoCFName()), Bytes.toBytes(validateColumns));
//        if(visitInfo == null || visitInfo.length == 0){
//            error = "no data";
//            return true;
//        }
//        return false;
//    }

    /**
     * 两层
     */
    protected void load(JsonObject visit) {
        for (int i = 0; i < resultkeys.length; i++) {
            String tmpkey = keys[i];
            String tmpnext = tmpkey;
            JsonObject tmpjson = visit;
            int find = tmpnext.indexOf(".");
            if (find > 0) {
                tmpkey = tmpnext.substring(0, find);
                tmpnext = tmpnext.substring(find + 1, tmpnext.length());

                if (tmpjson.has(tmpkey)) {
                    for (JsonElement elem : tmpjson.get(tmpkey).getAsJsonArray()) {
                        addJson(i, tmpnext, elem.getAsJsonObject());
                    }
                }

            } else if (!StringUtil.isEmptyStr(tmpkey)) {
                addJson(i, tmpkey, tmpjson);
            }
        }
    }

    protected void addJson(int i, String tmpkey, JsonObject tmpjson) {
        if (tmpjson != null && tmpjson.has(tmpkey)) {
            if (!result.has(resultkeys[i])) {
                result.add(resultkeys[i], tmpjson.getAsJsonArray(tmpkey));
            } else {
                result.getAsJsonArray(resultkeys[i]).addAll(tmpjson.getAsJsonArray(tmpkey));
            }
        } else if (!result.has(resultkeys[i])) {
            result.add(resultkeys[i], new JsonArray());
        }
    }

}
