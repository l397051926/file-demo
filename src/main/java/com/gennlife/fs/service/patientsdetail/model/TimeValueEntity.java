package com.gennlife.fs.service.patientsdetail.model;

public class TimeValueEntity implements Comparable<TimeValueEntity> {
    private String time;
    private double value;

    public TimeValueEntity(String time, double value) {
        this.time = time;
        this.value = value;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public int compareTo(TimeValueEntity o) {
        return this.getTime().compareTo(o.getTime());
    }
}