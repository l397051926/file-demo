package com.gennlife.fs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.collection.keypath.KeyPathSet;
import com.gennlife.fs.common.configurations.GeneralConfiguration;
import com.gennlife.fs.common.configurations.HeaderType;
import com.gennlife.fs.common.configurations.Model;
import com.gennlife.fs.common.utils.TypeUtil;
import lombok.Builder;
import lombok.val;
import lombok.var;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.gennlife.darren.controlflow.exception.Try.try_;
import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachValue;
import static com.gennlife.fs.common.configurations.Model.CUSTOM_MODEL_NAME;
import static com.gennlife.fs.common.configurations.Model.modelByName;
import static com.gennlife.fs.common.configurations.TaskState.*;
import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;
import static com.gennlife.fs.common.utils.DBUtils.P;
import static com.gennlife.fs.common.utils.DBUtils.Q;
import static com.gennlife.fs.common.utils.KeyPathUtil.toPathString;
import static com.gennlife.fs.common.utils.TypeUtil.*;
import static com.gennlife.fs.service.ProjectExportTaskDefinitions.*;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.createDirectories;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.poi.ss.usermodel.CellStyle.VERTICAL_BOTTOM;
import static org.apache.poi.ss.usermodel.CellStyle.VERTICAL_TOP;

public class ProjectExportTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectExportTask.class);

    public AtomicBoolean shouldStop = new AtomicBoolean(false);

    @Builder
    public static class ProjectExportTaskParameters {
        String userId;
        Long taskId;
    }

    ProjectExportTask(ProjectExportTaskParameters params) {
        this.params = params;
    }

    @Override
    public void run() {
        Path directoryPath = null;
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
                RUNNING.value(), 0, params.taskId);
            directoryPath = def.dir(params.userId, params.taskId);
            val o = db.queryForMap(
                "SELECT " + P(FILE_NAME, HEADER_TYPE, PROJECT_ID, PROJECT_NAME, FILE_NAME, MODELS, SELECTED_FIELDS, CUSTOM_VARS, PATIENTS, PATIENT_COUNT) +
                    " FROM " + Q(cfg.projectExportTaskDatabaseTable) + " WHERE " + Q(TASK_ID) + " = ?",
                params.taskId);
            val headerType = HeaderType.withValue(I(o.get(HEADER_TYPE)));
            projectName = S(o.get(PROJECT_NAME));
            projectId = S(o.get(PROJECT_ID));
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
                    .sorted(comparingInt(field -> model.fieldInfo(field).index))
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
            val patientCount = L(o.get(PATIENT_COUNT));
            createDirectories(directoryPath);
            val filePath = directoryPath.resolve(cfg.projectExportStorageFileName + ".zip");
            try (val out = new FileOutputStream(filePath.toFile())) {
                val zip = new ZipOutputStream(out);
                int line = 0;
                int count = 0;
                int volume = 1;
                int volumeSize = 0;
                Workbook workbook = null;
                Sheet sheet = null;
                Font boldFont = null;
                Font errorFont = null;
                CellStyle defaultCellStyle = null;
                CellStyle titleCellStyle = null;
                CellStyle errorCellStyle = null;
                for (val group : groupedPatients.keySet()) {
                    val patients = groupedPatients.get(group);
                    for (val patient : patients) {
                        if (shouldStop.get()) {
                            throw new CancellationException();
                        }
                        sleep(0);  // break if interrupted
                        if (workbook == null) {
                            line = HeaderType.FLAT.equals(headerType) ? 1 : layers;
                            workbook = new XSSFWorkbook();
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
                                defaultCellStyle.setVerticalAlignment(VERTICAL_TOP);
                            }
                            titleCellStyle = workbook.createCellStyle();
                            {
                                titleCellStyle.setVerticalAlignment(VERTICAL_BOTTOM);
                                titleCellStyle.setFont(boldFont);
                            }
                            errorCellStyle = workbook.createCellStyle();
                            {
                                errorCellStyle.setVerticalAlignment(VERTICAL_TOP);
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
                                                        // merge upper layer
                                                        sheet.addMergedRegion(
                                                            new CellRangeAddress(
                                                                stack.size() - 2,
                                                                tree.isLeaf() ? layers - 1 : stack.size() - 2,
                                                                cols.peek(),
                                                                col));
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
                                fields.add(model.partitionGroup()
                                    .keyPathByAppending(model.sortField()));
                            }
                            val sub = model.isCustom() ?
                                projectService.computeCustomVariablesValue(
                                    ProjectService.ComputeCustomVariablesValueParameters.builder()
                                        .userId(params.userId)
                                        .taskId(params.taskId)
                                        .crfId(crfId)
                                        .projectId(projectId)
                                        .patientSn(patient)
                                        .varIds(fields
                                            .stream()
                                            .map(KeyPath::firstAsString)
                                            .collect(toList()))
                                        .build()) :
                                searchService.fetchPatientData(
                                    SearchService.FetchPatientDataParameters.builder()
                                        .model(model)
                                        .fields(fields)
                                        .patientSn(patient)
                                        .build());
                            data.put(model.name(), sub);
                            if (partitioned && model.isPartitioned()) {
                                model
                                    .partitionGroup()
                                    .flatFuzzyResolve(sub)
                                    .stream()
                                    .filter(part ->
                                        S(model.partitionField().fuzzyResolveFirst(part)) != null &&
                                            DT(model.sortField().fuzzyResolveFirst(part)) != null)
                                    .forEach(part -> {
                                        val sn = S(model.partitionField().fuzzyResolveFirst(part));
                                        val obj = groups.computeIfAbsent(sn, k -> new JSONObject());
                                        new KeyPath(model.name(), model.partitionGroup()).assign(obj, part);
                                        groupSns.put(sn, DT(model.sortField().fuzzyResolveFirst(part)).getTime());
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
                        val dataSize = new AtomicLong(0);
                        foreachValue(data, v -> dataSize.addAndGet(S(v).getBytes().length));
                        volumeSize += dataSize.get();
                        if (cfg.projectExportStorageVolumeSizeThreshold > 0 && volumeSize >= cfg.projectExportStorageVolumeSizeThreshold) {
                            writeZipExcelEntry(zip, fileBaseName + " (" + volume + ").xlsx", workbook);
                            workbook = null;
                            volumeSize = 0;
                            ++volume;
                        }
                        try {
                            val progress = (float)(++count) / (float)patientCount;
                            db.update(
                                "UPDATE " + Q(cfg.projectExportTaskDatabaseTable) + " SET " +
                                    Q(PROGRESS) + " = ?, " +
                                    Q(ESTIMATED_FINISH_TIME) + " = from_unixtime(ceil((unix_timestamp() - unix_timestamp(" + Q(START_TIME) + ")) / ?) + unix_timestamp(" + Q(START_TIME) + ")) " +
                                    "WHERE " + Q(TASK_ID) + " = ?",
                                progress, progress, params.taskId);
                        } catch (Exception e) {
                            LOGGER.error(e.getLocalizedMessage(), e);
                        }
                    }
                }
                if (workbook != null && volumeSize > 0) {
                    writeZipExcelEntry(zip, fileBaseName + " (" + volume + ").xlsx", workbook);
                }
                zip.finish();
            }
            LOGGER.info("Task " + params.taskId + " finished.");
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
                1, FINISHED.value(), Files.size(filePath), params.taskId);
            def.sendMessage("2201", new JSONObject()
                .fluentPut("user_id", params.userId)
                .fluentPut("task_id", params.taskId)
                .fluentPut("project_id", projectId)
                .fluentPut("msg", projectName + "项目的导出到本地任务已完成"));
        } catch (Exception e) {
            LOGGER.error("Task " + params.taskId + " failed.", e);
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
                    null, FAILED.value(), params.taskId);
                if (directoryPath != null) {
                    deleteDirectory(directoryPath.toFile());
                }
                def.sendMessage("2202", new JSONObject()
                    .fluentPut("user_id", params.userId)
                    .fluentPut("task_id", params.taskId)
                    .fluentPut("project_id", projectId)
                    .fluentPut("msg", orDefault(projectName, "未知") + "项目的导出到本地任务失败"));
            } catch (Exception ignored) {
                // TODO: Add implementation.
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        } finally {
            TASKS.remove(params.taskId);
        }
    }

    private static void writeZipExcelEntry(ZipOutputStream zip, String fileName, Workbook workbook) throws IOException {
        zip.putNextEntry(new ZipEntry(fileName));
        {
            val buf = new ByteArrayOutputStream();
            workbook.write(buf);
            buf.writeTo(zip);
        }
        zip.closeEntry();
    }

    private ProjectExportTaskParameters params;

    private final GeneralConfiguration cfg = getBean(GeneralConfiguration.class);
    private final ProjectExportTaskDefinitions def = getBean(ProjectExportTaskDefinitions.class);
    private final ProjectService projectService = getBean(ProjectService.class);
    private final SearchService searchService = getBean(SearchService.class);
    private final JdbcTemplate db = getBean(DatabaseService.class).jdbcTemplate();

}
