package com.gennlife.fs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.google.common.collect.ImmutableBiMap;
import lombok.val;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gennlife.darren.controlflow.exception.Try.try_;
import static com.gennlife.fs.common.utils.TypeUtil.S;
import static java.util.stream.Collectors.toList;

@Service
public class ProjectExportTaskDefinitions implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectExportTaskDefinitions.class);

    @Autowired
    private GeneralConfiguration cfg;

    MQProducer mq;

    static final Object TASKS_MUTEX = new Object();
    static final Map<Long, ProjectExportTask> QUEUING_TASKS = new LinkedHashMap<>();  // ordered
    static final Map<Long, ProjectExportTask> RUNNING_TASKS = new ConcurrentHashMap<>();

    static final String TASK_ID = "TASK_ID";
    static final String USER_ID = "USER_ID";
    static final String PROJECT_ID = "PROJECT_ID";
    static final String PROJECT_NAME = "PROJECT_NAME";
    static final String ESTIMATED_FINISH_TIME = "ESTIMATED_FINISH_TIME";
    static final String ESTIMATED_REMAIN_TIME = "ESTIMATED_REMAIN_TIME";
    static final String EXECUTOR = "EXECUTOR";
    static final String STATE = "STATE";
    static final String LAST_MODIFY_TIME = "LAST_MODIFY_TIME";
    static final String CREATE_TIME = "CREATE_TIME";
    static final String START_TIME = "START_TIME";
    static final String FINISH_TIME = "FINISH_TIME";
    static final String PROGRESS = "PROGRESS";
    static final String FILE_NAME = "FILE_NAME";
    static final String FILE_SIZE = "FILE_SIZE";
    static final String DOWNLOADED = "DOWNLOADED";
    static final String EXPIRE_NOTIFIED = "EXPIRE_NOTIFIED";
    static final String PATIENTS = "PATIENTS";
    static final String PATIENT_COUNT = "PATIENT_COUNT";
    static final String VISIT_TYPES = "VISIT_TYPES";
    static final String HEADER_TYPE = "HEADER_TYPE";
    static final String MODELS = "MODELS";
    static final String SELECTED_FIELDS = "SELECTED_FIELDS";
    static final String CUSTOM_VARS = "CUSTOM_VARS";

    static final ImmutableBiMap<String, String> FIELD_KEYS = ImmutableBiMap.<String, String>builder()
        .put(CREATE_TIME, "createTime")
        .put(DOWNLOADED, "downloaded")
        .put(ESTIMATED_FINISH_TIME, "estimatedFinishTime")
        .put(ESTIMATED_REMAIN_TIME, "estimatedRemainTime")
        .put(FILE_NAME, "fileName")
        .put(FILE_SIZE, "fileSize")
        .put(FINISH_TIME, "finishTime")
        .put(HEADER_TYPE, "headerType")
        .put(PROGRESS, "progress")
        .put(LAST_MODIFY_TIME, "lastModifyTime")
        .put(PATIENT_COUNT, "patientCount")
        .put(PROJECT_ID, "projectId")
        .put(PROJECT_NAME, "projectName")
        .put(START_TIME, "startTime")
        .put(STATE, "state")
        .put(TASK_ID, "taskId")
        .put(USER_ID, "userId")
        .put(VISIT_TYPES, "visitTypes")
        .build();

    static <T> T orDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    static String K(String field) {
        return FIELD_KEYS.get(field);
    }

    static List<ProjectService.ProjectInfo.CustomVariable> CVS(String s) {
        return try_(() -> JSON.parseArray(s)
            .stream()
            .map(JSONObject.class::cast)
            .map(ProjectService.ProjectInfo.CustomVariable::fromJSONObject)
            .collect(toList()))
            .orElse(null);
    }

    Path dir(String userId, Long taskId) {
        return cfg.storageRoot
            .resolve(cfg.projectExportStorageDirectory)
            .resolve(userId)
            .resolve(S(taskId));
    }

    void sendMessage(String tags, JSONObject body) {
        if (mq == null) {
            return;
        }
        val msg = new Message();
        msg.setTopic("TOPIC_PRO");
        msg.setTags(tags);
        msg.setBody(body.toJSONString().getBytes(cfg.projectExportMessageProducerCharset));
        try {
            mq.send(msg);
        } catch (Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (cfg.projectExportMessageProducerEnabled) {
            val p = new DefaultMQProducer();
            p.setNamesrvAddr(cfg.projectExportMessageProducerServerEndpoint.toString());
            p.setProducerGroup(cfg.projectExportMessageProducerGroupName);
            p.setMaxMessageSize(cfg.projectExportMessageProducerMaxMessageSize);
            p.setSendMsgTimeout(cfg.projectExportMessageProducerSendMessageTimeout);
            p.setRetryTimesWhenSendFailed(cfg.projectPexportMessageProducerMaxRetryTimes);
            p.setRetryTimesWhenSendAsyncFailed(cfg.projectPexportMessageProducerMaxRetryTimes);
            mq = p;
            mq.start();
        }
    }

    @PreDestroy
    public void destroy() {
        if (mq != null) {
            mq.shutdown();
        }
    }

}
