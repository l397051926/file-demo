package com.gennlife.fs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.collection.keypath.KeyPathSet;
import com.gennlife.fs.common.utils.TypeUtil;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.configurations.model.Model;
import com.gennlife.fs.configurations.project.export.HeaderType;
import lombok.val;
import lombok.var;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.gennlife.darren.controlflow.exception.Suppress.suppress;
import static com.gennlife.darren.controlflow.exception.Try.try_;
import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachValue;
import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;
import static com.gennlife.fs.common.utils.DBUtils.P;
import static com.gennlife.fs.common.utils.DBUtils.Q;
import static com.gennlife.fs.common.utils.KeyPathUtil.toPathString;
import static com.gennlife.fs.common.utils.TypeUtil.*;
import static com.gennlife.fs.configurations.model.Model.CUSTOM_MODEL_NAME;
import static com.gennlife.fs.configurations.model.Model.modelByName;
import static com.gennlife.fs.configurations.model.SourceType.RWS_SERVICE;
import static com.gennlife.fs.configurations.project.export.TaskState.*;
import static com.gennlife.fs.service.ProjectExportTaskDefinitions.*;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.createDirectories;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.poi.ss.usermodel.VerticalAlignment.BOTTOM;
import static org.apache.poi.ss.usermodel.VerticalAlignment.TOP;

public class ProjectExportTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectExportTask.class);

    public AtomicBoolean shouldStop = new AtomicBoolean(false);

    public final long taskId;
    public final AtomicLong localStartTime = new AtomicLong(0);
    public final AtomicLong totalPatientCount = new AtomicLong(0);
    public final AtomicLong exportedPatientCount = new AtomicLong(0);

    ProjectExportTask(long taskId) {
        this.taskId = taskId;
    }

    @Override
    public void run() {
        Path directoryPath = null;
        String userId = null;
        String projectName = null;
        String projectId = null;
        try {
            db.update(
                "UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET " +
                    Q(STATE) + " = ?, " +
                    Q(PROGRESS) + " = ?, " +
                    Q(START_TIME) + " = now(), " +
                    Q(LAST_MODIFY_TIME) + " = now() " +
                    "WHERE " + Q(TASK_ID) + " = ?",
                RUNNING.value(), 0, taskId);
            val o = db.queryForMap(
                "SELECT " + P(
                    USER_ID, FILE_NAME, HEADER_TYPE, PROJECT_ID, PROJECT_NAME, FILE_NAME,
                    MODELS, SELECTED_FIELDS, CUSTOM_VARS, PATIENTS) +
                    " FROM " + Q(cfg.projectExportTaskDatabaseTable) + " WHERE " + Q(TASK_ID) + " = ?",
                taskId);
            userId = S(o.get(USER_ID));
            directoryPath = def.dir(userId, taskId);
            projectName = S(o.get(PROJECT_NAME));
            projectId = S(o.get(PROJECT_ID));
            val headerType = HeaderType.withValue(I(o.get(HEADER_TYPE)));
            val fileBaseName = S(o.get(FILE_NAME));
            val unorderedPaths = JSON.parseArray(S(o.get(SELECTED_FIELDS)))
                .stream()
                .map(TypeUtil::S)
                .map(KeyPath::compile)
                .collect(toCollection(KeyPathSet::new));
            val models = JSON.parseArray(S(o.get(MODELS)))
                .fluentAdd(CUSTOM_MODEL_NAME)
                .stream()
                .map(String.class::cast)
                .filter(modelName -> unorderedPaths.subSet(modelName) != null)
                .collect(toMap(
                    identity(),
                    modelName -> CUSTOM_MODEL_NAME.equals(modelName) ?
                        try_(() -> new Model(CVS(S(o.get(CUSTOM_VARS))))).orElse(null) :
                        modelByName(modelName),
                    (a, b) -> a,
                    LinkedHashMap::new));
            val pathList = models
                .values()
                .stream()
                .flatMap(model -> unorderedPaths.subSet(model.name())
                    .stream()
                    .sorted(comparingInt(field -> model.fieldInfo(field).projectExport.index))
                    .map(field -> new KeyPath(model.name(), field)))
                .collect(toList());
            val paths = new KeyPathSet(pathList, LinkedHashMap::new);
            val layers = pathList
                .stream()
                .mapToInt(KeyPath::size)
                .max()
                .orElse(1);
            val crfId = models
                .values()
                .stream()
                .filter(Model::isCRF)
                .map(Model::rwsName)
                .findAny()
                .orElse(null);
            val partitioned = models
                .values()
                .stream()
                .anyMatch(model -> paths
                    .subSet(model.name())
                    .contains(model.partitionGroup(), true));
            val groupedPatients = JSON.parseObject(S(o.get(PATIENTS)))
                .entrySet()
                .stream()
                .collect(toMap(
                    Map.Entry::getKey,
                    e -> ((JSONArray)e.getValue())
                        .stream()
                        .map(String.class::cast)
                        .collect(toList())));
            createDirectories(directoryPath);
            val filePath = directoryPath.resolve(cfg.projectExportStorageFileName + ".zip");
            SXSSFWorkbook workbook = null;
            try (val out = new FileOutputStream(filePath.toFile())) {
                val zip = new ZipOutputStream(out);
                int line = 0;
                long volume = 1L;
                long volumeSize = 0L;
                long patientSize = 0L;
                SXSSFSheet sheet = null;
                Font boldFont = null;
                Font errorFont = null;
                CellStyle defaultCellStyle = null;
                CellStyle titleCellStyle = null;
                CellStyle errorCellStyle = null;
                localStartTime.set(currentTimeMillis());
                for (val group : groupedPatients.keySet()) {
                    val patients = groupedPatients.get(group);
                    for (val patient : patients) {
                        ++patientSize;
                        if (shouldStop.get()) {
                            throw new CancellationException();
                        }
                        sleep(0);  // break if interrupted
                        if (workbook == null) {
                            zip.putNextEntry(new ZipEntry(fileBaseName + " (" + volume + ").xlsx"));
                            line = HeaderType.FLAT.equals(headerType) ? 1 : layers;
                            workbook = new SXSSFWorkbook(-1);
                            sheet = workbook.createSheet("患者集");
                            boldFont = workbook.createFont();
                            {
                                boldFont.setBold(true);
                            }
                            errorFont = workbook.createFont();
                            {
                                errorFont.setColor(Font.COLOR_RED);
                            }
                            defaultCellStyle = workbook.createCellStyle();
                            {
                                defaultCellStyle.setVerticalAlignment(TOP);
                            }
                            titleCellStyle = workbook.createCellStyle();
                            {
                                titleCellStyle.setVerticalAlignment(BOTTOM);
                                titleCellStyle.setFont(boldFont);
                            }
                            errorCellStyle = workbook.createCellStyle();
                            {
                                errorCellStyle.setVerticalAlignment(TOP);
                                errorCellStyle.setFont(errorFont);
                            }
                            switch (headerType) {
                                case FLAT: {
                                    val row0 = sheet.createRow(0);
                                    for (int i = 0; i < pathList.size(); ++i) {
                                        val path = pathList.get(i);
                                        val modelName = path.firstAsString();
                                        val model = models.get(modelName);
                                        val cell = row0.createCell(i + 3);
                                        cell.setCellValue(toPathString(new KeyPath(model.displayName(), model.fieldInfo(path.keyPathByRemovingFirst()).displayPath)));
                                        cell.setCellStyle(titleCellStyle);
                                        sheet.setDefaultColumnStyle(i + 3, defaultCellStyle);
                                    }
                                    break;
                                }
                                case TREE: {
                                    val rows = IntStream.range(0, layers)
                                        .mapToObj(sheet::createRow)
                                        .collect(toList());
                                    if (!paths.isEmpty()) {
                                        var tree = paths;
                                        val stack = new Stack<Iterator<Comparable>>();
                                        stack.push(tree.subKeys().iterator());
                                        val cols = new Stack<Integer>();
                                        int col = 3;
                                        cols.push(col);
                                        while (!stack.isEmpty()) {
                                            val it = stack.peek();
                                            if (it.hasNext()) {
                                                val e = it.next();
                                                val path = tree.path().keyPathByAppending(e);
                                                val modelName = path.firstAsString();
                                                val model = models.get(modelName);
                                                val cell = rows.get(stack.size() - 1).createCell(col);
                                                cell.setCellValue(path.size() == 1 ?
                                                    model.displayName() :
                                                    model
                                                        .pathDictionary()
                                                        .get(path.keyPathByRemovingFirst())
                                                        .lastAsString());
                                                cell.setCellStyle(titleCellStyle);
                                                tree = tree.subSet(e);
                                                stack.push(tree.subKeys().iterator());
                                                cols.push(col);
                                            } else {
                                                while (!stack.isEmpty() && !stack.peek().hasNext()) {
                                                    if (stack.size() > 1) {
                                                        try {
                                                            // merge upper layer
                                                            sheet.addMergedRegion(
                                                                new CellRangeAddress(
                                                                    stack.size() - 2,
                                                                    tree.isLeaf() ? layers - 1 : stack.size() - 2,
                                                                    cols.peek(),
                                                                    col));
                                                        } catch (IllegalArgumentException ignored) {
                                                            // Merged region D4 must contain 2 or more cells
                                                        }
                                                    }
                                                    stack.pop();
                                                    cols.pop();
                                                    tree = tree.superSet();
                                                }
                                                ++col;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                            {
                                val row0 = sheet.getRow(0);
                                String[] titles = {"组名称", "患者编号", "当前就诊次数/总次数"};
                                for (int i = 0; i < 3; ++i) {
                                    val cell = row0.createCell(i);
                                    cell.setCellValue(titles[i]);
                                    cell.setCellStyle(titleCellStyle);
                                    if (HeaderType.TREE.equals(headerType) && layers > 1) {
                                        sheet.addMergedRegion(new CellRangeAddress(0, layers - 1, i, i));
                                    }
                                }
                            }
                        }
                        val data = new JSONObject();
                        val groups = new HashMap<String, JSONObject>();
                        val groupSns = new HashMap<String, Long>();
                        for (val model : models.values()) {
                            val fields = new KeyPathSet(paths.subSet(model.name()));
                            if (partitioned && model.isPartitioned()) {
                                fields.add(model.partitionGroup()
                                    .keyPathByAppending(model.partitionField()));
                                model.sortFields()
                                    .keySet()
                                    .stream()
                                    .map(model.partitionGroup()::keyPathByAppending)
                                    .forEach(fields::add);
                            }
                            final JSONObject sub;
                            switch (model.sourceType()) {
                                case RWS_SERVICE:
                                    if (!model.isCustom()) {
                                        throw new Exception("Model with " + RWS_SERVICE + " source should be custom.");
                                    }
                                    sub = projectService.computeCustomVariablesValue(
                                        ProjectService.ComputeCustomVariablesValueParameters.builder()
                                            .userId(userId)
                                            .taskId(taskId)
                                            .crfId(crfId)
                                            .projectId(projectId)
                                            .patientSn(patient)
                                            .varIds(fields
                                                .stream()
                                                .map(KeyPath::firstAsString)
                                                .collect(toList()))
                                            .build());
                                    break;
                                case SEARCH_SERVICE:
                                    sub = searchService.fetchPatientData(
                                        SearchService.FetchPatientDataParameters.builder()
                                            .model(model)
                                            .fields(fields)
                                            .patientSn(patient)
                                            .build());
                                    break;
                                case EMPI_SERVICE:
                                    sub = empiService.fetchPatientInfo(
                                        EmpiService.FetchPatientDataParameters.builder()
                                            .fields(fields)
                                            .patientSn(patient)
                                            .build());
                                    break;
                                default:
                                    throw new Exception("Unexpected model source type: " + model.sourceType());
                            }
                            data.put(model.name(), sub);
                            if (partitioned && model.isPartitioned()) {
                                model
                                    .partitionGroup()
                                    .flatFuzzyResolve(sub)
                                    .stream()
                                    .filter(part -> S(model.partitionField().fuzzyResolveFirst(part)) != null)
                                    .forEach(part -> {
                                        val sn = S(model.partitionField().fuzzyResolveFirst(part));
                                        val obj = groups.computeIfAbsent(sn, k -> new JSONObject());
                                        new KeyPath(model.name(), model.partitionGroup()).assign(obj, part);
                                        long time = Long.MAX_VALUE;
                                        for (val sortField : model.sortFields().keySet()) {
                                            val date = DT(sortField.fuzzyResolveFirst(part));
                                            if (date != null) {
                                                time = date.getTime();
                                                break;
                                            }
                                        }
                                        groupSns.put(sn, time);
                                    });
                            }
                        }
                        val sns = groupSns
                            .entrySet()
                            .stream()
                            .sorted(comparing(Map.Entry::getValue))
                            .map(Map.Entry::getKey)
                            .toArray(String[]::new);
                        val row0 = sheet.createRow(line++);
                        {
                            {
                                val cell = row0.createCell(0);
                                cell.setCellValue(group);
                                cell.setCellStyle(defaultCellStyle);
                            }
                            {
                                val cell = row0.createCell(1);
                                cell.setCellValue(patient);
                                cell.setCellStyle(defaultCellStyle);
                            }
                            {
                                val cell = row0.createCell(2);
                                cell.setCellValue("1/" + (partitioned ? groups.size() : 1));
                                cell.setCellStyle(defaultCellStyle);
                            }
                            if (partitioned && groups.size() > 1) {
                                for (int i = 0; i < 2; ++i) {
                                    sheet.addMergedRegion(
                                        new CellRangeAddress(
                                            row0.getRowNum(),
                                            row0.getRowNum() + groups.size() - 1,
                                            i,
                                            i));
                                }
                            }
                        }
                        val rows = new ArrayList<Row>();
                        {
                            rows.add(row0);
                        }
                        if (partitioned) {
                            for (int i = 2; i <= groups.size(); ++i) {
                                val row = sheet.createRow(line++);
                                {
                                    row.createCell(2).setCellValue(i + "/" + groups.size());
                                }
                                rows.add(row);
                            }
                        }
                        for (int i = 0; i < pathList.size(); ++i) {
                            val path = pathList.get(i);
                            val modelName = path.firstAsString();
                            val model = models.get(modelName);
                            val field = path.keyPathByRemovingFirst();
                            val columnIndex = i + 3;
                            if (model.isPartitioned() && field.isPrefixedBy(model.partitionGroup())) {
                                for (int j = 0; j < sns.length; ++j) {
                                    val cell = rows.get(j).createCell(columnIndex);
                                    try {
                                        cell.setCellValue(
                                            path.fuzzyResolve(groups.get(sns[j]))
                                                .stream()
                                                .map(TypeUtil::S1)
                                                .collect(joining(";")));
                                        cell.setCellStyle(defaultCellStyle);
                                    } catch (Exception e) {
                                        cell.setCellValue("数据过长");
                                        cell.setCellStyle(errorCellStyle);
                                    }
                                }
                            } else {
                                val cell = row0.createCell(columnIndex);
                                try {
                                    cell.setCellValue(
                                        path.fuzzyResolve(data)
                                            .stream()
                                            .map(TypeUtil::S1)
                                            .collect(joining(";")));
                                    cell.setCellStyle(defaultCellStyle);
                                } catch (Exception e) {
                                    cell.setCellValue("数据过长");
                                    cell.setCellStyle(errorCellStyle);
                                }
                                if (partitioned && groups.size() > 1) {
                                    sheet.addMergedRegion(
                                        new CellRangeAddress(
                                            row0.getRowNum(),
                                            row0.getRowNum() + groups.size() - 1,
                                            columnIndex,
                                            columnIndex));
                                }
                            }
                        }
                        sheet.flushRows();
                        boolean limitExceeded = false;
                        if (cfg.projectExportStorageVolumeSizeThreshold > 0) {
                            val dataSize = new AtomicLong(0);
                            foreachValue(data, v -> {
                                val s = S(v);
                                if (s != null) {
                                    dataSize.addAndGet(s.getBytes().length);
                                }
                            });
                            volumeSize += dataSize.get();
                            if (volumeSize >= cfg.projectExportStorageVolumeSizeThreshold) {
                                limitExceeded = true;
                            }
                        }
                        if (cfg.projectExportStoragePatientSizeThreshold > 0) {
                            if (patientSize >= cfg.projectExportStoragePatientSizeThreshold) {
                                limitExceeded = true;
                            }
                        }
                        if (limitExceeded) {
                            workbook.write(zip);
                            zip.closeEntry();
                            workbook.dispose();
                            workbook = null;
                            ++volume;
                            volumeSize = 0L;
                            patientSize = 0L;
                        }
                        try {
                            val progress = (float)exportedPatientCount.incrementAndGet() / (float)totalPatientCount.get();
                            db.update(
                                "UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET " +
                                    Q(PROGRESS) + " = ?, " +
                                    Q(ESTIMATED_FINISH_TIME) + " = from_unixtime(ceil((unix_timestamp() - unix_timestamp(" + Q(START_TIME) + ")) / ?) + unix_timestamp(" + Q(START_TIME) + ")) " +
                                    "WHERE " + Q(TASK_ID) + " = ?",
                                progress, progress, taskId);
                        } catch (Exception e) {
                            LOGGER.error(e.getLocalizedMessage(), e);
                        }
                    }
                }
                if (workbook != null) {
                    workbook.write(zip);
                    zip.closeEntry();
                    workbook.dispose();
                }
                zip.finish();
            } catch (Throwable e) {
                if (workbook != null) {
                    suppress(workbook::dispose);
                }
                throw e;
            }
            LOGGER.info("Task " + taskId + " finished.");
            db.update(
                "UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET " +
                    Q(PROGRESS) + " = ?, " +
                    Q(STATE) + " = ?, " +
                    Q(DOWNLOADED) + " = FALSE, " +
                    Q(EXPIRE_NOTIFIED) + " = FALSE, " +
                    Q(FILE_SIZE) + " = ?, " +
                    Q(FINISH_TIME) + " = now(), " +
                    Q(ESTIMATED_FINISH_TIME) + " = NULL, " +
                    Q(LAST_MODIFY_TIME) + " = now() " +
                    "WHERE " + Q(TASK_ID) + " = ?",
                1, FINISHED.value(), Files.size(filePath), taskId);
            def.sendMessage("2201", new JSONObject()
                .fluentPut("user_id", userId)
                .fluentPut("task_id", taskId)
                .fluentPut("project_id", projectId)
                .fluentPut("msg", projectName + "项目的导出到本地任务已完成"));
        } catch (Throwable e) {
            LOGGER.error("Task " + taskId + " failed.", e);
            try {
                if (directoryPath != null) {
                    deleteDirectory(directoryPath.toFile());
                }
            } catch (Throwable ignored) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
            try {
                db.update(
                    "UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET " +
                        Q(PROGRESS) + " = ?, " +
                        Q(STATE) + " = ?, " +
                        Q(DOWNLOADED) + " = NULL, " +
                        Q(EXPIRE_NOTIFIED) + " = NULL, " +
                        Q(LAST_MODIFY_TIME) + " = now(), " +
                        Q(ESTIMATED_FINISH_TIME) + " = NULL " +
                        "WHERE " + Q(TASK_ID) + " = ?",
                    null, FAILED.value(), taskId);
            } catch (Throwable ignored) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
            def.sendMessage("2202", new JSONObject()
                .fluentPut("user_id", userId)
                .fluentPut("task_id", taskId)
                .fluentPut("project_id", projectId)
                .fluentPut("msg", orDefault(projectName, "未知") + "项目的导出到本地任务失败"));
        }
    }

    private final GeneralConfiguration cfg = getBean(GeneralConfiguration.class);
    private final JdbcTemplate db = getBean(DatabaseService.class).jdbcTemplate();
    private final ProjectExportTaskDefinitions def = getBean(ProjectExportTaskDefinitions.class);

    private final ProjectService projectService = getBean(ProjectService.class);
    private final SearchService searchService = getBean(SearchService.class);
    private final EmpiService empiService = getBean(EmpiService.class);

}
