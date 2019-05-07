package com.gennlife.fs.system.config;

/**
 * @author liumingxin
 * @create 2018 13 15:59
 * @desc
 **/
public class StatisticsSearch {
    private static final String STATISTICS="statistics";
    private static final String TIME_LINE_ASC ="timeLineASC";
    private static final String TIME_LINE_DESC ="timeLineDesc";

    private String patient_sn ;
    private String source;

    public StatisticsSearch() {
    }

    public StatisticsSearch(String patient_sn) {
        this.patient_sn = patient_sn;
        this.source = STATISTICS;
    }

    public StatisticsSearch(String patient_sn, boolean isDesc) {
        this.patient_sn = patient_sn;
        this.source = isDesc ? TIME_LINE_DESC : TIME_LINE_ASC;
    }

    public String getPatient_sn() {
        return patient_sn;
    }

    public void setPatient_sn(String patient_sn) {
        this.patient_sn = patient_sn;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
