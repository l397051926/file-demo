package com.gennlife.fs.system.bean;

import com.gennlife.fs.common.utils.StringUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Created by Chenjinfeng on 2017/4/5.
 */
@Component
@Scope("singleton")
@ConfigurationProperties(prefix = "compatibleconfig")
public class Compatible {
    private String operation;
    /**
     *
     *  value: sql 从数据库拿数据的url
     *         空 从mogodb 获取
     *   图片获取功能
     * */
    private String imageGetFun;

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }

    public void setImageGetFun(String imageGetFun) {
        this.imageGetFun = imageGetFun;
    }

    public String getImageGetFun() {
        return imageGetFun;
    }
    public boolean imageGetFunIsSql()
    {
        return "sql".equalsIgnoreCase(imageGetFun);
    }

    public boolean isDefaultImageGetFun() {
        return StringUtil.isEmptyStr(imageGetFun);
    }
}
