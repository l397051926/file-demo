package com.gennlife.fs.system.bean;

/**
 * Created by wangyiyan on 2019/4/9
 */
public class SampleResult {
    private int code;
    private String taskId;
    private int totalPatSn;
    private int totalBBH;
    private Object data;

    public SampleResult(int code, String taskId, int totalPatSn, int totalBBH, Object data){
        this.code = code;
        this.taskId = taskId;
        this.totalPatSn = totalPatSn;
        this.totalBBH = totalBBH;
        this.data = data;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public int getTotalPatSn() {
        return totalPatSn;
    }

    public void setTotalPatSn(int totalPatSn) {
        this.totalPatSn = totalPatSn;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getTotalBBH() {
        return totalBBH;
    }

    public void setTotalBBH(int totalBBH) {
        this.totalBBH = totalBBH;
    }
}
