package com.gennlife.fs.system.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Created by Chenjinfeng on 2017/7/26.
 */
@Component
@Scope("singleton")
@ConfigurationProperties(prefix = "smbconfig")
public class SmbConfig {
    private static final Logger logger= LoggerFactory.getLogger(SmbConfig.class);
    private String userName;
    private String passwd;
    private String localpath;

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setLocalpath(String localpath) {
        logger.info("Localpath refresh "+localpath);
        this.localpath = localpath;
    }

    public String getLocalpath() {
        return localpath;
    }
}
