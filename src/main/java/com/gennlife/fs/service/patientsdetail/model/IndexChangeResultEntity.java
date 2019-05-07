package com.gennlife.fs.service.patientsdetail.model;

import com.gennlife.fs.common.utils.StringUtil;

import java.util.ArrayList;

/**
 * Created by Chenjinfeng on 2018/3/19.
 */
public class IndexChangeResultEntity {
    private int id;
    private String name;
    private String unit;
    private ArrayList<String> time;
    private ArrayList<Double> value;
    private Double interval_max;
    private Double interval_min;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public ArrayList<String> getTime() {
        return time;
    }

    public void setTime(ArrayList<String> time) {
        this.time = time;
    }

    public ArrayList<Double> getValue() {
        return value;
    }

    public void setValue(ArrayList<Double> value) {
        this.value = value;
    }

    public Double getInterval_max() {
        return interval_max;
    }


    public Double getInterval_min() {
        return interval_min;
    }

    public void setInterval_min(Double interval_min) {
        this.interval_min = interval_min;
    }

    public void setInterval_max(Double interval_max) {
        this.interval_max = interval_max;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void sub(String start, String end) {
        if (StringUtil.isEmptyStr(start) && StringUtil.isEmptyStr(end)) return;
        int begin = 0;
        int last = time.size();
        if (!StringUtil.isEmptyStr(start)) {
            for (int i = 0; i < time.size(); i++) {
                if (time.get(i).compareTo(start) >= 0)
                {
                    begin=i;
                    break;
                }
            }
        }
        if (!StringUtil.isEmptyStr(end)) {
            for (int i = begin; i < time.size(); i++) {
                if (time.get(i).compareTo(end) > 0) {
                    last = i;
                    break;
                }

            }
        }
        if(begin==last) {
            time.clear();
            value.clear();
            return;
        }
        time = new ArrayList<>(time.subList(begin, last));
        value = new ArrayList<>(value.subList(begin, last));
    }
}
