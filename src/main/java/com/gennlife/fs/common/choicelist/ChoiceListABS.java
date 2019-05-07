package com.gennlife.fs.common.choicelist;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
/**
 * Created by Chenjinfeng on 2016/11/24.
 */
public abstract class ChoiceListABS<T> {
    protected T result;
    /**
    @param  vist_jason 在visit层
    */
    public void count(JsonArray vist_jason)
    {
        for(JsonElement one_visit : vist_jason){
            JsonObject one_visit_json = one_visit.getAsJsonObject();
            JsonArray all_reports = new JsonArray();
            if(visit_filter(one_visit_json)) continue;
            if(one_visit_json.has("inspection_reports")){
                all_reports = one_visit_json.get("inspection_reports").getAsJsonArray();
            }
            else
                continue;

            for(JsonElement one_report : all_reports){  //每个检验大项
                JsonObject one_report_jason = one_report.getAsJsonObject();
                JsonArray all_sub_reports = null;
                if(inspection_reports_filter(one_report_jason)) continue;
                if(one_report_jason.has("sub_inspection")){ //获取所有检验子项
                    all_sub_reports = one_report_jason.get("sub_inspection").getAsJsonArray();
                }

                if(all_sub_reports != null) { //如果检验项包含检索子项
                    for(JsonElement sub_elem : all_sub_reports){ //每个检验子项
                        JsonObject one_sub_report = sub_elem.getAsJsonObject();
                        boolean continueflag=countItem(one_sub_report);
                        if(!continueflag) break;
                    }
                }
            }

        }
    }

    protected abstract boolean visit_filter(JsonObject one_visit_json);

    protected abstract boolean inspection_reports_filter(JsonObject one_report_jason);

    protected abstract boolean countItem(JsonObject one_sub_report);
    public  T getResult()
    {
        return result;
    }
}
