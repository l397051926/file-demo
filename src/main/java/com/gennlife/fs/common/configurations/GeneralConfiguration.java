package com.gennlife.fs.common.configurations;

import com.gennlife.darren.util.ImmutableEndpoint;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

@Component
@PropertySource("classpath:general.properties")
public class GeneralConfiguration implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralConfiguration.class);

    @Value("#{T(com.gennlife.darren.util.Networking).tomcatEndpoint()}")
    public ImmutableEndpoint localEndpoint;

    @Value("${gennlife.fs.storage.root}")
    public Path storageRoot = null;

    @Value("${server.context-path}")
    public String applicationContextPath = null;

    @Value("#{'${gennlife.redis.server.endpoints}'.split(';')}")
    public Set<ImmutableEndpoint> redisServerEndpoints;
    @Value("${gennlife.fs.redis.namespace}")
    public String redisFsNamespace;
    public Config redissonConfig;

    @Value("${gennlife.fs.cluster.retry-limit}")
    public int clusterRetryLimit = 3;
    @Value("${gennlife.fs.cluster.heartbeat-interval}")
    public long clusterHeartbeatInterval = 5000;

    // Driver world not be loaded to current classloader if using @Value("${gennlife.fs.project-export.database.driver-class}")
    @Value("#{Class.forName('${gennlife.fs.database.driver-class}')}")
    public Class databaseDriverClass;
    @Value("${gennlife.fs.database.endpoint}")
    public ImmutableEndpoint databaseEndpoint;
    @Value("${gennlife.fs.database.url}")
    public String databaseUrl;
    @Value("${gennlife.fs.database.username}")
    public String databaseUsername;
    @Value("${gennlife.fs.database.password}")
    public String databasePassword;
    @Value("${gennlife.fs.database.version}")
    public int databaseVersion;
    @Value("${gennlife.fs.database.constants-table}")
    public String databaseConstantsTable;

    @Value("${gennlife.fs.project-export.storage.directory}")
    public String projectExportStorageDirectory;
    @Value("${gennlife.fs.project-export.storage.file-name}")
    public String projectExportStorageFileName;
    @Value("${gennlife.fs.project-export.storage.volume-size-threshold}")
    public long projectExportStorageVolumeSizeThreshold = 0L;  // in bytes, unlimited if <= 0
    @Value("${gennlife.fs.project-export.storage.patient-size-threshold}")
    public long projectExportStoragePatientSizeThreshold = 0;  // unlimited if <= 0

    @Value("${gennlife.fs.project-export.task.expiration-period}")
    public long projectExportTaskExpirationPeriod = 0L;  // in milliseconds, unlimited if <= 0
    @Value("${gennlife.fs.project-export.task.expiration-polling-interval}")
    public long projectExportTaskExpirationPollingInterval = 60000L;  // in milliseconds
    @Value("${gennlife.fs.project-export.task.expiration-warning-lead-time}")
    public long projectExportTaskExpirationWarningLeadTime = 0L;  // in milliseconds, no warning if <= 0
    @Value("${gennlife.fs.project-export.task.queuing-tasks-time-estimating-interval}")
    public long projectExportTaskQueuingTasksTimeEstimatingInterval = 0L;  // in milliseconds, does not estimate if <= 0
    @Value("${gennlife.fs.project-export.task.patient-count-limit}")
    public long projectExportTaskPatientCountLimit = 0L;  // unlimited if <= 0
    @Value("${gennlife.fs.project-export.task.concurrent-task-size-limit}")
    public long projectExportTaskConcurrentTaskSizeLimit = 0L;  // 1 if < 1
    @Value("${gennlife.fs.project-export.task.user-queue-size-limit}")
    public long projectExportTaskUserQueueSizeLimit = 0L;  // unlimited if <= 0
    @Value("${gennlife.fs.project-export.task.database-table}")
    public String projectExportTaskDatabaseTable;

    @Value("${gennlife.fs.project-export.message.producer.enabled}")
    public boolean projectExportMessageProducerEnabled;
    @Value("${gennlife.fs.project-export.message.producer.server-endpoint}")
    public ImmutableEndpoint projectExportMessageProducerServerEndpoint;
    @Value("${gennlife.fs.project-export.message.producer.group-name}")
    public String projectExportMessageProducerGroupName;
    @Value("${gennlife.fs.project-export.message.producer.max-message-size}")
    public int projectExportMessageProducerMaxMessageSize = 32768;
    @Value("${gennlife.fs.project-export.message.producer.send-message-timeout}")
    public int projectExportMessageProducerSendMessageTimeout = 1000;
    @Value("${gennlife.fs.project-export.message.producer.max-retry-times}")
    public int projectPexportMessageProducerMaxRetryTimes = 3;
    @Value("${gennlife.fs.project-export.message.producer.charset}")
    public Charset projectExportMessageProducerCharset = StandardCharsets.UTF_8;

    @Value("${gennlife.rwss.endpoint}")
    public ImmutableEndpoint rwsServerEndpoint;

    @Value("${gennlife.rwss.api.project-basic-info}")
    public String rwsServerProjectBasicInfoApi;
    @Value("${gennlife.rwss.api.project-patients}")
    public String rwsServerProjectPatientsApi;
    @Value("${gennlife.rwss.api.project-compute-custom-variable}")
    public String rwsServerProjectComputeCustomVariableApi;

    @Value("${gennlife.ss.endpoint}")
    public ImmutableEndpoint searchServerEndpoint;

    @Value("${gennlife.ss.api.search}")
    public String searchServerSearchApi;
    @Value("${gennlife.ss.api.highlight}")
    public String searchServerHighlightApi;
    @Value("${gennlife.ss.api.patient-document-id}")
    public String searchServerPatientDocumentIdApi;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("General configuration has been successfully loaded.");
    }

}
