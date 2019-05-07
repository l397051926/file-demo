package com.gennlife.fs.common.utils;

/**
 * Created by Chenjinfeng on 2016/12/5.
 */
public class SystemUtil {
    public static String getPath(String filename)
    {
        return SystemUtil.class.getClassLoader().getResource("").getPath()+filename;
    }

}
