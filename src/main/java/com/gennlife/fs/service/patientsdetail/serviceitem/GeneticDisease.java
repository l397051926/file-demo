package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * Created by Xuhui on 2016/7/18.
 * 基因型-遗传性疾病（组学数据）
 * GERMLINE 数据
 * demo:
     {
         "patient_sn":"42001471410153191097",
         "currentTable":"genetic-disease",
         "current_page":1,
         "page_size":12,
         "limit":"1,12",
         "roles":[{}]
     }
 */
public class GeneticDisease extends PatientDetailService {
	private static final Logger logger = LoggerFactory.getLogger(GeneticDisease.class);
    public String getGeneticDisease(String param){
    	logger.info("getGeneticDisease(): REQUEST genetic disease by variation id..." + param);
                
        String patient_sn = null;
        int current_page = 0;
        int page_size = 0;
        JsonObject param_json = JsonAttrUtil.toJsonObject(param);
        if(param_json==null) return ResponseMsgFactory.buildFailStr(" not json");
        if (param_json.has("patient_sn")) {
            patient_sn = param_json.get("patient_sn").getAsString();
        }
        if (param_json.has("current_page")) {
            current_page = param_json.get("current_page").getAsInt();
        }
        if (param_json.has("page_size")) {
            page_size = param_json.get("page_size").getAsInt();
        }


        JsonObject result = new JsonObject();
        result.add("geneticDisease", new JsonArray());


        QueryParam qp=new QueryParam(param_json,patient_sn,"germline.detection_result");
        JsonObject search=get_data(qp);
        JsonArray genomics = JsonAttrUtil.getJsonArrayValueMutilSource(new String[]{"germline"},search);
        if(genomics==null || genomics.size()==0)
        {
            result.addProperty("hasgenomics",false);
            result.addProperty("msg","search no data");
            return ResponseMsgFactory.buildResponseStr(result);
        }

        JsonObject genomics_obj = genomics.get(0).getAsJsonObject();
        if(genomics_obj!=null&&genomics_obj.has("detection_result")) {
        	genomics = genomics_obj.getAsJsonArray("detection_result");
        } else {
            JsonObject json=new JsonObject();
            json.addProperty("hasgenomics",false);
            return ResponseMsgFactory.buildResponseStr(json);
        }
        Comparator<JsonElement> comparator=new Comparator<JsonElement>() {
            @Override
            public int compare(JsonElement o1, JsonElement o2) {

                return getKey(o1.getAsJsonObject()).compareTo(getKey(o2.getAsJsonObject()));
            }
            private String getKey(JsonObject json)
            {
                String result="";
                if(json.has("RS_ID"))result+=json.get("RS_ID");
                if(json.has("GENE_SYMBOL"))result+=json.get("GENE_SYMBOL");
                if(json.has("GENOTYPE"))result+=json.get("GENOTYPE");
                return result;

            }

        };
        TreeSet<JsonElement> genomicsSet= new TreeSet<JsonElement>(comparator);
        genomicsSet.addAll((LinkedList<JsonElement>) JsonAttrUtil.fromJson(genomics,new TypeToken<LinkedList<JsonElement>>(){}.getType()));
        genomics=null;

        JsonObject request_json_obj = getRequire(genomicsSet);
        logger.info("getGeneticDisease(): request ..." + request_json_obj.toString());

        JsonElement response_str = HttpRequestUtils.httpGetJson(knowledge_service_uri + "/knowledge/graph?param="+ URLEncoder.encode(request_json_obj.toString()));
        logger.debug("getGeneticDisease(): get response ..." + response_str);

        final JsonObject response_json = JsonAttrUtil.toJsonObject(response_str);;
        JsonArray disease_array = null;
        if(response_json==null)
        {
            result.addProperty("hasgenomics",true);
            result.addProperty("msg","knowledge no data");
            return ResponseMsgFactory.buildSuccessStr(result);
        }
        if (response_json.has("data")) {
            disease_array = response_json.getAsJsonArray("data");
        }
      /*  System.out.println("\n==============");
        for(Map.Entry<String, JsonElement> item: rs_id_to_disease_obj.entrySet())
        {
            if(item.getValue().getAsJsonArray().size()>0)
            {
                System.out.print(item.getKey()+",");
            }
        }
        System.out.println("\n================");*/

        LinkedList<JsonElement> total_disease_records = new LinkedList<>();
        LinkedList<JsonElement> empty=new LinkedList<>();
        for (JsonElement elem2 : disease_array) {
        for (JsonElement elem1: genomicsSet) {
            JsonObject genomic_obj = elem1.getAsJsonObject();
            if (genomic_obj.has("VARIATION_SOURCE")
                    && genomic_obj.get("VARIATION_SOURCE").getAsString().equalsIgnoreCase("GERMLINE")
                    && genomic_obj.has("RS_ID")) {
                String rs_id = genomic_obj.get("RS_ID").getAsString();
                //if (rs_id_to_disease_obj.has(rs_id)) {
                        JsonObject disease_obj = elem2.getAsJsonObject();
                        if(!disease_obj.has("detailPageVariant"))continue;
                        if(!rs_id.equals(disease_obj.get("detailPageVariant").getAsString())) continue;
                        JsonObject temp = new JsonObject();
                        temp.addProperty("RS_ID", rs_id);
                        if (genomic_obj.has("GENE_SYMBOL")) {
                            temp.add("GENE_SYMBOL", genomic_obj.get("GENE_SYMBOL"));
                        }
                        if (genomic_obj.has("GENOTYPE")) {
                            temp.addProperty("GENOTYPE", genomic_obj.get("GENOTYPE").getAsString().replaceAll("/",""));
                        }
                        if (disease_obj.has("source")) {
                            temp.add("source", disease_obj.get("source"));
                        }
                        if(disease_obj.has("clinicalSignificance"))
                        {
                            temp.add("clinicalSignificance", disease_obj.get("clinicalSignificance"));
                        }
                        if (disease_obj.has("name")) {
                            temp.add("name", disease_obj.get("name"));
                        }
                        if (disease_obj.has("id")) {
                            temp.add("disease_id", disease_obj.get("id"));
                        }
                        if (disease_obj.has("url")) {
                            temp.add("url", disease_obj.get("url"));
                        }
                        if(temp.has("clinicalSignificance"))
                        {
                            total_disease_records.add(temp);
                        }
                        else
                        {
                            empty.add(temp);
                        }
                    }
                //}
            }
        }
        total_disease_records.addAll(empty);
        result = generatePagingResult(JsonAttrUtil.toJsonTree(total_disease_records).getAsJsonArray(), current_page, page_size);
        return ResponseMsgFactory.buildResponseStr(result);
    }

    protected JsonObject getRequire(TreeSet<JsonElement> genomicsSet) {
        TreeSet<String> rsid_array = new TreeSet<>();
        for (JsonElement elem : genomicsSet) {
            JsonObject genomic_obj = elem.getAsJsonObject();
            if (genomic_obj.has("VARIATION_SOURCE")
                    && genomic_obj.get("VARIATION_SOURCE").getAsString().equalsIgnoreCase("GERMLINE")
                    && genomic_obj.has("RS_ID")) {
                rsid_array.add(genomic_obj.get("RS_ID").getAsString());
            }
        }

        JsonObject request_json_obj = new JsonObject();
        request_json_obj.addProperty("from", "variationArray");
        request_json_obj.addProperty("to", "disease");
        request_json_obj.addProperty("currentPage", 1);
        request_json_obj.addProperty("pageSize", 99999);
        request_json_obj.add("query", JsonAttrUtil.toJsonTree(rsid_array));
        return request_json_obj;
    }


}
