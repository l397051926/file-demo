package com.gennlife.fs.common.utils;

import com.gennlife.fs.common.comparator.JsonComparatorInterface;
import com.gennlife.fs.service.patientsdetail.serviceitem.TripleTestTable;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Json 字段过滤和保留
 * Created by Chenjinfeng on 2016/9/30.
 */
public class JsonAttrUtil {
    private static JsonParser jsonParser = new JsonParser();
    private static Gson gson = new Gson();
    private static Logger logger = LoggerFactory.getLogger(JsonAttrUtil.class);

    public static JsonArray getJsonArray(String[] arr){
        JsonArray result = new JsonArray();
        int length = arr.length;
        for (int i = 0; i < length; i++) {
            String str = arr[i];
            result.add(str);
        }
        return result;
    }
    public static JsonArray getSingleValJsonArrray(Object arr){
        JsonArray result = new JsonArray();
        result.add(String.valueOf(arr));
        return result;
    }

    public static JsonObject getJsonObject(String key,String val){
        JsonObject object = new JsonObject();
        object.addProperty(key,val);
        return object;
    }

    public static String getValByJsonObject(JsonObject obj,String key){
        if(obj.has(key)){
            return obj.get(key).getAsString();
        }else {
            return "";
        }
    }

    public static JsonArray array_combine(JsonArray... arrays) {
        JsonArray result = new JsonArray();
        if (arrays.length == 0) return result;
        for (JsonArray item : arrays)
            if (item != null && item.size() > 0) {
                result.addAll(item);
            }
        return result;
    }

    public static void safelyAdd(JsonObject json, String key, JsonElement fromElem) {
        try {
            json.add(key, fromElem.getAsJsonArray().get(0));
        } catch (Exception e) {
        }
    }

    public static JsonObject analyse(String str, JsonObject json) {
        String[] keys = str.split("\\.");

        if (keys.length == 0) return null;
        for (int i = 0; i < keys.length - 1; i++) {
            if (json == null) return null;
            if (json.has(keys[i])) {
                try {
                    json = json.getAsJsonArray(keys[i]).get(0).getAsJsonObject();
                } catch (Exception e) {
                    logger.error("", e);
                    return null;
                }
            }
        }
        if (json.has(keys[keys.length - 1])) {
            json = json.getAsJsonObject(keys[keys.length - 1]);
            return json;
        } else
            return null;

    }

    public static JsonObject getOneFromJsonArray(String[] leave, JsonElement fromElem) {
        JsonElement from = null;
        try {
            from = fromElem.getAsJsonArray().get(0);
        } catch (Exception e) {
            return null;
        }
        return Leave(leave, from);

    }

    public static JsonObject LeaveWithDefaultValue(String[] leave, JsonElement fromElem, JsonElement defaultvalue) {
        JsonObject from = null;
        try {
            from = fromElem.getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
        JsonObject json = new JsonObject();
        for (String attr : leave) {
            JsonElement value = from.get(attr);
            if (value == null) {
                if (defaultvalue != null)
                    value = defaultvalue;
            }
            if (value != null) json.add(attr, value);
        }
        return json;
    }

    public static JsonObject Leave(String[] leave, JsonElement fromElem) {
        return LeaveWithDefaultValue(leave, fromElem, null);
    }


    /**
     * 批量赋值
     */
    public static JsonArray setAttr(String key, String value, JsonArray array) {
        for (JsonElement item : array)
            item.getAsJsonObject().addProperty(key, value);
        return array;
    }

    /**
     * 批量赋值
     */
    public static JsonArray setAttr(String key, int value, JsonArray array) {
        for (JsonElement item : array)
            item.getAsJsonObject().addProperty(key, value);
        return array;
    }

    /**
     * json联合
     */
    public static JsonArray combineJsonArray(JsonArray a1, JsonArray a2) {
        if (a1 == null && a2 == null) return null;
        if (a1 == null) return a2;
        if (a2 == null) return a1;
        a1.addAll(a2);
        return a1;

    }

    /**
     * 验证多个key是否存在
     */
    public static boolean checkKeys(JsonObject json, String[] keys) {
        if (json == null) return false;
        for (String key : keys)
            if (json.has(key) == false)
                return false;
        return true;
    }


    public static void add_propery(JsonObject from, JsonObject to, String... keys) {
        for (String key : keys) {
            if (from.has(key)) to.add(key, from.get(key));

        }
    }

    public static boolean isEmptyArray(JsonObject json, String key) {
        if (!json.has(key)) return true;
        JsonElement elem = json.get(key);
        if (elem == null || !elem.isJsonArray()) return true;
        if (elem.getAsJsonArray().size() <= 0) return true;
        return false;

    }

    public static JsonElement toJsonTree(Object obj) {
        return gson.toJsonTree(obj);
    }

    public static JsonObject toJsonObjectWithLenient(String str) {
        if (StringUtil.isEmptyStr(str)) return null;
        try {
            return jsonParser.parse(new JsonReader(new StringReader(str))).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            logger.error("", e);
        } catch (IllegalStateException e) {
            logger.error("", e);
        } catch (NullPointerException e) {
            logger.error("", e);
        }
        return null;

    }

    public static JsonObject toJsonObject(Object obj) {
        if (obj != null && obj instanceof JsonObject) return (JsonObject) obj;
        try {
            return (JsonObject) toJsonTree(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonObject toJsonObject(String str) {
        if (StringUtil.isEmptyStr(str)) return null;
        try {
            return jsonParser.parse(str).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            // logger.error("",e);
        } catch (IllegalStateException e) {
            //logger.error("",e);
        } catch (NullPointerException e) {
            // logger.error("",e);
        }
        return null;

    }

    public static boolean has_key(JsonObject json, String key) {
        if (json == null) return false;
        if (!json.has(key)) return false;
        if (json.get(key).isJsonNull()) return false;
        if (json.get(key).isJsonPrimitive()) {
            return !StringUtil.isEmptyStr(json.get(key).getAsString());
        }
        return true;
    }


    public static String toJsonStr(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(JsonElement array, Type collection_type) {
        return gson.fromJson(array, collection_type);
    }

    public static <T> T fromJson(String json, Type class_type) {
        return gson.fromJson(json, class_type);
    }

    public static JsonObject getJsonObjectfromFile(String jsonfile) {
        return toJsonObject(getJsonStringfromFile(jsonfile));
    }

    public static String getJsonStringfromFile(String jsonfile) {
        StringBuffer buffer = new StringBuffer();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(jsonfile), "UTF-8"));
            String line = "";
            line = br.readLine();
            while (line != null) {
                buffer.append(line.trim());
                line = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
        return buffer.toString();
    }

    public static List<JsonElement> getPagingResult(List<JsonElement> datalist, int page_size, int currentPage) {
        Iterator<JsonElement> iter = datalist.iterator();
        int offset = (currentPage - 1) * page_size;
        LinkedList<JsonElement> list = new LinkedList<JsonElement>();
        if (datalist == null || offset >= datalist.size())
            return list;
        if (currentPage == 1 && page_size > datalist.size())
            return datalist;
        while (iter.hasNext() && offset > 0) {
            iter.next();
            offset--;
        }
        int nowsize = page_size;
        while (iter.hasNext() && nowsize > 0) {
            list.add(iter.next());
            nowsize--;
        }
        return list;
    }

    public static LinkedList<JsonElement> jsonArrayToList(JsonArray array) {
        return JsonAttrUtil.fromJson(array, new TypeToken<LinkedList<JsonElement>>() {
        }.getType());
    }

    public static JsonArray strToJsonArray(JsonObject json, String key) {
        JsonArray result = new JsonArray();
        if (json.has(key)) {
            String[] tmp = json.get(key).getAsString().split(" ");
            for (String item : tmp) {
                result.add(item);
            }
        }
        return result;
    }

    public static String getStringValue(String key, JsonObject tmp) {
        String[] keys = key.split("\\.");
        Object obj = getObjValue(keys, tmp);
        if (obj instanceof String) return (String) obj;
        if (obj == null) return "";
        return obj.toString();
    }

    public static String getStringValueSplit(String[] keys, JsonObject tmp) {
        Object obj = getObjValue(keys, tmp);
        if (obj instanceof String) return (String) obj;
        if (obj == null) return null;
        return obj.toString();
    }

    public static boolean isEmptyJsonElement(JsonElement json) {
        if (json == null || json.isJsonNull()) return true;
        if (json.isJsonPrimitive() && StringUtil.isEmptyStr(json.getAsString())) return true;
        if (json.isJsonObject() && json.getAsJsonObject().entrySet().size() == 0) return true;
        return json.isJsonArray() && json.getAsJsonArray().size() == 0;
    }

    public static JsonArray getJsonArrayValue(String key, JsonObject tmp) {
        String[] keys = key.split("\\.");
        Object obj = getObjValue(keys, tmp);
        if (obj instanceof JsonArray) return (JsonArray) obj;
        return null;
    }

    /**
     * 数组取第一项
     */
    public static JsonObject getJsonObjectValue(String key, JsonObject tmp) {
        String[] keys = key.split("\\.");
        Object obj = getObjValue(keys, tmp);
        if (obj instanceof JsonArray) {
            if (((JsonArray) obj).size() > 0)
                try {
                    return ((JsonArray) obj).get(0).getAsJsonObject();
                } catch (Exception e) {
                    logger.error("", e);
                }
            else
                return null;
        } else if (obj instanceof JsonObject)
            return (JsonObject) obj;
        return null;
    }

    public static Object getObjValue(String[] keys, JsonObject tmp) {
        if (tmp == null) return null;
        for (int i = 0; i < (keys.length - 1); i++) {
            if (tmp.has(keys[i])) {
                JsonElement tmpelem = tmp.get(keys[i]);
                if (tmpelem.isJsonArray()) {
                    if (tmpelem.getAsJsonArray().size() == 0) return null;
                    tmp = tmpelem.getAsJsonArray().get(0).getAsJsonObject();
                } else if (tmpelem.isJsonObject())
                    tmp = tmpelem.getAsJsonObject();
                else if (tmpelem.isJsonNull())
                    return null;
            } else
                return null;

        }
        if (tmp.has(keys[keys.length - 1])) {
            JsonElement result = tmp.get(keys[keys.length - 1]);
            if (result.isJsonPrimitive()) return result.getAsString();
            else if (result.isJsonArray()) return result.getAsJsonArray();
            else if (result.isJsonNull()) return null;
            else if (result.isJsonObject()) return result.getAsJsonObject();
            else return result.toString();
        }
        return null;
    }

    public static String getStringValueFromMoreSources(JsonObject visit, String[] source) {
        String result = null;
        for (String key : source) {
            result = JsonAttrUtil.getStringValue(key, visit);
            if (!StringUtil.isEmptyStr(result)) {
                break;
            }
        }
        if (StringUtil.isEmptyStr(result)) return null;
        return result;
    }

    public static String getStringValueMutilSource(String[] sources, JsonObject json) {
        if (json == null) return null;
        if (sources == null || sources.length == 0) return null;
        for (String source : sources) {
            String value = getStringValue(source, json);
            if (!StringUtil.isEmptyStr(value))
                return value;
        }
        return null;
    }

    public static JsonArray getJsonArrayValueMutilSource(String[] sources, JsonObject json) {
        if (json == null) return null;
        if (sources == null || sources.length == 0) return null;
        for (String source : sources) {
            JsonArray value = getJsonArrayValue(source, json);
            if (value != null)
                return value;
        }
        return null;
    }

    //所有数据填充
    public static LinkedList<JsonElement> getJsonArrayAllValue(String key, JsonObject data) {
        LinkedList<JsonElement> tmplist = new LinkedList<>();
        int find = -1;
        LinkedList<JsonElement> resultlist = new LinkedList<>();
        resultlist.add(data);
        LinkedList<JsonElement> swap = null;
        String head = null;
        while (!StringUtil.isEmptyStr(key)) {
            find = key.indexOf('.');
            if (find > 0) {
                head = key.substring(0, find);
                key = key.substring(find + 1);
            } else {
                head = key;
                key = null;
            }
            for (JsonElement element : resultlist) {
                if (element.isJsonObject()) {
                    JsonObject tmp = element.getAsJsonObject();
                    if (tmp.has(head)) {
                        JsonElement tmpelement = tmp.get(head);
                        if (tmpelement.isJsonArray())
                            arrayToCollection(tmplist, tmpelement.getAsJsonArray());
                        else
                            tmplist.add(tmpelement);
                    }
                }
            }
            swap = tmplist;
            tmplist = resultlist;
            resultlist = swap;
            tmplist.clear();
        }

        return resultlist;

    }

    public static void arrayToCollection(Collection collection, JsonArray array) {
        for (JsonElement element : array) {
            collection.add(element);
        }
    }

    public static boolean isNotEmptyArray(JsonObject json, String key) {
        if (json.has(key)) {
            JsonElement element = json.get(key);
            if (element.isJsonArray() && element.getAsJsonArray().size() > 0)
                return true;
        }
        return false;

    }

    public static boolean isNotEmptyKey(JsonObject json, String key) {
        if (json.has(key)) {
            JsonElement element = json.get(key);
            if (element.isJsonArray() && element.getAsJsonArray().size() > 0)
                return true;
            else if (element.isJsonPrimitive() && !StringUtil.isEmptyStr(element.getAsString()))
                return true;
            else if (element.isJsonObject() && element.getAsJsonObject().entrySet().size() > 0)
                return true;
        }
        return false;
    }

    public static LinkedList<JsonElement> getListFromJsonArray(String key, JsonObject json) {
        try {
            return JsonAttrUtil.fromJson(JsonAttrUtil.getJsonArrayValue(key, json), new TypeToken<LinkedList<JsonElement>>() {
            }.getType());
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
    }

    public static boolean arrayContain(JsonArray array, String value) {
        if (StringUtil.isEmptyStr(value)) return false;
        if (array == null || array.size() == 0) return false;
        for (JsonElement elem : array) {
            if (value.equals(elem.getAsString())) return true;
        }
        return false;
    }

    public static JsonParser getJsonpParse() {
        return jsonParser;
    }

    public static JsonElement toJsonElement(InputStream content) {
        InputStreamReader inputStream = null;
        JsonElement result = null;
        JsonReader reader = null;
        try {
            inputStream = new InputStreamReader(content, "utf-8");
            reader = new JsonReader(inputStream);
            result = jsonParser.parse(reader);
        } catch (Exception e) {
            logger.error("", e);
            result = null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }
        return result;
    }

    public static JsonElement toJsonElement(String s) {
        try {
            if (StringUtil.isEmptyStr(s)) return null;
            return jsonParser.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static LinkedList<JsonElement> sort(Collection<JsonElement> datas, JsonComparatorInterface<String> comparator) {
        LinkedList<JsonElement> empty = new LinkedList<>();
        LinkedList<JsonElement> hasValue = new LinkedList<>();
        for (JsonElement dataItem : datas) {
            if (StringUtil.isEmptyStr(comparator.getValue(dataItem))) {
                empty.add(dataItem);
            } else
                hasValue.add(dataItem);
        }
        Collections.sort(hasValue, comparator);
        hasValue.addAll(empty);
        return hasValue;
    }

    public static JsonObject addProperyJsonObject(JsonObject tmpObj, JsonObject obj) {
        for (Map.Entry<String, JsonElement> entrys  : obj.entrySet()){
            String key = entrys.getKey();
            Set<String> set = new HashSet<>();
            set.add("TEMPRATURE");
            set.add("PULSE");
            set.add("BREATH");
            set.add("DIASTOLIC");
            set.add("SYSTOLIC");
            set.add("EXECUTE_DEPT");
            set.add("EXECUTE_DEPT_CODE");
            set.add("VISIT_SN");
            set.add("PATIENT_SN");
            set.add("VISIT_TYPE");
            set.add("EXAM_TIME");
            set.add("RECORD_DATE");
            set.add("BLOOD_PRESSURE");
            set.add("HOSPITAL_DISCHARGE_DATE");
            set.add("HOSPITAL_ADMISSION_DATE");
            set.add("OPERATION_DATE");
            set.add("EXAM_TIME_HOUR");
            set.add("HEART_RATE");
            if(TripleTestTable.isTriple(key,set)){
                continue;
            }
            String val = entrys.getValue().getAsString();
            if (tmpObj.has(key)){
                val = tmpObj.get(key).getAsString() + "|" + val;
            }
            tmpObj.addProperty(key,val);
        }
        return tmpObj;
    }
}
