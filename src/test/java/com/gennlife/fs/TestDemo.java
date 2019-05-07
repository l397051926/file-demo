package com.gennlife.fs;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static com.gennlife.fs.service.patientsdetail.serviceitem.LabResultItemList.exchange;

/**
 * @author liumingxin
 * @create 2018 31 18:43
 * @desc
 **/
public class TestDemo {
    private static JsonParser jsonParser = new JsonParser();

    public static void main(String[] args) throws IOException {
        BufferedReader br;
        br = new BufferedReader(new InputStreamReader(new FileInputStream("a.json"), "utf-8"));
        String onLine = null;
        StringBuffer str = new StringBuffer();
        while ((onLine = br.readLine()) != null) {
            str.append(onLine.trim());
        }
        String ss = str.toString();
        JsonObject jsonObject = (JsonObject) new JsonParser().parse(ss);
        JsonArray visits = jsonObject.getAsJsonArray("visits");
        String item_name_cn = null;
        for (JsonElement one_visit : visits) {
            JsonObject one_visit_json = one_visit.getAsJsonObject();
            JsonArray all_reports = null;
            if (one_visit_json.has("inspection_reports")) {
                all_reports = one_visit_json.get("inspection_reports").getAsJsonArray();
            } else {
                continue;
            }

            for (JsonElement one_report : all_reports) {
                JsonObject one_report_jason = one_report.getAsJsonObject();
                JsonArray all_sub_reports = null;
                String inspectionName = exchange(JsonAttrUtil.getStringValue("INSPECTION_NAME", one_report_jason));
                if (item_name_cn == null || item_name_cn.isEmpty()
                        || item_name_cn.equals(inspectionName)) {
                    //获取所有检验子项
                    all_sub_reports = JsonAttrUtil.getJsonArrayValue("sub_inspection", one_report_jason);
                    if(all_sub_reports ==null){
                        continue;
                    }
                    if (one_report_jason.has("REPORT_TIME"))
                        JsonAttrUtil.setAttr("REPORT_TIME", one_report_jason.get("REPORT_TIME").getAsString(), all_sub_reports);
                }

                if (all_sub_reports != null) {
                    for (JsonElement sub_elem : all_sub_reports) { //每个检验子项
                        JsonObject one_sub_report = sub_elem.getAsJsonObject();
                    }
                }
                // of if(one_report_jason
            }// of for(JsonElement one_report
        } // for(JsonElement one_visit
        System.out.println(ss);
    }

}
