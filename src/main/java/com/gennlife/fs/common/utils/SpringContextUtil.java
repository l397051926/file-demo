
package com.gennlife.fs.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * Created by Chenjinfeng on 2017/1/4.
 */
@Service
public class SpringContextUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext = null;
    public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    public final static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    public final static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }


    public final static boolean containsBean(String beanName) {
        return applicationContext.containsBean(beanName);
    }
    public final static <T> T getBeanByDefaultNull(String beanName, Class<T> clz) {
        try {
            return (T) applicationContext.getBean(beanName);
        } catch (Exception e) {
            return null;
        }
    }
}
