package com.gennlife.fs.system.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Created by Chenjinfeng on 2016/12/15.
 */
@Configuration
public class TimeConfig implements FactoryBean<TimeConfig>, InitializingBean {
    private int CACHESIMILAREXPIRETIME;
    private int CACHESIMILARPAGESIZE;
    private int CACHEPRIMARYEXPORETIME;
    private int HTTPTIMEOUT;
    private int HTTPSLOWTIME;
    private static TimeConfig config;
    @Primary
    @Bean
    @ConfigurationProperties(prefix = "timeconfig")
    public TimeConfig createConfig() {
        config = new TimeConfig();
        return config;
    }

    public static int get_similar_expiretime() {
        return config.CACHESIMILAREXPIRETIME;
    }

    public static int get_similar_page_size() {
        return config.CACHESIMILARPAGESIZE;
    }

    public static int get_primary_exporetime() {
        return config.CACHEPRIMARYEXPORETIME;
    }

    public static int getHTTPTIMEOUT() {
        try {
            return config.HTTPTIMEOUT;
        }catch (Exception e)
        {
            return 60000;
        }
    }

    public static int getHTTPSLOWTIME() {
        return config.HTTPSLOWTIME;
    }

    @Override
    public TimeConfig getObject() throws Exception {
        return this;
    }

    @Override
    public Class<?> getObjectType() {
        return TimeConfig.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (CACHESIMILARPAGESIZE <= 50) CACHESIMILARPAGESIZE = 50;
        if (CACHESIMILAREXPIRETIME <= 60) CACHESIMILAREXPIRETIME = 60;
        if (CACHEPRIMARYEXPORETIME <= 60) CACHEPRIMARYEXPORETIME = 60;
    }

    public void setCACHESIMILAREXPIRETIME(int CACHESIMILAREXPIRETIME) {
        this.CACHESIMILAREXPIRETIME = CACHESIMILAREXPIRETIME;
    }

    public void setCACHESIMILARPAGESIZE(int CACHESIMILARPAGESIZE) {
        this.CACHESIMILARPAGESIZE = CACHESIMILARPAGESIZE;
    }

    public void setCACHEPRIMARYEXPORETIME(int CACHEPRIMARYEXPORETIME) {
        this.CACHEPRIMARYEXPORETIME = CACHEPRIMARYEXPORETIME;
    }

    public void setHTTPTIMEOUT(int HTTPTIMEOUT) {
        this.HTTPTIMEOUT = HTTPTIMEOUT;
    }

    public void setHTTPSLOWTIME(int HTTPSLOWTIME) {
        this.HTTPSLOWTIME = HTTPSLOWTIME;
    }
}
