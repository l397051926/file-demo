package com.gennlife.fs.service.patientsdetail.serviceitem;

/**
 * Created by Chenjinfeng on 2018/3/20.
 */
public class TriListIdMapEntity implements Comparable<TriListIdMapEntity> {
    private String cn;
    private String en;
    private String unit;
    private Double max_value;
    private Double min_value;

    public TriListIdMapEntity(String cn, String en) {
        this.cn = cn;
        this.en = en;
    }

    public TriListIdMapEntity(String cn, String en, String unit, Double min_value, Double max_value) {
        this.cn = cn;
        this.en = en;
        this.unit = unit;
        this.max_value = max_value;
        this.min_value = min_value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getMax_value() {
        return max_value;
    }

    public void setMax_value(Double max_value) {
        this.max_value = max_value;
    }

    public Double getMin_value() {
        return min_value;
    }

    public void setMin_value(Double min_value) {
        this.min_value = min_value;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getEn() {
        return en;
    }

    public void setEn(String en) {
        this.en = en;
    }

    @Override
    public int compareTo(TriListIdMapEntity o) {
        return en.compareTo(o.en);
    }
}
