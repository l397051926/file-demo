package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.dao.JedisClusterDao;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.model.IndexChangeResultEntity;
import com.gennlife.fs.service.patientsdetail.model.TimeValueEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Created by Chenjinfeng on 2018/3/20.
 */
public abstract class CacheForOneService {
    protected static final int EXPIRE_TIME = 30 * 60 * 1000;
    public static final String ALL_KEY = "all";
    public static final String ID_VALUE_KEY = "idValue";
    protected static JedisClusterDao jedisClusterDao = JedisClusterDao.getRedisDao();

    public JsonObject getIdList(String patient_sn, String id, String start, String end) {
        JsonObject idJson = getMatch(patient_sn, ID_VALUE_KEY).getAsJsonObject();
        if (!idJson.has(id)) return null;
        JsonObject match = idJson.get(id).getAsJsonObject();
        if (StringUtil.isEmptyStr(start) && StringUtil.isEmptyStr(end)) return match;
        IndexChangeResultEntity result = JsonAttrUtil.fromJson(match, IndexChangeResultEntity.class);
        result.sub(start, end);
        return JsonAttrUtil.toJsonObject(result);
    }

    public JsonElement getMatch(String patient_sn, String idValueKey)
    {
        return getMatch(patient_sn,idValueKey,null);
    }

    public abstract String getCacheKey(String patient_sn);

    /**
     * visit_sn : [list]
     * ALL_KEY : [all list]
     * ID_VALUE_KEY : {all value}
     */
    public abstract Map<String, JsonElement> getData(String patient_sn);

    public final JsonElement getMatch(String patient_sn, String field,String key) {
        String cacheKey = getCacheKey(patient_sn);
        if (StringUtil.isEmptyStr(field)) field = ALL_KEY;
        if (!jedisClusterDao.isExists(cacheKey)) {
            synchronized (getLock()) {
                if (jedisClusterDao.isExists(cacheKey)) {
                    jedisClusterDao.expire(cacheKey, EXPIRE_TIME);
                    return JsonAttrUtil.toJsonElement(jedisClusterDao.hget(cacheKey, field));
                }
                Map<String, JsonElement> result = getData(patient_sn);
                if (result == null) return null;
                Map<String, String> setRedisMap = new HashMap<>();
                for (Map.Entry<String, JsonElement> item : result.entrySet()) {
                    setRedisMap.put(item.getKey(), JsonAttrUtil.toJsonStr(item.getValue()));
                }
                jedisClusterDao.hmset(cacheKey, setRedisMap, EXPIRE_TIME);
                return search(result.get(field),key);
            }
        }
        jedisClusterDao.expire(cacheKey, EXPIRE_TIME);
        return search(JsonAttrUtil.toJsonElement(jedisClusterDao.hget(cacheKey, field)),key);
    }
    public JsonElement search(JsonElement element,String key){
        if(element==null||!element.isJsonArray()|| element.getAsJsonArray().size()==0)return element;
        if(StringUtil.isDateEmpty(key))return element;
        return searchByKey(element.getAsJsonArray(),key);
    }

    protected abstract JsonArray searchByKey(JsonArray array, String key);

    protected void setTimeAndValue(IndexChangeResultEntity resultItem, TreeSet<TimeValueEntity> dataList) {
        ArrayList<String> time = new ArrayList<>(dataList.size());
        ArrayList<Double> value = new ArrayList<>(dataList.size());
        resultItem.setTime(time);
        resultItem.setValue(value);
        for (TimeValueEntity timeValueEntity : dataList) {
            time.add(timeValueEntity.getTime());
            value.add(timeValueEntity.getValue());
        }
    }

    protected abstract Object getLock();
}
