package com.gennlife.fs.system.bean;

/**
 * Created by wangyiyan on 2019/4/8
 */
public class SampleBean {
    private int key;
    private String PATIENT_SN = "";
    private String GENDER = "";
    private String BIRTH_DATE = "";
    private String ETHNIC = "";
    private String MARITAL_STATUS = "";
    private String SPECIMEN_TYPE = "";
    private String SAMPLING_TIME = "";
    private String bbh = "";

    public SampleBean() {
    }

    public SampleBean(String bbh) {
        this.bbh = bbh;
    }


    public String getBbh() {

        return bbh;
    }

    public void setBbh(String bbh) {

        this.bbh = bbh;
    }

    public String getPATIENT_SN() {
        return PATIENT_SN;
    }

    public void setPATIENT_SN(String PATIENT_SN) {
        this.PATIENT_SN = PATIENT_SN;
    }

    public String getGENDER() {
        return GENDER;
    }

    public void setGENDER(String GENDER) {
        this.GENDER = GENDER;
    }

    public String getBIRTH_DATE() {
        return BIRTH_DATE;
    }

    public void setBIRTH_DATE(String BIRTH_DATE) {
        this.BIRTH_DATE = BIRTH_DATE;
    }

    public String getETHNIC() {
        return ETHNIC;
    }

    public void setETHNIC(String ETHNIC) {
        this.ETHNIC = ETHNIC;
    }

    public String getMARITAL_STATUS() {
        return MARITAL_STATUS;
    }

    public void setMARITAL_STATUS(String MARITAL_STATUS) {
        this.MARITAL_STATUS = MARITAL_STATUS;
    }

    public String getSPECIMEN_TYPE() {
        return SPECIMEN_TYPE;
    }

    public void setSPECIMEN_TYPE(String SPECIMEN_TYPE) {
        this.SPECIMEN_TYPE = SPECIMEN_TYPE;
    }

    public String getSAMPLING_TIME() {
        return SAMPLING_TIME;
    }

    public void setSAMPLING_TIME(String SAMPLING_TIME) {
        this.SAMPLING_TIME = SAMPLING_TIME;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }
}



