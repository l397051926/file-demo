package com.gennlife.fs.service.patientsdetail.model;

import com.google.gson.JsonObject;

import java.util.Comparator;

/**
 * Created by Chenjinfeng on 2016/12/13.
 */
public class GeneCompator implements Comparator<JsonObject> {
    @Override
    public int compare(JsonObject o1, JsonObject o2) {

        return buildKey(o1).compareTo(buildKey(o2));
    }
    private String buildKey(JsonObject json)
    {
        String result="";
        if(json.has("RS_ID")) result+=json.get("RS_ID");
        if(json.has("DNA_CHANGE")) result+=json.get("DNA_CHANGE");
        if(json.has("GENE_LOCATION")) result+=json.get("RS_ID");
        if(json.has("GENE_SYMBOL")) result+=json.get("GENE_SYMBOL");
        if(json.has("VARTITION_TYPE")) result+=json.get("VARTITION_TYPE");
        if(json.has("CONSEQUENCES_TYPE")) result+=json.get("CONSEQUENCES_TYPE");
        return result;
    }
}
