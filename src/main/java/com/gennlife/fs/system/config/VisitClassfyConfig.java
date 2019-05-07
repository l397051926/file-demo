package com.gennlife.fs.system.config;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.common.utils.SystemUtil;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by Chenjinfeng on 2016/12/5.
 */
public class VisitClassfyConfig {
    public static JsonObject visitsclassfy;
    public synchronized static  boolean init()
    {
        Logger logger= LoggerFactory.getLogger(VisitClassfyConfig.class);
       String file= SystemUtil.getPath(BeansContextUtil.getDataBean().getVisitClassfy());
        logger.info("visitclassfy file "+file);
        visitsclassfy= JsonAttrUtil.getJsonObjectfromFile(file);
        if(visitsclassfy==null){
            logger.error("visitsclassfy error");
            return false;
        }
        logger.info("visitsclassfy success");
        return true;
    }
    public static boolean isIn(String key)
    {
        if(StringUtil.isEmptyStr(key)) return false;
        return visitsclassfy.has(key);
    }
    public static JsonArray getData(String param)
    {
        String key=null;
        if(param==null) key="";
        else key=param;
        key= key.toUpperCase();
        if(visitsclassfy!=null)
        {
            if(key.equals("")||!visitsclassfy.has(key)) {
                JsonArray array=new JsonArray();
                for(Map.Entry<String, JsonElement> item: visitsclassfy.entrySet())
                {
                    if(item.getValue().isJsonArray())
                        array.addAll(item.getValue().getAsJsonArray());
                }
                return array;
            }
            return visitsclassfy.getAsJsonArray(key);
        }
        return null;
    }
}
