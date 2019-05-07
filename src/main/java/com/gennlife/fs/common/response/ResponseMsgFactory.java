package com.gennlife.fs.common.response;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Chenjinfeng on 2016/9/30.
 */
public class ResponseMsgFactory{

    public static final String RESPONSE_ERROR_KEY="RESPONSE_ERROR";
    public static final String SUCCESS_FLAG="success";
    public static final String MISSPARAMETER="missing parameter";
    private static final Logger logger= LoggerFactory.getLogger(ResponseMsgFactory.class);
    public static String buildResponseStr(JsonObject data)
    {
        return buildResponseStr(data,"no data ");
    }
    public static String buildResponseStr(JsonObject data, String msg)
    {
        if(data==null) return buildFailStr(msg);
        else if(data.has(RESPONSE_ERROR_KEY))
        {
            data.addProperty(SUCCESS_FLAG,false);
            return JsonAttrUtil.toJsonStr(data);
        }
        else if(data.has(SUCCESS_FLAG))
            return JsonAttrUtil.toJsonStr(data);
        else return buildSuccessStr(data);
    }
    public static String buildSuccessStr(JsonObject data)
    {
        data.addProperty(SUCCESS_FLAG,true);
        return JsonAttrUtil.toJsonStr(data);
    }
    public static String buildFailStr(JsonObject addition, String error)
    {
        addition.addProperty(RESPONSE_ERROR_KEY,error);
        return JsonAttrUtil.toJsonStr(addition);
    }
    public static JsonObject buildFailJson(String error)
    {
        JsonObject result = new JsonObject();
        result.addProperty(RESPONSE_ERROR_KEY,error);
        result.addProperty(SUCCESS_FLAG,false);
        return result;
    }
    public static JsonObject MissingParameterJson()
    {
        return buildFailJson(MISSPARAMETER);
    }
    public static String MissingParameterStr()
    {
        return buildFailStr(MISSPARAMETER);
    }
    public static JsonObject buildSystemErrorJson(String error)
    {
        JsonObject result = new JsonObject();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String prefix = "SYS ERROR "+sdf.format(new Date(System.currentTimeMillis()))+":";
        result.addProperty(RESPONSE_ERROR_KEY,prefix+error);
        return result;
    }
    public static String buildFailStr()
    {
        JsonStrBuild jsonStrBuild=new JsonStrBuild();
        jsonStrBuild.beginObject();
        jsonStrBuild.add(SUCCESS_FLAG,false);
        jsonStrBuild.endObject();
        return jsonStrBuild.getJson();
    }
    public static String buildDeprecatedStr()
    {
        return buildFailStr("接口废弃");
    }
    public static String buildFailStr(String msg)
    {
        JsonStrBuild jsonStrBuild=new JsonStrBuild();
        jsonStrBuild.beginObject();
        jsonStrBuild.add(SUCCESS_FLAG,false);
        jsonStrBuild.add(RESPONSE_ERROR_KEY,msg);
        jsonStrBuild.endObject();
        return jsonStrBuild.getJson();
    }

    public static String getResponseStr(ResponseInterface template, String param)
    {
        return getResponseStr(template, JsonAttrUtil.toJsonObject(param));
    }
    public static String getResponseStr(ResponseInterface template, JsonObject paramJson)
    {
        if(paramJson==null)return buildFailStr("参数不是json");
        template.execute(JsonAttrUtil.toJsonObject(paramJson));
        return buildResponseStr(template.get_result(),template.get_error());
    }
    public static JsonObject getResponseJson(ResponseInterface template, String param)
    {
        template.execute(JsonAttrUtil.toJsonObject(param));
        return buildResponseJson(template.get_result(),template.get_error());
    }
    public static JsonObject getResponseJson(ResponseInterface template, JsonObject paramJson)
    {
        template.execute(paramJson);
        return buildResponseJson(template.get_result(),template.get_error());
    }

    public static JsonObject buildResponseJson(JsonObject data, String msg) {
        {
            if(data==null) return buildFailJson(msg);
            else if(data.has(RESPONSE_ERROR_KEY))
            {
                data.addProperty(SUCCESS_FLAG,false);
                return data;
            }
            else
                data.addProperty(SUCCESS_FLAG,true);
            return data;
        }
    }
    public static boolean isSuccess(JsonObject data)
    {
        if(data==null)return false;
        if(data.has(SUCCESS_FLAG))
        return data.get(SUCCESS_FLAG).getAsBoolean();
        else return !data.has(RESPONSE_ERROR_KEY);
    }
    public static boolean hasSuccesssFlag(JsonObject data)
    {
        if(data==null)return false;
        return data.has(SUCCESS_FLAG);
    }
    public static JsonObject buildSuccessJson(JsonObject result) {
        result.addProperty(SUCCESS_FLAG,true);
        return result;
    }
    public static JsonObject paramChecks(JsonObject json,String[] keys)
    {
        if(keys==null) return null;
        for(String key:keys)
            if(!JsonAttrUtil.has_key(json,key))
                return buildFailJson("missing param "+key);
            else if(StringUtil.isEmptyStr(json.get(key).getAsString()))
                return buildFailJson("Empty param "+key);
        return null;
    }
    public static void responseStream(InputStream inputStream,HttpServletResponse response)
    {
        InputStream in = null;
        OutputStream out = null;
        try {

            in = new BufferedInputStream(inputStream);
            out = new BufferedOutputStream(response.getOutputStream());
            byte[] buffer = new byte[1024];
            while (in.read(buffer) != -1) {
                out.write(buffer);
                buffer = new byte[1024];
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e) {

            }
        }
    }
}
