package com.gennlife.fs.system.config;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.SystemUtil;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

/**
 * Created by Chenjinfeng on 2016/11/10.
 */

/**
 * 基因词典
 * */
public class GenDictConfig {
    private  static  HashSet<String> gen_dict = new HashSet<String>();
    public static HashSet<String> get_GenDict() {
        return gen_dict;
    }
    public static synchronized boolean init(){
        Logger logger = LoggerFactory.getLogger(GenDictConfig.class);
        //基因词典
        logger.info("gene init start");
        gen_dict.clear();
        try {
            String jsonfile = SystemUtil.getPath(BeansContextUtil.getDataBean().getGene());
            logger.info("intdict() for gene file....." + jsonfile);
            JsonObject obj = JsonAttrUtil.getJsonObjectfromFile(jsonfile);
            for (JsonElement item : obj.get("records").getAsJsonArray()) {
                gen_dict.add(item.getAsString());
            }
            logger.info("基因数据 "+ JsonAttrUtil.toJsonTree(gen_dict));
        }
        catch (Exception e)
        {
            gen_dict.clear();
            logger.error("gene Exception ",e);
        }

        if(gen_dict==null || gen_dict.size()==0) {
            logger.error("基因词典配置失败");
            return false;
        }
        else {
            logger.info("基因词典配置成功");
            return true;
        }
    }
}
