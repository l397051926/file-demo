package com.gennlife.fs.system.listen;

import com.gennlife.fs.common.cache.MemLocalcacheService;
import com.gennlife.fs.common.utils.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Created by Chenjinfeng on 2016/10/29.
 */
@WebListener
public class ServletContextListen implements ServletContextListener {
    Logger logger = LoggerFactory.getLogger(ServletContextListen.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            logger.info("free memory " + NumberUtil.countSize(Runtime.getRuntime().freeMemory()));
            MemLocalcacheService.getMemcacheObj().start();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("start error", e);
            throw new RuntimeException();
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        logger.info("MemLocalcacheService stop");
        MemLocalcacheService.getMemcacheObj().stopService();
    }
}