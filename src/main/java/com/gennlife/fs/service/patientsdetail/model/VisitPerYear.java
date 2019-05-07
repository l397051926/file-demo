package com.gennlife.fs.service.patientsdetail.model;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class VisitPerYear {
    private String visit_year;
    private LinkedList<Visit> visits;

    public VisitPerYear(String year) {
        super();
        this.visit_year = year;
        this.visits = new LinkedList<Visit>();
    }

    public void addVisit(Visit visit) {
        visits.add(visit);
    }

    public LinkedList<Visit> getVisit() {
        return visits;
    }

    public JsonObject getJson(boolean isDesc) {

        ArrayList<JsonObject> list = new ArrayList<>(visits.size());
        JsonObject result = new JsonObject();
        Collections.sort(visits);
        if (isDesc)
            Collections.reverse(visits);
        for (Visit visit : visits) {
            JsonObject visititem = new JsonObject();
            //visititem.addProperty("patient_sn",visit.get_patient_sn());
            visititem.addProperty("visit_sn", visit.get_visit_sn());
            visititem.addProperty("visit_date", visit.get_visit_date());
            visititem.addProperty("visit_type", visit.get_visit_type());
            visititem.addProperty("visit_dept", visit.get_visit_dept());
            visititem.addProperty("diagnosis_department", visit.get_visit_dept());//visit.get_diagnosis_department());
            visititem.addProperty("main_diagnosis_flag", visit.get_main_diagnosis_flag());
            visititem.addProperty("disease_name", visit.get_disease_name());
            visititem.addProperty("diagnosis_type_name", visit.get_diagnosis_type_name());
            visititem.addProperty("diagnosis_date", visit.get_diagnosis_date());
            visititem.addProperty("disease_code", visit.get_disease_code());
            visititem.add("event", JsonAttrUtil.toJsonTree(visit.get_visit_event()));
            list.add(visititem);
        }
        result.addProperty("visit_year", visit_year);
        result.add("visits", JsonAttrUtil.toJsonTree(list));
        return result;
    }

    public boolean isEmpty() {
        return visits == null || visits.size() == 0;
    }

    public int size() {
        if (isEmpty()) return 0;
        return visits.size();
    }
}

