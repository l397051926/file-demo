package com.gennlife.fs.common.response;

/**
 * Created by Chenjinfeng on 2017/2/7.
 */
public class JsonStrBuild {
    private StringBuffer buffer=new StringBuffer();
    private boolean addValue=false;
    public void beginObject()
    {
        buffer.append("{");
        addValue=false;
    }
    public void endObject()
    {
        buffer.append("}");
        addValue=false;
    }
    public void add(String key,String value)
    {
        if(addValue)buffer.append(",");
        buffer.append("\"").append(key).append("\":").append("\"").append(value).append("\"");
        addValue=true;
    }
    public void add(String key,Number value)
    {
        if(addValue)buffer.append(",");
        buffer.append("\"").append(key).append("\":").append(value);
        addValue=true;
    }
    public void add(String key,Boolean value)
    {
        if(addValue)buffer.append(",");
        buffer.append("\"").append(key).append("\":").append(value);
        addValue=true;
    }
    public String getJson()
    {
        return buffer.toString();
    }
}
