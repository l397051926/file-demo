package com.gennlife.fs.service.patientsdetail.model;

import com.google.gson.JsonArray;

/**
 * Created by zhangshijian on 2016/7/14.
 */
public class DiagnosisSub {
    private JsonArray disease_normal_name;
    private JsonArray disease_normal_code;
    private String disease_code = null;
    private String disease_name = null;
    private String diagnostic_dept_name = null;
    

    public DiagnosisSub(Visit vist){
        super();
        this.disease_code = vist.get_disease_code();
        this.disease_name = vist.get_disease_name();
        this.diagnostic_dept_name = vist.get_visit_dept();
        this.disease_normal_code=vist.get_disease_normal_code();
        this.disease_normal_name=vist.get_disease_normal_name();
    }
    
    public String getKey() {
    	String code = null;
    	String name = null;
    	String dept = null;
    	if(this.disease_code == null) {
    		code = "";
    	}
    	if(this.disease_name == null) {
    		name = "";
    	}
    	if(this.diagnostic_dept_name == null) {
    		dept = "";
    	}
    		
    	return code + name + dept;
    }

}
