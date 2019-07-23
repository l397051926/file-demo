package com.gennlife.fs.configurations;

import com.gennlife.darren.util.ImmutableEndpoint;
import com.gennlife.fs.configurations.model.ModelLoader;
import com.gennlife.fs.configurations.project.export.ProjectExportConfigurationLoader;
import lombok.val;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Set;

import static com.gennlife.fs.configurations.model.Model.generateCachesForAllModels;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;

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

    @Value("${gennlife.fs.hospital-name}")
    public String hospitalName;

    // Driver world not be loaded to current classloader if using @Value("${gennlife.fs.project.export.database.driver-class}")
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

    @Value("${gennlife.fs.project.export.storage.directory}")
    public String projectExportStorageDirectory;
    @Value("${gennlife.fs.project.export.storage.file-name}")
    public String projectExportStorageFileName;
    @Value("${gennlife.fs.project.export.storage.volume-size-threshold}")
    public long projectExportStorageVolumeSizeThreshold = 0L;  // in bytes, unlimited if <= 0
    @Value("${gennlife.fs.project.export.storage.patient-size-threshold}")
    public long projectExportStoragePatientSizeThreshold = 0;  // unlimited if <= 0

    @Value("${gennlife.fs.project.export.field-config.version}")
    public ModelVersion projectExportFieldConfigVersion;

    @Value("${gennlife.fs.project.export.task.expiration-period}")
    public long projectExportTaskExpirationPeriod = 0L;  // in milliseconds, unlimited if <= 0
    @Value("${gennlife.fs.project.export.task.expiration-polling-interval}")
    public long projectExportTaskExpirationPollingInterval = 60000L;  // in milliseconds
    @Value("${gennlife.fs.project.export.task.expiration-warning-lead-time}")
    public long projectExportTaskExpirationWarningLeadTime = 0L;  // in milliseconds, no warning if <= 0
    @Value("${gennlife.fs.project.export.task.queuing-tasks-time-estimating-interval}")
    public long projectExportTaskQueuingTasksTimeEstimatingInterval = 0L;  // in milliseconds, does not estimate if <= 0
    @Value("${gennlife.fs.project.export.task.patient-count-limit}")
    public long projectExportTaskPatientCountLimit = 0L;  // unlimited if <= 0
    @Value("${gennlife.fs.project.export.task.concurrent-task-size-limit}")
    public long projectExportTaskConcurrentTaskSizeLimit = 0L;  // 1 if < 1
    @Value("${gennlife.fs.project.export.task.user-queue-size-limit}")
    public long projectExportTaskUserQueueSizeLimit = 0L;  // unlimited if <= 0
    @Value("${gennlife.fs.project.export.task.database-table}")
    public String projectExportTaskDatabaseTable;

    @Value("${gennlife.fs.project.export.message.producer.enabled}")
    public boolean projectExportMessageProducerEnabled;
    @Value("${gennlife.fs.project.export.message.producer.server-endpoint}")
    public ImmutableEndpoint projectExportMessageProducerServerEndpoint;
    @Value("${gennlife.fs.project.export.message.producer.group-name}")
    public String projectExportMessageProducerGroupName;
    @Value("${gennlife.fs.project.export.message.producer.max-message-size}")
    public int projectExportMessageProducerMaxMessageSize = 32768;
    @Value("${gennlife.fs.project.export.message.producer.send-message-timeout}")
    public int projectExportMessageProducerSendMessageTimeout = 1000;
    @Value("${gennlife.fs.project.export.message.producer.max-retry-times}")
    public int projectPexportMessageProducerMaxRetryTimes = 3;
    @Value("${gennlife.fs.project.export.message.producer.charset}")
    public Charset projectExportMessageProducerCharset = UTF_8;

    @Value("${gennlife.rwss.endpoint}")
    public ImmutableEndpoint rwsServiceEndpoint;

    @Value("${gennlife.rwss.api.project-basic-info}")
    public String rwsServiceProjectBasicInfoApi;
    @Value("${gennlife.rwss.api.project-patients}")
    public String rwsServiceProjectPatientsApi;
    @Value("${gennlife.rwss.api.project-compute-custom-variable}")
    public String rwsServiceProjectComputeCustomVariableApi;

    @Value("${gennlife.ss.endpoint}")
    public ImmutableEndpoint searchServiceEndpoint;

    @Value("${gennlife.ss.api.search}")
    public String searchServiceSearchApi;
    @Value("${gennlife.ss.api.highlight}")
    public String searchServiceHighlightApi;
    @Value("${gennlife.ss.api.patient-document-id}")
    public String searchServicePatientDocumentIdApi;

    @Value("${gennlife.empi.endpoint}")
    public ImmutableEndpoint empiServiceEndpoint;

    @Value("${gennlife.empi.api.patient-info}")
    public String empiServicePatientInfoApi;

    @Value("${gennlife.fs.image.url}")
    public String imageUrl;

    @Autowired
    public void setEnvironment(Environment environment) throws IOException {
        val resolver = new RelaxedPropertyResolver(environment, "gennlife.fs.model.type.");
        val modelNames = resolver.getSubProperties("")
            .keySet()
            .stream()
            .map(rem -> rem.split("\\.")[0])
            .collect(toSet());
        for (val modelName : modelNames) {
            ModelLoader.load(modelName, new RelaxedPropertyResolver(environment, "gennlife.fs.model.type." + modelName + "."));
        }
        ProjectExportConfigurationLoader.load(projectExportFieldConfigVersion);
        generateCachesForAllModels();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("General configuration has been loaded successfully.");
    }

}
