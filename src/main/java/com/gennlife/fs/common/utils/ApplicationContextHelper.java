package com.gennlife.fs.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextHelper implements ApplicationContextAware {

    private static ApplicationContext _context;

    @Autowired
    @Primary
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHelper._context = applicationContext;
    }

    public static ApplicationContext applicationContext() {
        return _context;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException {
        return (T)_context.getBean(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return _context.getBean(name, requiredType);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> requiredType) throws BeansException {
        return _context.getBean(requiredType);
    }

}
