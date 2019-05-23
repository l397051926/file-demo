package com.gennlife.fs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.Pair;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.collection.keypath.KeyPathSet;
import com.gennlife.darren.collection.string.Escape;
import com.gennlife.darren.util.Endpoint;
import com.gennlife.darren.util.ImmutableEndpoint;
import com.gennlife.fs.common.configurations.GeneralConfiguration;
import com.gennlife.fs.common.configurations.Model;
import com.gennlife.fs.common.configurations.TaskState;
import com.gennlife.fs.common.exception.*;
import com.gennlife.fs.common.utils.DBUtils;
import com.gennlife.fs.common.utils.KeyPathUtil;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultiset;
import lombok.Builder;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.gennlife.darren.collection.Pair.makePair;
import static com.gennlife.darren.controlflow.for_.Foreach.foreach;
import static com.gennlife.darren.util.Constants.INT_TRUE_VALUE;
import static com.gennlife.fs.common.configurations.Model.*;
import static com.gennlife.fs.common.configurations.TaskState.*;
import static com.gennlife.fs.common.utils.DBUtils.*;
import static com.gennlife.fs.common.utils.HttpRequestUtil.postData;
import static com.gennlife.fs.common.utils.KeyPathUtil.toPathString;
import static com.gennlife.fs.common.utils.TypeUtil.*;
import static com.gennlife.fs.controller.ProjectExportTaskController.PROJECT_EXPORT_TASK_CANCEL_BY_PROJECT_ID_SUB_API_PATH;
import static com.gennlife.fs.service.ProjectExportTaskDefinitions.*;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@Service
public class ProjectExportTaskService implements InitializingBean, ServletContextListener, ClusterEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectExportTaskService.class);

    @Builder
    public static class CreateParameters {
        public String userId;
        public String projectId;
    }

    public JSONObject create(CreateParameters params) throws ResponseException {
        requireNonNull(params.userId, "缺少参数：userId");
        requireNonNull(params.projectId, "缺少参数：projectId");
        val info = projectService.info(
            ProjectService.BasicInfoParameters.builder()
                .projectId(params.projectId)
                .build());
        if (!info.creatorId.equals(params.userId)) {
            throw new RestrictedException("用户 " + params.userId + " 无权创建该项目的导出任务");
        }
        val groupedPatients =
            projectService.patients(
                ProjectService.PatientsParameters.builder()
                    .userId(info.creatorId)
                    .projectId(params.projectId)
                    .build());
        val patientCount = groupedPatients
            .values()
            .stream()
            .mapToLong(List::size)
            .sum();
        if (cfg.projectExportTaskPatientCountLimit > 0) {
            if (patientCount > cfg.projectExportTaskPatientCountLimit) {
                throw new LimitExceededException("人数超出限制，无法导出");
            }
        }
        val mainModel = modelByRwsName(info.crfId);
        val models = new ArrayList<Model>();
        models.add(mainModel);  // SD-6100
        if (mainModel.isCRF()) {
            models.add(Model.emrModel());
        }
        models.add(new Model(info.customVariables));
        val keyHolder = new GeneratedKeyHolder();
        db.update(
            c ->
                DBUtils.prepareStatement(c, Statement.RETURN_GENERATED_KEYS,
                    "INSERT INTO " + Q(cfg.projectExportTaskDatabaseTable) + " "
                        + QP(USER_ID, PROJECT_ID, PROJECT_NAME, EXECUTOR, FILE_NAME, PATIENTS, PATIENT_COUNT, VISIT_TYPES, MODELS, SELECTED_FIELDS, CUSTOM_VARS)
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    info.creatorId,
                    info.id,
                    info.name,
                    cfg.localEndpoint.toString(),
                    info.name,
                    new JSONObject()
                        .fluentPutAll(groupedPatients)
                        .toJSONString(),
                    patientCount,
                    new JSONArray(asList(1, 2, 3)).toJSONString(),
                    models
                        .stream()
                        .filter(Model::isPredefined)
                        .map(Model::name)
                        .collect(toCollection(JSONArray::new))
                        .toJSONString(),
                    models
                        .stream()
                        .filter(model -> !mainModel.isCRF() || !model.isEMR())  // SD-6100
                        .flatMap(model -> model
                            .allFieldInfo()
                            .values()
                            .stream()
                            .filter(x -> x.selectedByDefault)
                            .map(x -> new KeyPath(model.name(), x.path)))
                        .map(KeyPathUtil::toPathString)
                        .collect(toCollection(JSONArray::new))
                        .toJSONString(),
                    info.customVariables
                        .stream()
                        .map(ProjectService.ProjectInfo.CustomVariable::toJSONObject)
                        .collect(toCollection(JSONArray::new))
                        .toJSONString()),
            keyHolder);
        return new JSONObject()
            .fluentPut("errorCode", ResponseCode.OK.getValue())
            .fluentPut("taskId", keyHolder.getKey());
    }

    @Builder
    public static class InfoParameters {
        public String userId;
        public Long taskId;
        @Builder.Default
        public boolean fetchModel = false;
    }

    public JSONObject info(InfoParameters params) throws ResponseException {
        requireNonNull(params.userId, "缺少参数：userId");
        requireNonNull(params.taskId, "缺少参数：taskId");
        val o = db.queryForMap("SELECT " + P(
            CREATE_TIME, FINISH_TIME, DOWNLOADED, FILE_NAME, FILE_SIZE, HEADER_TYPE,
            LAST_MODIFY_TIME, MODELS, PATIENT_COUNT, PROGRESS, PROJECT_ID, PROJECT_NAME,
            START_TIME, STATE, VISIT_TYPES, SELECTED_FIELDS, CUSTOM_VARS) +
            " FROM " + Q(cfg.projectExportTaskDatabaseTable) + " WHERE " + Q(TASK_ID) + " = ?",
            params.taskId);
        val ret = new JSONObject()
            .fluentPut(K(CREATE_TIME), L(o.get(CREATE_TIME)))
            .fluentPut(K(DOWNLOADED), B(o.get(DOWNLOADED)))
            .fluentPut(K(FILE_NAME), S(o.get(FILE_NAME)))
            .fluentPut(K(FILE_SIZE), L(o.get(FILE_SIZE)))
            .fluentPut(K(FINISH_TIME), L(o.get(FINISH_TIME)))
            .fluentPut(K(HEADER_TYPE), L(o.get(HEADER_TYPE)))
            .fluentPut(K(LAST_MODIFY_TIME), L(o.get(LAST_MODIFY_TIME)))
            .fluentPut(K(PATIENT_COUNT), L(o.get(PATIENT_COUNT)))
            .fluentPut(K(PROGRESS), F(o.get(PROGRESS)))
            .fluentPut(K(PROJECT_ID), S(o.get(PROJECT_ID)))
            .fluentPut(K(PROJECT_NAME), S(o.get(PROJECT_NAME)))
            .fluentPut(K(START_TIME), L(o.get(START_TIME)))
            .fluentPut(K(STATE), L(o.get(STATE)))
            .fluentPut(K(VISIT_TYPES), JSON.parseArray(S(o.get(VISIT_TYPES))));
        if (params.fetchModel) {
            val models = JSON.parseArray(S(o.get(MODELS)))
                .stream()
                .map(String.class::cast)
                .map(Model::modelByName)
                .collect(toList());
            val customModel = new Model(CVS(S(o.get(CUSTOM_VARS))));
            models.add(customModel);
            ret
                .fluentPut("model", models
                    .stream()
                    .map(Model::toFrontEndObject)
                    .filter(Objects::nonNull)
                    .collect(toCollection(JSONArray::new)))
                .fluentPut("selectedFields", JSON.parseArray(S(o.get(SELECTED_FIELDS))))
                .fluentPut("mergedFields", models
                    .stream()
                    .flatMap(model -> model.mergedFields()
                        .stream()
                        .map(path -> new KeyPath(model.name(), path)))
                    .map(KeyPathUtil::toPathString)
                    .collect(toCollection(JSONArray::new)));
        }
        return ret
            .fluentPut("errorCode", ResponseCode.OK.getValue());
    }

    @Builder
    public static class SaveInfoParameters {
        public String userId;
        public Long taskId;
        public String fileName;
        public Integer headerType;
        public KeyPathSet selectedFields;
        public Set<Integer> visitTypes;
    }

    public JSONObject saveInfo(SaveInfoParameters params) throws ResponseException {
        requireNonNull(params.userId, "缺少参数：userId");
        requireNonNull(params.taskId, "缺少参数：taskId");
        val updates = new JSONObject();
        if (params.fileName != null) {
            updates.put(FILE_NAME, params.fileName);
        }
        if (params.headerType != null) {
            updates.put(HEADER_TYPE, params.headerType);
        }
        if (params.selectedFields != null) {
            val paths = new JSONArray();
            Set<String> customVarIds = null;
            for (val path: params.selectedFields) {
                if (path.isEmpty() || path.stream().anyMatch(e -> !(e instanceof String))) {
                    throw new UnexpectedException("Field path format error: " + toPathString(path));
                }
                val modelName = path.firstAsString();
                if (CUSTOM_MODEL_NAME.equals(modelName)) {
                    if (customVarIds == null) {
                        val o = db.queryForMap(
                            "SELECT " + P(CUSTOM_VARS) +
                                " FROM " + Q(cfg.projectExportTaskDatabaseTable) +
                                " WHERE " + Q(TASK_ID) + " = ?",
                            params.taskId);
                        customVarIds = CVS(S(o.get(CUSTOM_VARS)))
                            .stream()
                            .map(e -> e.id)
                            .collect(toSet());
                    }
                    val varId = path.asString(1);
                    if (!customVarIds.contains(varId)) {
                        throw new UnexpectedException("Unknown custom variable: " + varId);
                    }
                } else {
                    val model = modelByName(modelName);
                    if (model == null) {
                        throw new UnexpectedException("Model not found: " + path.firstAsString());
                    }
                    val field = path.keyPathByRemovingFirst();
                    if (!model.fields().contains(field)) {
                        throw new UnexpectedException("Field not found: " + toPathString(path));
                    }
                    if (!model.fieldInfo(field).exportSupported) {
                        throw new UnexpectedException(toPathString(path) + " doesn't support export operation.");
                    }
                }
                paths.add(toPathString(path));
            }
            updates.put(SELECTED_FIELDS, paths.toJSONString());
        }
        if (params.visitTypes != null) {
            updates.put(VISIT_TYPES, new JSONArray().fluentAddAll(params.visitTypes).toJSONString());
        }
        if (updates.isEmpty()) {
            return new JSONObject()
                .fluentPut("errorCode", ResponseCode.OK.getValue());
        }
        try {
            db.update(c -> {
                val sql = new StringBuilder("UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET ");
                val values = new ArrayList<>();
                foreach(updates, (field, value) -> {
                    sql.append(Q(field)).append(" = ?,");
                    values.add(value);
                });
                sql.deleteCharAt(sql.lastIndexOf(","));
                sql.append(" WHERE ").append(Q(TASK_ID)).append(" = ?");
                values.add(params.taskId);
                return DBUtils.prepareStatement(c, Statement.RETURN_GENERATED_KEYS, sql.toString(), values.toArray());
            });
            return new JSONObject()
                .fluentPut("errorCode", ResponseCode.OK.getValue());
        } catch (Exception e) {
            throw new TransferFailedException(e);
        }
    }

    @Builder
    public static class StartParameters {
        public String userId;
        public Long taskId;
    }

    public JSONObject start(StartParameters params) throws ResponseException {
        requireNonNull(params.userId, "缺少参数：userId");
        requireNonNull(params.taskId, "缺少参数：taskId");
        val o = db.queryForMap(
            "SELECT " + P(USER_ID, PATIENT_COUNT) +
                " FROM " + Q(cfg.projectExportTaskDatabaseTable) +
                " WHERE " + Q(TASK_ID) + " = ?",
            params.taskId);
        val userId = S(o.get(USER_ID));
        if (!Objects.equals(userId, params.userId)) {
            throw new RestrictedException("用户 " + params.userId + " 无权操作此任务");
        }
        synchronized (TASKS_MUTEX) {
            if (QUEUING_TASKS.containsKey(params.taskId) || RUNNING_TASKS.containsKey(params.taskId)) {
                throw new IncorrentStateException("任务已经开始执行");
            }
        }
        val queuingTaskSize = db.queryForObject(
            "SELECT COUNT(1) FROM " + Q(cfg.projectExportTaskDatabaseTable) +
                " WHERE " + Q(USER_ID) + " = ? AND " +
                Q(STATE) + " = ?",
            Long.class, params.userId, QUEUING.value());
        if (cfg.projectExportTaskUserQueueSizeLimit > 0) {
            if (queuingTaskSize >= cfg.projectExportTaskUserQueueSizeLimit) {
                throw new RestrictedException("排队任务数较多，请完成后再导出");
            }
        }
        val task = new ProjectExportTask(params.taskId);
        task.totalPatientCount.set(L(o.get(PATIENT_COUNT)));
        synchronized (TASKS_MUTEX) {
            QUEUING_TASKS.put(params.taskId, task);
        }
        db.update(
            "UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET " +
                Q(EXECUTOR) + " = ?, " +
                Q(STATE) + " = ?, " +
                Q(START_TIME) + " = NULL, " +
                Q(LAST_MODIFY_TIME) + " = now() " +
                "WHERE " + Q(TASK_ID) + " = ?",
            cfg.localEndpoint.toString(), QUEUING.value(), params.taskId);
        taskExecutor.execute(task);
        return new JSONObject()
            .fluentPut("errorCode", ResponseCode.OK.getValue());
    }

    @Builder
    public static class CancelParameters {
        public String userId;
        public Long taskId;
    }

    public JSONObject cancel(CancelParameters params) throws ResponseException {
        requireNonNull(params.userId, "缺少参数：userId");
        requireNonNull(params.taskId, "缺少参数：taskId");
        ProjectExportTask task;
        synchronized (TASKS_MUTEX) {
            task = QUEUING_TASKS.getOrDefault(params.taskId, RUNNING_TASKS.get(params.taskId));
        }
        if (task != null) {
            task.shouldStop.set(true);
            taskExecutor.remove(task);
        }
        db.update(
            "UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET " +
                Q(PROGRESS) + " = ?, " +
                Q(STATE) + " = ?, " +
                Q(LAST_MODIFY_TIME) + " = now(), " +
                Q(ESTIMATED_FINISH_TIME) + " = null " +
                "WHERE " + Q(TASK_ID) + " = ?",
            null, FAILED.value(), params.taskId);
        return new JSONObject()
            .fluentPut("errorCode", ResponseCode.OK.getValue());
    }

    @Builder
    public static class CancelByProjectIdParameters {
        public String userId;
        public String projectId;
    }

    public JSONObject cancelByProjectId(CancelByProjectIdParameters params) throws ResponseException {
        requireNonNull(params.userId, "缺少参数：userId");
        requireNonNull(params.projectId, "缺少参数：projectId");
        val os = db.queryForList(
            "SELECT " + P(TASK_ID, USER_ID, EXECUTOR) +
                " FROM " + Q(cfg.projectExportTaskDatabaseTable) +
                " WHERE " + Q(PROJECT_ID) + " = ?",
            params.projectId);
        for (val o : os) {
            val taskId = L(o.get(TASK_ID));
            val userId = S(o.get(USER_ID));
            val executor = S(o.get(EXECUTOR));
            batchCancellationExecutor.submit(() -> {
                try {
                    postData(
                        new ImmutableEndpoint(executor),
                        PROJECT_EXPORT_TASK_CANCEL_BY_PROJECT_ID_SUB_API_PATH,
                        new JSONObject()
                            .fluentPut("userId", userId)
                            .fluentPut("taskId", taskId));
                } catch (Exception e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
            });
        }
        return new JSONObject()
            .fluentPut("errorCode", ResponseCode.OK.getValue());
    }

    @Builder
    public static class RetryParameters {
        public String userId;
        public Long taskId;
    }

    public JSONObject retry(RetryParameters params) throws ResponseException {
        requireNonNull(params.userId, "缺少参数：userId");
        requireNonNull(params.taskId, "缺少参数：taskId");
        start(StartParameters.builder()
            .userId(params.userId)
            .taskId(params.taskId)
            .build());
        return new JSONObject()
            .fluentPut("errorCode", ResponseCode.OK.getValue());
    }

    @Builder
    public static class DeleteParameters {
        public String userId;
        public Long taskId;
    }

    public JSONObject delete(DeleteParameters params) throws ResponseException {
        requireNonNull(params.userId, "缺少参数：userId");
        requireNonNull(params.taskId, "缺少参数：taskId");
        synchronized (TASKS_MUTEX) {
            if (QUEUING_TASKS.containsKey(params.taskId) || RUNNING_TASKS.containsKey(params.taskId)) {
                throw new IncorrentStateException("Export has not been finished yet.");
            }
        }
        val directoryPath = def.dir(params.userId, params.taskId);
        try {
            deleteDirectory(directoryPath.toFile());
            db.update("DELETE FROM " + Q(cfg.projectExportTaskDatabaseTable) + " WHERE " + Q(TASK_ID) + " = ?",
                params.taskId);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
        return new JSONObject()
            .fluentPut("errorCode", ResponseCode.OK.getValue());
    }

    @Builder
    public static class DownloadParameters {
        public String userId;
        public Long taskId;
        public HttpServletResponse response;
    }

    public void download(DownloadParameters params) throws ResponseException {
        requireNonNull(params.userId, "缺少参数：userId");
        requireNonNull(params.taskId, "缺少参数：taskId");
        synchronized (TASKS_MUTEX) {
            if (QUEUING_TASKS.containsKey(params.taskId) || RUNNING_TASKS.containsKey(params.taskId)) {
                throw new IncorrentStateException("Export has not been finished yet.");
            }
        }
        val o = db.queryForMap("SELECT " + P(USER_ID, STATE, FILE_NAME) +
                " FROM " + Q(cfg.projectExportTaskDatabaseTable) +
                " WHERE " + Q(TASK_ID) + " = ?",
            params.taskId);
        val userId = S(o.get(USER_ID));
        if (!Objects.equals(userId, params.userId)) {
            throw new RestrictedException("Current user is not allowed to download the result of task " + params.taskId + ".");
        }
        val state = TaskState.withValue(I(o.get(STATE)));
        if (!FINISHED.equals(state)) {
            throw new IncorrentStateException("Export has not been finished yet.");
        }
        val fileName = S(o.get(FILE_NAME));
        try {
            val is = Files.newInputStream(
                def.dir(params.userId, params.taskId)
                    .resolve(cfg.projectExportStorageFileName + ".zip"));
            val response = params.response;
            response.setContentType(APPLICATION_PDF_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()) + ".zip\"");
            IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
            db.update("UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET " + Q(DOWNLOADED) + " = ? WHERE " + Q(TASK_ID) + " = ?",
                INT_TRUE_VALUE, params.taskId);
        } catch (IOException e) {
            throw new TransferFailedException(e);
        }
    }

    @Builder
    public static class ListParameters {
        public Long createTimeFrom;
        public Long createTimeTo;
        public Boolean downloaded;
        public Long estimatedFinishTimeFrom;
        public Long estimatedFinishTimeTo;
        public String fileName;
        public Long fileSizeFrom;
        public Long fileSizeTo;
        public Long finishTimeFrom;
        public Long finishTimeTo;
        public Integer headerType;
        public Float progressFrom;
        public Float progressTo;
        public Long lastModifyTimeFrom;
        public Long lastModifyTimeTo;
        public Long patientCountFrom;
        public Long patientCountTo;
        public Long page;
        public Long pageSize;
        public String projectId;
        public String projectName;
        public Long startTimeFrom;
        public Long startTimeTo;
        public Map<String, Boolean> sortedBys;  // true -> ASC, false -> DESC
        public Integer state;
        public Long taskId;
        public String userId;
    }

    public JSONObject list(ListParameters params) throws ResponseException {
        val page = orDefault(params.page, 1L);
        val pageSize = orDefault(params.pageSize, 10L);
        val orderBys = (params.sortedBys == null || params.sortedBys.isEmpty() ?
            singletonList(Q(LAST_MODIFY_TIME) + " DESC") :
            params.sortedBys
                .entrySet()
                .stream()
                .map(e -> Q(FIELD_KEYS.inverse().get(e.getKey())) + (e.getValue() ? " ASC" : " DESC"))
                .collect(toList()));
        final StringBuilder where = new StringBuilder("1");
        val values = new ArrayList<>();
        append(where, CREATE_TIME, GE, params.createTimeFrom, values);
        append(where, CREATE_TIME, LE, params.createTimeTo, values);
        append(where, DOWNLOADED, EQ, params.downloaded, values);
        append(where, ESTIMATED_FINISH_TIME, GE, params.estimatedFinishTimeFrom, values);
        append(where, ESTIMATED_FINISH_TIME, LE, params.estimatedFinishTimeTo, values);
        append(where, FILE_NAME, LIKE, params.fileName, values);
        append(where, FILE_SIZE, GE, params.fileSizeFrom, values);
        append(where, FILE_SIZE, LE, params.fileSizeTo, values);
        append(where, FINISH_TIME, GE, params.finishTimeFrom, values);
        append(where, FINISH_TIME, LE, params.finishTimeTo, values);
        append(where, HEADER_TYPE, EQ, params.headerType, values);
        append(where, LAST_MODIFY_TIME, GE, params.lastModifyTimeFrom, values);
        append(where, LAST_MODIFY_TIME, LE, params.lastModifyTimeTo, values);
        append(where, PATIENT_COUNT, GE, params.patientCountFrom, values);
        append(where, PATIENT_COUNT, LE, params.patientCountTo, values);
        append(where, PROGRESS, GE, params.progressFrom, values);
        append(where, PROGRESS, LE, params.progressTo, values);
        append(where, PROJECT_ID, EQ, params.projectId, values);
        append(where, PROJECT_NAME, LIKE, params.projectName, values);
        append(where, START_TIME, GE, params.startTimeFrom, values);
        append(where, START_TIME, LE, params.startTimeTo, values);
        append(where, STATE, EQ, params.state, values);
        append(where, TASK_ID, EQ, params.taskId, values);
        append(where, USER_ID, EQ, params.userId, values);
        val tasks = db.query(
            "SELECT " + P(
                CREATE_TIME, DOWNLOADED, ESTIMATED_FINISH_TIME, FILE_NAME, FILE_SIZE,
                FINISH_TIME, HEADER_TYPE, PROGRESS, LAST_MODIFY_TIME, PATIENT_COUNT,
                PROJECT_ID, PROJECT_NAME, START_TIME, STATE, TASK_ID, USER_ID, VISIT_TYPES) +
                ", (" + Q(ESTIMATED_FINISH_TIME) + " - now()) * 1000 AS " + Q(ESTIMATED_REMAIN_TIME) +
                " FROM " + Q(cfg.projectExportTaskDatabaseTable) +
                " WHERE " + where +
                " ORDER BY " + Q(STATE) + " = " + FINISHED.value() +
                " AND " + Q(DOWNLOADED) + " IS FALSE DESC, " +
                join(", ", orderBys) +
                " LIMIT " + ((page - 1) * pageSize) + ", " + pageSize,
            (rs, n) -> new JSONObject()
                .fluentPut(K(CREATE_TIME), L(rs.getObject(CREATE_TIME)))
                .fluentPut(K(DOWNLOADED), B(rs.getObject(DOWNLOADED)))
                .fluentPut(K(ESTIMATED_FINISH_TIME), L(rs.getObject(ESTIMATED_FINISH_TIME)))
                .fluentPut(K(ESTIMATED_REMAIN_TIME), L(rs.getObject(ESTIMATED_REMAIN_TIME)))
                .fluentPut(K(FILE_NAME), S(rs.getObject(FILE_NAME)))
                .fluentPut(K(FILE_SIZE), L(rs.getObject(FILE_SIZE)))
                .fluentPut(K(FINISH_TIME), L(rs.getObject(FINISH_TIME)))
                .fluentPut(K(HEADER_TYPE), I(rs.getObject(HEADER_TYPE)))
                .fluentPut(K(PROGRESS), F(rs.getObject(PROGRESS)))
                .fluentPut(K(LAST_MODIFY_TIME), L(rs.getObject(LAST_MODIFY_TIME)))
                .fluentPut(K(PATIENT_COUNT), L(rs.getObject(PATIENT_COUNT)))
                .fluentPut(K(PROJECT_ID), S(rs.getObject(PROJECT_ID)))
                .fluentPut(K(PROJECT_NAME), S(rs.getObject(PROJECT_NAME)))
                .fluentPut(K(START_TIME), L(rs.getObject(START_TIME)))
                .fluentPut(K(STATE), I(rs.getObject(STATE)))
                .fluentPut(K(TASK_ID), L(rs.getObject(TASK_ID)))
                .fluentPut(K(USER_ID), S(rs.getObject(USER_ID)))
                .fluentPut(K(VISIT_TYPES), JSON.parseArray(S(rs.getObject(VISIT_TYPES)))),
            values.toArray());
        val total = db.queryForObject(
            "SELECT count(1) FROM " + Q(cfg.projectExportTaskDatabaseTable) + " WHERE " + where,
            values.toArray(),
            Integer.class);
        return new JSONObject()
            .fluentPut("errorCode", ResponseCode.OK.getValue())
            .fluentPut("list", new JSONArray().fluentAddAll(tasks))
            .fluentPut("total", total)
            .fluentPut("totalPageCount", Math.ceil((double)total / (double)pageSize));
    }

    private static void append(StringBuilder sql_, String field, String condition, Object value, List<Object> values) {
        if (value != null) {
            if (LIKE.equals(condition)) {
                value = "%" + Escape.escapeSQLLike(value.toString()) + "%";
            }
            sql_.append(" AND ")
                .append(Q(field))
                .append(" ")
                .append(condition)
                .append(" ?");
            values.add(value);
        }
    }

    public ImmutableEndpoint executor(long taskId) {
        val ret = db.queryForObject(
            "SELECT " + Q(EXECUTOR) +
                " FROM " + Q(cfg.projectExportTaskDatabaseTable) +
                " WHERE " + Q(TASK_ID) + " = ?",
            String.class,
            taskId);
        return new ImmutableEndpoint(ret);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        db.update(
            "UPDATE " + Q(cfg.projectExportTaskDatabaseTable) +
                " SET " + Q(STATE) + " = ?, " +
                Q(ESTIMATED_FINISH_TIME) + " = NULL" +
                " WHERE " + Q(STATE) + " IN (?, ?)" +
                " AND " + Q(EXECUTOR) + " = ?",
            FAILED.value(), QUEUING.value(), RUNNING.value(),
            cfg.localEndpoint.toString());
        if (cfg.projectExportTaskExpirationPeriod > 0) {
            expirationChecker.scheduleAtFixedRate(() -> {
                try {
                    val os = db.queryForList(
                        "SELECT " + P(USER_ID, TASK_ID) +
                            " FROM " + Q(cfg.projectExportTaskDatabaseTable) +
                            " WHERE " + Q(STATE) + " = ?" +
                            " AND unix_timestamp() - unix_timestamp(" + Q(FINISH_TIME) + ") > ? / 1000",
                        FINISHED.value(), cfg.projectExportTaskExpirationPeriod);
                    for (val o : os) {
                        try {
                            val userId = S(o.get(USER_ID));
                            val taskId = L(o.get(TASK_ID));
                            deleteDirectory(def.dir(userId, taskId).toFile());
                            db.update(
                                "UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET " +
                                    Q(STATE) + " = ?, " +
                                    Q(DOWNLOADED) + " = NULL" +
                                    " WHERE " + Q(TASK_ID) + " = ?",
                                EXPIRED.value(), taskId);
                            LOGGER.info("Task " + taskId + " has expired.");
                        } catch (Exception e) {
                            LOGGER.error(e.getLocalizedMessage(), e);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
            }, 0, cfg.projectExportTaskExpirationPollingInterval, MILLISECONDS);
            if (cfg.projectExportTaskExpirationWarningLeadTime > 0) {
                expirationChecker.scheduleAtFixedRate(() -> {
                    if (cfg.localEndpoint.equals(master.get())) {
                        try {
                            val os = db.queryForList(
                                "SELECT " + P(USER_ID, TASK_ID, PROJECT_ID, PROJECT_NAME) +
                                    " FROM " + Q(cfg.projectExportTaskDatabaseTable) +
                                    " WHERE " + Q(STATE) + " = ?" +
                                    " AND " + Q(EXPIRE_NOTIFIED) + " IS NOT TRUE" +
                                    " AND unix_timestamp() - unix_timestamp(" + Q(FINISH_TIME) + ") > ? / 1000",
                                FINISHED.value(), cfg.projectExportTaskExpirationPeriod - cfg.projectExportTaskExpirationWarningLeadTime);
                            for (val o : os) {
                                try {
                                    val userId = S(o.get(USER_ID));
                                    val taskId = L(o.get(TASK_ID));
                                    val projectId = S(o.get(PROJECT_ID));
                                    val projectName = S(o.get(PROJECT_NAME));
                                    def.sendMessage("2203", new JSONObject()
                                        .fluentPut("user_id", userId)
                                        .fluentPut("task_id", taskId)
                                        .fluentPut("project_id", projectId)
                                        .fluentPut("msg", projectName + "项目的导出文件将在1天内过期，请及时下载"));  // TODO: calculate this duration
                                    db.update(
                                        "UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET " +
                                            Q(EXPIRE_NOTIFIED) + " = TRUE" +
                                            " WHERE " + Q(TASK_ID) + " = ?",
                                        taskId);
                                    LOGGER.info("Task " + taskId + " will expire in " + cfg.projectExportTaskExpirationWarningLeadTime + "ms.");
                                } catch (Exception e) {
                                    LOGGER.error(e.getLocalizedMessage(), e);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error(e.getLocalizedMessage(), e);
                        }
                    }
                }, 0, cfg.projectExportTaskExpirationPollingInterval, MILLISECONDS);
            }
        }
        if (cfg.projectExportTaskQueuingTasksTimeEstimatingInterval > 0) {
            queuingTaskTimeEstimator.scheduleAtFixedRate(() -> {
                try {
                    List<ProjectExportTask> runningTasks;
                    List<ProjectExportTask> queuingTasks;
                    synchronized (TASKS_MUTEX) {
                        runningTasks = new ArrayList<>(RUNNING_TASKS.values());
                        queuingTasks = new ArrayList<>(QUEUING_TASKS.values());
                    }
                    // {<patients, time>} sorted by patients
                    val barrels = TreeMultiset.<Pair<Long, Long>>create(comparing(Pair::_1));
                    val currentTime = currentTimeMillis();
                    long timeElapsed = 0L;
                    long patientExported = 0L;
                    for (val task : runningTasks) {
                        val startTime = task.localStartTime.get();
                        val exportedPatientCount = task.exportedPatientCount.get();
                        val totalPatientCount = task.totalPatientCount.get();
                        if (exportedPatientCount > 0) {
                            timeElapsed += currentTime - startTime;
                            patientExported += exportedPatientCount;
                        }
                        val o = db.queryForMap("SELECT " + Q(START_TIME) +
                                " FROM " + Q(cfg.projectExportTaskDatabaseTable) +
                                " WHERE " + Q(TASK_ID) + " = ?",
                            task.taskId);
                        barrels.add(makePair(
                            totalPatientCount - exportedPatientCount,
                            currentTime - startTime + L(o.get(START_TIME))));  // prevent time out of sync
                    }
                    if (timeElapsed == 0) {
                        return;
                    }
                    val speed = (double)patientExported / (double)timeElapsed;  // in (person / ms)
                    for (val task : queuingTasks) {
                        val totalPatientCount = task.totalPatientCount.get();
                        val barrel = barrels.pollFirstEntry().getElement();
                        val estimatedFinishTime = (long)(barrel._2() + (totalPatientCount / speed));
                        barrels.add(makePair(barrel._1() + totalPatientCount, estimatedFinishTime));
                        db.update("UPDATE " + Q(cfg.projectExportTaskDatabaseTable) +
                                " SET " + Q(ESTIMATED_FINISH_TIME) + " = from_unixtime(? / 1000)" +
                                " WHERE " + Q(TASK_ID) + " = ? " +
                                " AND " + Q(STATE) + " = ?",  // prevent concurrent problems
                            estimatedFinishTime, task.taskId, QUEUING.value());
                    }
                } catch (Throwable e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
            }, 0, cfg.projectExportTaskQueuingTasksTimeEstimatingInterval, MILLISECONDS);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        taskExecutor.shutdownNow();
        queuingTaskTimeEstimator.shutdownNow();
        batchCancellationExecutor.shutdownNow();
        expirationChecker.shutdownNow();
    }

    @Override
    public void clusterMasterChanged(ImmutableEndpoint from, ImmutableEndpoint to) throws Exception {
        master.set(to);
    }

    @Override
    public void clusterNodesDisconnected(ImmutableSet<ImmutableEndpoint> nodes) throws Exception {
        if (cfg.localEndpoint.equals(master.get())) {
            val db = new NamedParameterJdbcTemplate(this.db);
            val toState = "toState";
            val fromStates = "fromStates";
            val executors = "executors";
            val params = new MapSqlParameterSource()
                .addValue(toState, FAILED.value())
                .addValue(fromStates, asList(QUEUING.value(), RUNNING.value()))
                .addValue(executors, nodes
                    .stream()
                    .map(Endpoint::toString)
                    .collect(toList()));
            db.update("UPDATE " + Q(cfg.projectExportTaskDatabaseTable) +
                    " SET " + Q(STATE) + " = :" + toState + ", " +
                    Q(ESTIMATED_FINISH_TIME) + " = NULL" +
                    " WHERE " + Q(STATE) + " IN (:" + fromStates +  ")" +
                    " AND " + Q(EXECUTOR) + " IN (:" + executors + ")",
                params);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        val threadCount = (int)Math.max(cfg.projectExportTaskConcurrentTaskSizeLimit, 1);
        taskExecutor = new ThreadPoolExecutor(
            threadCount, threadCount,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>()) {

            @Override
            public void execute(Runnable cmd) {
                super.execute(cmd);
                synchronized (TASKS_MUTEX) {
                    val task = (ProjectExportTask)cmd;
                    QUEUING_TASKS.put(task.taskId, task);
                }
            }

            @Override
            @SuppressWarnings("SuspiciousMethodCalls")
            public boolean remove(Runnable cmd) {
                boolean ret = super.remove(cmd);
                synchronized (TASKS_MUTEX) {
                    val task = (ProjectExportTask)cmd;
                    QUEUING_TASKS.remove(task.taskId);
                    RUNNING_TASKS.remove(task.taskId);
                }
                return ret;
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                synchronized (TASKS_MUTEX) {
                    val task = (ProjectExportTask)r;
                    RUNNING_TASKS.remove(task.taskId);
                }
            }

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                synchronized (TASKS_MUTEX) {
                    val task = (ProjectExportTask)r;
                    QUEUING_TASKS.remove(task.taskId);
                    RUNNING_TASKS.put(task.taskId, task);
                }
            }

        };
        clusterService.addClusterEventListener(this);
        db = databaseService.jdbcTemplate();
    }

    @Autowired
    private GeneralConfiguration cfg;

    @Autowired
    private ProjectExportTaskDefinitions def;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private DatabaseService databaseService;
    private JdbcTemplate db;

    private ThreadPoolExecutor taskExecutor;
    private final ThreadPoolExecutor batchCancellationExecutor = (ThreadPoolExecutor)newFixedThreadPool(getRuntime().availableProcessors());
    private final ScheduledExecutorService expirationChecker = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService queuingTaskTimeEstimator = Executors.newSingleThreadScheduledExecutor();

    private final AtomicReference<ImmutableEndpoint> master = new AtomicReference<>();

}
