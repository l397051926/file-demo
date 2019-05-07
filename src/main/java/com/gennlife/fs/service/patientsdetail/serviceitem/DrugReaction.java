package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Helios on 2016/7/18.
 * 基因型-药物反应（组学数据）
 */
public class DrugReaction extends PatientDetailService {
	private static final Logger logger = LoggerFactory.getLogger(DrugReaction.class);
    public String getDrugReaction(String param) {
    	logger.info("getDrugReaction(): REQUEST genetic disease by variation id..." + param);
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
        result.add("drugReaction", new JsonArray());
        QueryParam qp=new QueryParam(param_json,patient_sn,"germline.detection_result");
        JsonObject searchresult=get_data(qp);
        JsonArray genomics = JsonAttrUtil.getJsonArrayValueMutilSource(new String[]{"germline"},searchresult);
        searchresult=null;
        if(genomics==null || genomics.size()==0)
        {
            return ResponseMsgFactory.buildFailStr("no data");
        }
        JsonObject genomics_obj = genomics.get(0).getAsJsonObject();
        if(genomics_obj.has("detection_result")) {
        	genomics = genomics_obj.getAsJsonArray("detection_result");
        } else {
        	return ResponseMsgFactory.buildFailStr("no detection result");

        }
        
        JsonArray request_json = new JsonArray(); 
        Map<String, String> gene_symbol_map = new HashMap();
        
        //构造请求        
        for (JsonElement elem : genomics) {
            JsonObject drugReaction = elem.getAsJsonObject();
            JsonObject data = new JsonObject();
            if (drugReaction.has("VARIATION_SOURCE") 
            		&& drugReaction.get("VARIATION_SOURCE").getAsString().equalsIgnoreCase("GERMLINE")
            		&& drugReaction.has("RS_ID")
            		&& drugReaction.has("GENOTYPE")) {
                data.addProperty("RS_ID", drugReaction.get("RS_ID").getAsString());                
                JsonObject request_json_obj = new JsonObject();                
                request_json_obj.addProperty("rs_id", drugReaction.get("RS_ID").getAsString());
                String genotype = drugReaction.get("GENOTYPE").getAsString();
                String gene_symbol = null;
                
                String genotype_lib = filterSlash(genotype);
                
                request_json_obj.addProperty("genotype", genotype_lib);
                request_json.add(request_json_obj);
                
                if(drugReaction.has("GENE_SYMBOL")) {
                	gene_symbol = drugReaction.get("GENE_SYMBOL").getAsString();
                } else {
                	gene_symbol = "-";
                }                
                gene_symbol_map.put(drugReaction.get("RS_ID").getAsString() + "_" + genotype_lib, gene_symbol);
            }
        }
       
        //查询知识库
        logger.info("getDrugReaction(): request ..." + request_json.toString());
        JsonObject response_json = JsonAttrUtil.toJsonObject(HttpRequestUtils.httpJsonPost(knowledge_service_uri + "/knowledge/PharmGKBSearchDrug",
        												request_json.toString()));
        if(response_json==null) { // == null) {
        	return ResponseMsgFactory.buildFailStr(" knowledge no data");
        }
        logger.debug("getDrugReaction(): get response ..." + response_json);

        JsonArray drug_records = new JsonArray();
        if(response_json!=null&&response_json.has("data")) {
             for (JsonElement elem1: response_json.getAsJsonArray("data")) {
            	 JsonObject drug_info = elem1.getAsJsonObject();
                 if (drug_info.has("rs_id") && drug_info.has("genotype") && drug_info.has("drug")) {
                        for (JsonElement elem2: drug_info.getAsJsonArray("drug")) {
                            JsonObject drug_item = elem2.getAsJsonObject();
                            JsonObject temp = new JsonObject();
                            String rs_id = drug_info.get("rs_id").getAsString();
                            String genotype = drug_info.get("genotype").getAsString();
                            temp.addProperty("rs_id", rs_id);
                            temp.addProperty("gene_symbol", gene_symbol_map.get(rs_id + "_" + genotype));
                            temp.addProperty("genotype", genotype);
                            temp.add("drug", drug_item.get("drug"));
                            temp.add("annotation", drug_item.get("annotation"));
                            temp.add("source", drug_item.get("source"));
                            temp.add("url", drug_item.get("url"));
                            drug_records.add(temp);
                        }
                }
            }
        } 
        result = generatePagingResult(drug_records, current_page, page_size);
        return ResponseMsgFactory.buildSuccessStr(result);
    }
    
    public String testDrugReaction(String param) {
    	logger.info("getDrugReaction(): REQUEST genetic disease by variation id..." + param);
        
        JsonObject result = new JsonObject();
                
        JsonArray request_json =  new JsonParser().parse(param).getAsJsonArray();
        JsonArray request = new JsonArray();        
        
        for(JsonElement elem : request_json) {
        	JsonObject request_obj = elem.getAsJsonObject();
        	String genotype_type = request_obj.get("genotype").getAsString();
        	String genotype = filterSlash(genotype_type);
        	JsonObject request_out_obj = new JsonObject();
        	request_out_obj.addProperty("rs_id", request_obj.get("rs_id").getAsString());
        	request_out_obj.addProperty("genotype", genotype);
        	request.add(request_out_obj);
        }
        //查询知识库
        logger.debug("getDrugReaction(): request ..." + request.toString());
        JsonObject response_json = JsonAttrUtil.toJsonObject(HttpRequestUtils.httpJsonPost(knowledge_service_uri + "/knowledge/PharmGKBSearchDrug",
        												request.toString()));
        if(response_json == null) {
        	return ResponseMsgFactory.buildFailStr(" no knowledge result");
        }
        logger.debug("getDrugReaction(): get response ..." + response_json);
        JsonArray drug_records = new JsonArray();
        if(response_json.has("data")) {
             for (JsonElement elem1: response_json.getAsJsonArray("data")) {
            	 JsonObject drug_info = elem1.getAsJsonObject();
                 if (drug_info.has("rs_id") && drug_info.has("genotype") && drug_info.has("drug")) {
                        for (JsonElement elem2: drug_info.getAsJsonArray("drug")) {
                            JsonObject drug_item = elem2.getAsJsonObject();
                            JsonObject temp = new JsonObject();
                            String rs_id = drug_info.get("rs_id").getAsString();
                            String genotype = drug_info.get("genotype").getAsString();
                            temp.addProperty("rs_id", rs_id);                            
                            temp.addProperty("genotype", genotype);
                            temp.add("drug", drug_item.get("drug"));
                            temp.add("annotation", drug_item.get("annotation"));
                            temp.add("source", drug_item.get("source"));
                            temp.add("url", drug_item.get("url"));
                            drug_records.add(temp);
                        }
                }
            }
        } 
        result = generatePagingResult(drug_records, 0, 1000000);
        return result.toString();
    }
    
    private static String filterSlash(String input) {
    	String output = "";
    	String [] input_arr = input.split("/");
    	for(String element  :  input_arr) {
    		output = output + element;
    	} 
    	
    	return output;
    }
}
