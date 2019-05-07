package com.gennlife.fs.system.bean;

import com.gennlife.fs.common.utils.SpringContextUtil;

/**
 * Created by chen-song on 16/6/18.
 */
public class BeansContextUtil extends SpringContextUtil {

    public static DataBean getDataBean() {
        return (DataBean) getApplicationContext().getBean("DataBean");
    }

    public static UrlBean getUrlBean() {
        return (UrlBean) getApplicationContext().getBean("urlBean");
    }

    public static Compatible getCompatible() {
        return getBeanByDefaultNull("compatible", Compatible.class);
    }

}
