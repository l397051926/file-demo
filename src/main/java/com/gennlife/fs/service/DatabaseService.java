package com.gennlife.fs.service;

import com.gennlife.fs.common.configurations.GeneralConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Objects;

import static com.gennlife.fs.common.utils.DBUtils.P;
import static com.gennlife.fs.common.utils.DBUtils.Q;

@Service
public class DatabaseService implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);

    public JdbcTemplate jdbcTemplate() {
        return template;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        val s = new HikariDataSource();
        s.setJdbcUrl(cfg.databaseUrl);
        s.setUsername(cfg.databaseUsername);
        s.setPassword(cfg.databasePassword);
        s.setMaximumPoolSize(10);
        template = new JdbcTemplate(dataSource = s);
        val version = template.queryForObject("SELECT " + P(VERSION) + "FROM " + Q(cfg.databaseConstantsTable), Integer.class);
        if (!Objects.equals(cfg.databaseVersion, version)) {
            throw new Exception("Current database version: " + version + ", expecting " + cfg.databaseVersion);
        }
    }

    @PreDestroy
    public void destroy() {
        dataSource.close();
    }

    private HikariDataSource dataSource;
    private JdbcTemplate template;

    @Autowired
    private GeneralConfiguration cfg;

    private static final String ID = "ID";
    private static final String VERSION = "VERSION";

}
