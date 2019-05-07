package com.gennlife.fs.common.utils;

import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * Created by Chenjinfeng on 2017/3/14.
 */
public class VisitUtil {
    /**
     * @param  visits 含有visit_info
     * @param asc 升序
     * **/
    public static VisitSTimeModel sortVisitByVisitTime(Collection<JsonObject> visits, final boolean asc, boolean saveEmpty)
    {
        if(visits==null||visits.size()==0) return null;
        LinkedList<JsonObject> notHasTime=new LinkedList<>();
        LinkedList<JsonObject> hasTime=new LinkedList<>();
        for(JsonObject visit:visits)
        {
            String date=StringUtil.get_visit_date(visit);
            if(StringUtil.isDateEmpty(date)) {
                if(saveEmpty)notHasTime.add(visit);
            }
            else hasTime.add(visit);
        }
        Collections.sort(hasTime, new Comparator<JsonObject>() {
            @Override
            public int compare(JsonObject o1, JsonObject o2) {
                if(asc){
                    return StringUtil.get_visit_date(o1).compareTo(StringUtil.get_visit_date(o2));
                }
                else
                    return StringUtil.get_visit_date(o2).compareTo(StringUtil.get_visit_date(o1));
            }
        });
        return new VisitSTimeModel(hasTime,notHasTime);
    }
}

class VisitSTimeModel{
    Collection<JsonObject> hasTime;
    Collection<JsonObject> notHasTime;

    public VisitSTimeModel(Collection<JsonObject> hasTime, Collection<JsonObject> notHasTime) {
        this.hasTime = hasTime;
        this.notHasTime = notHasTime;
    }

    public Collection<JsonObject> getHasTime() {
        return hasTime;
    }

    public Collection<JsonObject> getNotHasTime() {
        return notHasTime;
    }
}

