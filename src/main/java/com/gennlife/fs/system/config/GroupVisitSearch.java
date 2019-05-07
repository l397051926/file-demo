package com.gennlife.fs.system.config;

import com.alibaba.fastjson.JSONArray;
import com.google.gson.JsonArray;

/**
 * @author liumingxin
 * @create 2018 13 18:28
 * @desc
 **/
public class GroupVisitSearch {

    private static final String VISITS="visits";

    private String indexName;
    private String patientSn;
    private String visitSn;
    private JSONArray source;

    public GroupVisitSearch() {
    }

    public GroupVisitSearch(String indexName,String patientSn, String visitSn) {
        this.indexName = indexName;
        this.patientSn = patientSn;
        this.visitSn = visitSn;
        source = new JSONArray();
    }

    public GroupVisitSearch(String patientSn, String visitSn, JSONArray source) {
        this.patientSn = patientSn;
        this.visitSn = visitSn;
        this.source = source;
    }

    public String getPatientSn() {
        return patientSn;
    }

    public void setPatientSn(String patientSn) {
        this.patientSn = patientSn;
    }

    public String getVisitSn() {
        return visitSn;
    }

    public void setVisitSn(String visitSn) {
        this.visitSn = visitSn;
    }

    public JSONArray getSource() {
        return source;
    }

    public void setSource(JSONArray source) {
        this.source = source;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public void addSource(String str){
        this.source.add(str);
    }

    public void addSource(JsonArray arr){
        int size = arr == null ? 0 : arr.size();
        for (int i = 0; i < size; i++) {
            String tmp  = arr.get(i).getAsString();
            if(!tmp.startsWith(VISITS)){
                source.add(VISITS+"."+tmp);
            }
        }
    }
    public void addSource(String[] args){
        for (int i = 0; i < args.length; i++) {
            String tmp = args[i];
            if(!tmp.startsWith(VISITS)){
                source.add(VISITS+"."+tmp);
            }
        }
    }

}
