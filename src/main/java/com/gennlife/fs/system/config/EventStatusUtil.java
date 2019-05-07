package com.gennlife.fs.system.config;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * Created by Chenjinfeng on 2016/11/4.
 */
public final class EventStatusUtil {
    //状态节点
    private static final HashMap<String,String> STATUSMAP =new HashMap<>();
    static{
        STATUSMAP.put("确诊","原发,复发,转移");
        STATUSMAP.put("疗效评估","PD,PR,SD,CR");
    }
    //事件关系表
    private static final TreeMap<String, String> event_type_map = new TreeMap<>();
    static {
        event_type_map.put("EVENT_TYPE_1", "转移");
        event_type_map.put("EVENT_TYPE_2", "放疗");
        event_type_map.put("EVENT_TYPE_3", "消融术");
        event_type_map.put("EVENT_TYPE_4", "靶向治疗");
        event_type_map.put("EVENT_TYPE_5", "化疗");
        event_type_map.put("EVENT_TYPE_6", "免疫治疗");
        event_type_map.put("EVENT_TYPE_7", "药物联合治疗");
        event_type_map.put("EVENT_TYPE_8", "临床试验药物治疗");
        event_type_map.put("EVENT_TYPE_9", "手术切除");//是否已经手术切除
        event_type_map.put("EVENT_TYPE_10", "原发,复发,转移");//原发复发或转移
        //event_type_map.put("EVENT_TYPE_11", "CR,PR,SD,PD");//对本疗程治疗的响应
        event_type_map.put("EVENT_TYPE_12", "生物治疗");
        event_type_map.put("SYSTEMICTHERAPY", "复发");
    }

    //疗效评估
    private static final String[] EVALUATION=STATUSMAP.get("疗效评估").split(",");

    //中粒度状态节点
    private static final String[] MIDDLE_STATUS_NODE ={"确诊","原发","复发","转移","疗效评估",


    };

    //干预节点
    private static final LinkedList<String> MEDDLE=new LinkedList<>();
    static{
        MEDDLE.add("手术切除");
        MEDDLE.add("主动监测");
        MEDDLE.add("放疗");
        MEDDLE.add("消融术");
        MEDDLE.add("靶向治疗");
        MEDDLE.add("化疗");
        MEDDLE.add("药物联合治疗");
        MEDDLE.add("免疫治疗");
        MEDDLE.add("临床试验药物治疗");
        MEDDLE.add("最佳支持治疗");
        MEDDLE.add("生物治疗");
    }
    //中粒度节点
    private static final LinkedList<String> MIDDLE_EVENT=new LinkedList<>();
    static{
        MIDDLE_EVENT.addAll(MEDDLE);
        MIDDLE_EVENT.add("确诊");
        MIDDLE_EVENT.add("原发");
        MIDDLE_EVENT.add("复发");
        MIDDLE_EVENT.add("转移");
    }

    /**
     * 干预与状态结点合集,fixed
     * */
    public static LinkedList<String> eventTokey(String event)
    {
        LinkedList<String> list=new LinkedList<>();
        switch (event)
        {
            case "手术切除":
                list.add("EVENT_TYPE_9");
                break;
            case "放疗":
                list.add("EVENT_TYPE_2");
                break;
            case "消融术":
                list.add("EVENT_TYPE_3");
                break;
            case "靶向治疗":
                list.add("EVENT_TYPE_4");
                break;
            case "化疗":
                list.add("EVENT_TYPE_5");
                break;
            case "药物联合治疗":
                list.add("EVENT_TYPE_7");
                break;
            case "免疫治疗":
                list.add("EVENT_TYPE_6");
                break;
            case "临床试验药物治疗":
                list.add("EVENT_TYPE_8");
                break;
            case "确诊":
            case "复发":
                list.add("EVENT_TYPE_10");
                break;
            case "疗效评估":
                list.add("EVENT_TYPE_11");
                break;
            case "转移":
            case "确诊(转移)":
                list.add("EVENT_TYPE_10");
                list.add("EVENT_TYPE_1");
                break;
            case "生物治疗":
                list.add("EVENT_TYPE_12");
        }
        return list;
    }
    public static String getExtraMiddle(String small,String middle)
    {
        StringBuffer buffer=new StringBuffer(middle);
        buffer.append('(').append(small).append(')');
        return buffer.toString();
    }

    //状态节点
    public static Set<String> getMiddleStatusNode(JsonArray events)
    {
      return getHit(events, MIDDLE_STATUS_NODE);
    }
    public static Set<String> strStatusToSet(String status)
    {
        if(StringUtil.isEmptyStr(status))return null;
        Set<String> result=new TreeSet<>();
        for(String key:status.split(","))
        {
            result.add(key);
        }
        return result;
    }
    public static TreeMap getEventTypeMap()
    {
        return event_type_map;
    }
    //获取匹配的中粒度节点
    public static Set<String> getMeddle(JsonArray events)
    {
        return getHit(events,MEDDLE);
    }
    //当前状态
    public static String getCurrentStatus(JsonArray events)
    {
        return getStringHit(events,MIDDLE_EVENT);
    }
    public static Set<String> getCurrentStatusSet(JsonArray events)
    {
        return getHit(events,MIDDLE_EVENT);
    }
    //带次数的干预
    public static LinkedList<String> getMeddleListWithTimes(JsonArray events)
    {
        LinkedList<String> status=new LinkedList<>();
        if(events==null)return status;
        for(String str:MEDDLE){
            for(JsonElement item:events)
            {
                if(str.equals(StringUtil.getTimesEvent(item.getAsString())) || str.equals(item.getAsString()))
                {
                    status.add(item.getAsString());
                }
            }
        }
        return status;
    }

    public static String getStringHit(JsonArray from,Collection<String> sources)
    {

        TreeSet<String> status=new TreeSet<>();
        if(from==null)return "";
        for(String str:sources){
            for(JsonElement item:from)
            {
                if(str.equals(StringUtil.getTimesEvent(item.getAsString())) || str.equals(item.getAsString()))
                {
                    status.add(str);
                }
            }
        }
        if(status.size()==0)return "";
        return StringUtil.getStringFromCollection(",",status);
    }
    public static Set<String> getHit(JsonArray from,String[] sources)
    {
        TreeSet<String> status=new TreeSet<>();
        if(from==null)return status;
        for(String str:sources){
            for(JsonElement item:from)
            {
                if(str.equals(StringUtil.getTimesEvent(item.getAsString())) || str.equals(item.getAsString()))
                {
                    status.add(str);
                }
            }
        }
        return status;
    }
    public static Set<String> getHit(JsonArray from,Collection<String> sources)
    {

        TreeSet<String> status=new TreeSet<>();
        if(from==null)return status;
        for(JsonElement item:from){
            for(String str:sources)
            {
                if(str.equals(StringUtil.getTimesEvent(item.getAsString())) || str.equals(item.getAsString()))
                {
                    status.add(str);
                    break;
                }
            }
        }
        return status;
    }

    public static String[] get_evaluation()
    {
        return EVALUATION;
    }


    public static JsonArray getMiddle(JsonObject visit_info)
    {

        if(visit_info==null)return null;
        if(visit_info.has("event_vita"))
        {
            JsonArray array=visit_info.getAsJsonArray("event_vita");
            TreeSet<String> result=new TreeSet<>();
            for(JsonElement jsonElement:array)
            {
                JsonObject json=jsonElement.getAsJsonObject();
                String tmp=json.get("MIDDLE").getAsString();
                result.add(tmp);
                if("确诊".equals(StringUtil.getTimesEvent(tmp)) || "确诊".equals(tmp))
                {
                    if(json.has("SMALL")&&json.get("SMALL").isJsonPrimitive())result.add(json.get("SMALL").getAsString());
                    else result.remove("确诊");
                }
            }
            return JsonAttrUtil.toJsonTree(result).getAsJsonArray();
        }
        /*else
            if(visit_info.has("EVENT_TYPE"))
            {
                JsonArray tmp=visit_info.getAsJsonArray("EVENT_TYPE");
                JsonArray result=new JsonArray();
               for(JsonElement element:tmp)
               {
                  String value=element.getAsString();
                   if(value.equals("复发")||value.equals("术后复发"))
                       result.add("确诊(复发)");
                   else
                       result.add(value);
               }
                return  result;
            }*/
        return null;
    }

    public static JsonArray getMiddle(String key, JsonObject json)
    {
        JsonObject visit_info= JsonAttrUtil.getJsonObjectValue(key,json);
        return getMiddle(visit_info);
    }

    public static JsonArray getSmall(JsonObject visit_info)
    {
        if(visit_info==null)return null;
        return JsonAttrUtil.toJsonTree(JsonAttrUtil.getJsonArrayAllValue("event_vita.SMALL",visit_info)).getAsJsonArray();
    }

    public static JsonArray getSmall(String key, JsonObject json)
    {
        JsonObject visit_info= JsonAttrUtil.getJsonObjectValue(key,json);
        return getSmall(visit_info);
    }

    /**
     * 添加event_type数据源
     * */
    public static void addEventSource(JsonArray sources)
    {
        //sources.add("visits.visit_info.EVENT_TYPE");
        sources.add("visits.visit_info.event_vita");
    }

    public static LinkedList<String> getTimeSource()
    {
        LinkedList<String> list=new LinkedList<>();
        list.add("visits.visit_info.ADMISSION_DATE");
        list.add("visits.visit_info.REGISTERED_DATE");
        return list;

    }

    public static void addEventSource(Collection<String> sources)
    {
        //sources.add("visits.visit_info.EVENT_TYPE");
        sources.add("visits.visit_info.event_vita");
    }

    public static String[] getContainSources()
    {
        return new String[]{"visits.visit_info.event_vita",
                "visits.visit_info",
                "visits"};
    }

    public static String eventContainKey(JsonArray event, String[] keys) {
        if (keys == null || keys.length == 0) return null;
        for (JsonElement elem : event) {
            for (String key : keys) {
                if (key.equals(StringUtil.getTimesEvent(elem.getAsString())) || key.equals(elem.getAsString())) {
                    return key;
                }
            }
        }
        return null;
    }

    public static String eventContainKey(JsonArray event, Collection<String> keys) {
        if (keys == null || keys.size() == 0) return null;
        for (JsonElement elem : event) {
            for (String key : keys) {
                if (key.equals(elem.getAsString()) || key.equals(StringUtil.getTimesEvent(elem.getAsString()))) {
                    return key;
                }
            }
        }
        return null;
    }

    public static String eventContainKey(JsonArray event, String key) {

        if (key == null || key.length() == 0) return null;
        for (JsonElement elem : event)
            if (key.equals(StringUtil.getTimesEvent(elem.getAsString())) || key.equals(elem.getAsString())) {
                return key;
            }
        return null;
    }

    //当前节点当前状态
    public static JsonArray getNewestMiddle(JsonObject newest)
    {
        JsonObject visit_info= JsonAttrUtil.getJsonObjectValue("newvisit.visit_info",newest);
        return getMiddle(visit_info);
    }
}
