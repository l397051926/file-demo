package com.gennlife.fs.service.patientsdetail.model;

import com.gennlife.fs.common.utils.DateUtil;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * Created by Chenjinfeng on 2016/12/13.
 */
public class VisitClassifyFollowUp {
    private HashMap<String,LinkedList<JsonObject>> follows=new HashMap<>();
    // "Follow_up","targeted_drug_follow_up","SURGERY_FOLLOW_UP"
    private static final String[] DATE_KEYS=new String[]{"FOLLOW_UP_DATE","DATE_LASTFOLLOWUP"};
    private static final String[] HASHKEYS=new String[]{"follow_up","targeted_drug_follow_up","surgery_follow_up"};
    public VisitClassifyFollowUp(JsonObject datas)
    {
        Comparator<JsonObject> comparator=new Comparator<JsonObject>() {
            @Override
            public int compare(JsonObject o1, JsonObject o2) {
                return getDate(o1).compareTo(getDate(o2));
            }
        };
        load("Follow_up",datas,"follow_up",comparator);
        load("targeted_drug_follow_up",datas,"targeted_drug_follow_up",comparator);
        load("SURGERY_FOLLOW_UP",datas,"surgery_follow_up",comparator);
    }
    public static String[] getHashKeys()
    {
        return HASHKEYS;
    }
    private void load(String datakey,JsonObject datas,String hashKey,Comparator<JsonObject> comparator)
    {
        if(datas==null) return;
        if(datas.has(datakey))
        {
            JsonObject tmp;
            LinkedList<JsonObject> list=null;
            if(follows.containsKey(hashKey))
            {
                list=follows.get(list);
            }

            for(JsonElement followitem:datas.getAsJsonArray(datakey))
            {
                tmp=followitem.getAsJsonObject();
                if(!StringUtil.isEmptyStr(getDate(tmp)))
                {
                    if(list==null)list=new LinkedList<>();
                    list.add(tmp);
                }
            }
            if(list!=null && list.size()>0){
                Collections.sort(list,comparator);
                follows.put(hashKey,list);
            }

        }
    }
    public static String getDate(JsonObject json)
    {
        return JsonAttrUtil.getStringValueMutilSource(DATE_KEYS,json);
    }
    public JsonObject getFollowUp(Date group_in_date,Date group_out_date)
    {
        JsonObject json=null;
        final HashMap<String,String> time=new HashMap<>();
        TreeSet<String> sort=new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return time.get(o1).compareTo(time.get(o2));
            }
        });
        for(String key:new LinkedList<String>(follows.keySet()))
        {
            LinkedList<JsonObject> result=getOneTypeFollowUp(key,group_in_date,group_out_date);
            if(result==null||result.size()==0) continue;
            if(json==null) json=new JsonObject();
            json.add(key, JsonAttrUtil.toJsonTree(result));
            time.put(key,getDate(result.getFirst()));
            sort.add(key);
        }
        if(json!=null){
            JsonObject sortJson=new JsonObject();
            for(String key:sort)
            {
                sortJson.add(key,json.get(key));
            }
            return sortJson;
        }
        return null;
    }

    public boolean isEmpty()
    {
        return follows==null||follows.size()==0;
    }
    private LinkedList<JsonObject> getOneTypeFollowUp(String hashKey,Date group_in_date,Date group_out_date)
    {
        if(!follows.containsKey(hashKey)) return null;
        LinkedList<JsonObject> followsList = follows.get(hashKey);
        if(followsList==null||followsList.size()==0){
            follows.remove(hashKey);
            return null;
        }
        LinkedList<JsonObject> tmplist=new LinkedList<>();
        Iterator<JsonObject> followsiter = followsList.iterator();
        while(followsiter.hasNext())
        {
            JsonObject next = followsiter.next();
            String tmpDateStr = VisitClassifyFollowUp.getDate(next);
            if(DateUtil.isInPeriod(tmpDateStr,group_in_date,group_out_date))
            {
                tmplist.add(next);
                followsList.removeFirst();
                followsiter = followsList.iterator();
            }
            else{
                if(DateUtil.date_compare(tmpDateStr,group_in_date)<0)
                {
                    followsList.removeFirst();
                    followsiter = followsList.iterator();
                }
                else
                    break;
            }

        }
        return tmplist;
    }
}
