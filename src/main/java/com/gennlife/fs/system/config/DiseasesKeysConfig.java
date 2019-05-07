package com.gennlife.fs.system.config;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.SystemUtil;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Chenjinfeng on 2016/12/5.
 */
public class DiseasesKeysConfig {
    private static final ArrayList<String> diseases_en=new ArrayList<>();
    private static final ArrayList<String> diseases_cn=new ArrayList<>();
    private static final HashMap<String,String> section=new HashMap<>();
    public synchronized static  boolean init()
    {
        diseases_cn.clear();
        diseases_en.clear();
        Logger logger= LoggerFactory.getLogger(DiseasesKeysConfig.class);
        String file= SystemUtil.getPath(BeansContextUtil.getDataBean().getDiseaseKeys());
        logger.info("Diseasekeys file ="+file);
        JsonObject data= JsonAttrUtil.getJsonObjectfromFile(file);
        try{
           for(JsonElement key:data.getAsJsonArray("keys"))
           {
               JsonObject tmp=key.getAsJsonObject();
               diseases_cn.add(tmp.get("CN").getAsString());
               diseases_en.add(tmp.get("EN").getAsString());
               section.put(tmp.get("EN").getAsString(),tmp.get("section").getAsString());
           }

        }
        catch (Exception e)
        {
            diseases_cn.clear();
            diseases_en.clear();
            logger.error("Diseasekeys error ",e);
            return false;
        }
        logger.info("Diseasekeys success "+data.toString());
        return true;
    }
    public static List<String> getKeys()
    {
        return diseases_en;
    }
    public static String getSection(String en)
    {
        return section.get(en);
    }
}
