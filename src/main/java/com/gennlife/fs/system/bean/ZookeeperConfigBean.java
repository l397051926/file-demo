/**
 * copyRight
 */
package com.gennlife.fs.system.bean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author liuzhen
 * Created by liuzhen.
 * Date: 2018/8/24
 * Time: 10:46
 */
@Component("zkConfig")
@Scope("singleton")
@ConfigurationProperties(prefix = "zk.connection")
public class ZookeeperConfigBean {
    private String zkUrls;
    private String zkPort;

    public String getZkUrls() {

        return zkUrls;
    }

    public void setZkUrls(String zkUrls) {

        this.zkUrls = zkUrls;
    }

    public String getZkPort() {

        return zkPort;
    }

    public void setZkPort(String zkPort) {

        this.zkPort = zkPort;
    }
}
