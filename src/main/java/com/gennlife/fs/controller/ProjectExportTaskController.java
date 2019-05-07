package com.gennlife.fs.controller;

import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.collection.keypath.KeyPathSet;
import com.gennlife.darren.util.GenericTypeConverters;
import com.gennlife.fs.service.ProjectExportTaskService;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

import static com.gennlife.darren.controlflow.exception.Try.try_;
import static com.gennlife.fs.common.utils.KeyPathUtil.toKeyPath;
import static com.gennlife.fs.service.ProjectExportTaskService.*;
import static java.util.stream.Collectors.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@RequestMapping(ProjectExportTaskController.PROJECT_EXPORT_TASK_API_PATH)
public class ProjectExportTaskController extends ControllerBase {

    @RequestMapping(value = PROJECT_EXPORT_TASK_CREATE_SUB_API_PATH, method = POST, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String create(@RequestBody String request) {
        return run(request, o -> {
            val params = CreateParameters.builder()
                .userId(o.getString("userId"))
                .projectId(o.getString("projectId"))
                .build();
            return service.create(params);
        });
    }

    @RequestMapping(value = PROJECT_EXPORT_TASK_INFO_SUB_API_PATH, method = POST, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String info(@RequestBody String request) {
        return run(request, o -> {
            val params = InfoParameters.builder()
                .userId(o.getString("userId"))
                .taskId(o.getLong("taskId"))
                .fetchModel(o.getBooleanValue("fetchModel"))
                .build();
            return service.info(params);
        });
    }

    @RequestMapping(value = PROJECT_EXPORT_TASK_SAVE_INFO_SUB_API_PATH, method = POST, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String saveInfo(@RequestBody String request) {
        return run(request, o -> {
            val params = SaveInfoParameters.builder()
                .userId(o.getString("userId"))
                .taskId(o.getLong("taskId"))
                .fileName(o.getString("fileName"))
                .headerType(o.getInteger("headerType"))
                .selectedFields(try_(
                    () -> o.getJSONArray("selectedFields")
                        .stream()
                        .map(String.class::cast)
                        .map(KeyPath::compile)
                        .collect(toCollection(KeyPathSet::new)))
                    .orElse(null))
                .visitTypes(try_(
                    () -> o.getJSONArray("visitTypes")
                        .stream()
                        .map(GenericTypeConverters::toIntValue)
                        .collect(toSet()))
                    .orElse(null))
                .build();
            return service.saveInfo(params);
        });
    }

    @RequestMapping(value = PROJECT_EXPORT_TASK_START_SUB_API_PATH, method = POST, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String start(@RequestBody String request) {
        return run(request, o -> {
            val params = StartParameters.builder()
                .userId(o.getString("userId"))
                .taskId(o.getLong("taskId"))
                .build();
            return dispatch(service.executor(params.taskId),
                PROJECT_EXPORT_TASK_API_PATH + PROJECT_EXPORT_TASK_START_SUB_API_PATH, o,
                service::start, params);
        });
    }

    @RequestMapping(value = PROJECT_EXPORT_TASK_CANCEL_SUB_API_PATH, method = POST, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String cancel(@RequestBody String request) {
        return run(request, o -> {
            val params = CancelParameters.builder()
                .userId(o.getString("userId"))
                .taskId(o.getLong("taskId"))
                .build();
            return dispatch(service.executor(params.taskId),
                PROJECT_EXPORT_TASK_API_PATH + PROJECT_EXPORT_TASK_CANCEL_SUB_API_PATH, o,
                service::cancel, params);
        });
    }

    @RequestMapping(value = PROJECT_EXPORT_TASK_CANCEL_BY_PROJECT_ID_SUB_API_PATH, method = POST, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String cancelByProjectId(@RequestBody String request) {
        return run(request, o -> {
            val params = CancelByProjectIdParameters.builder()
                .userId(o.getString("userId"))
                .projectId(o.getString("projectId"))
                .build();
            return service.cancelByProjectId(params);
        });
    }

    @RequestMapping(value = PROJECT_EXPORT_TASK_RETRY_SUB_API_PATH, method = POST, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String retry(@RequestBody String request) {
        return run(request, o -> {
            val params = RetryParameters.builder()
                .userId(o.getString("userId"))
                .taskId(o.getLong("taskId"))
                .build();
            return dispatch(service.executor(params.taskId),
                PROJECT_EXPORT_TASK_API_PATH + PROJECT_EXPORT_TASK_RETRY_SUB_API_PATH, o,
                service::retry, params);
        });
    }

    @RequestMapping(value = PROJECT_EXPORT_TASK_DELETE_SUB_API_PATH, method = POST, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String delete(@RequestBody String request) {
        return run(request, o -> {
            val params = DeleteParameters.builder()
                .userId(o.getString("userId"))
                .taskId(o.getLong("taskId"))
                .build();
            return dispatch(service.executor(params.taskId),
                PROJECT_EXPORT_TASK_API_PATH + PROJECT_EXPORT_TASK_DELETE_SUB_API_PATH, o,
                service::delete, params);
        });
    }

    @RequestMapping(value = PROJECT_EXPORT_TASK_DOWNLOAD_SUB_API_PATH, method = GET)
    public void download(@RequestParam("userId") String userId, @RequestParam("taskId") Long taskId, HttpServletResponse response) throws Exception {
        run(new JSONObject()
                .fluentPut("userId", userId)
                .fluentPut("taskId", taskId)
                .toJSONString(),
            response,
            o -> {
                val params = DownloadParameters.builder()
                    .userId(o.getString("userId"))
                    .taskId(o.getLong("taskId"))
                    .response(response)
                    .build();
                service.download(params);
            });
    }

    @RequestMapping(value = PROJECT_EXPORT_TASK_LIST_SUB_API_PATH, method = POST, produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    String list(@RequestBody String request) {
        return run(request, o -> {
            val params = ListParameters.builder()
                .createTimeFrom(toKeyPath("createTime.from").tryResolveAsLong(o))
                .createTimeTo(toKeyPath("createTime.to").tryResolveAsLong(o))
                .downloaded(toKeyPath("downloaded").tryResolveAsBoolean(o))
                .estimatedFinishTimeFrom(toKeyPath("estimatedFinishTime.from").tryResolveAsLong(o))
                .estimatedFinishTimeTo(toKeyPath("estimatedFinishTime.to").tryResolveAsLong(o))
                .fileName(toKeyPath("fileName").tryResolveAsString(o))
                .fileSizeFrom(toKeyPath("fileSize.from").tryResolveAsLong(o))
                .fileSizeTo(toKeyPath("fileSize.to").tryResolveAsLong(o))
                .finishTimeFrom(toKeyPath("finishTime.from").tryResolveAsLong(o))
                .finishTimeTo(toKeyPath("finishTime.to").tryResolveAsLong(o))
                .headerType(toKeyPath("headerType").tryResolveAsInteger(o))
                .progressFrom(toKeyPath("progress.from").tryResolveAsFloat(o))
                .progressTo(toKeyPath("progress.to").tryResolveAsFloat(o))
                .lastModifyTimeFrom(toKeyPath("lastModifyTime.from").tryResolveAsLong(o))
                .lastModifyTimeTo(toKeyPath("lastModifyTime.to").tryResolveAsLong(o))
                .patientCountFrom(toKeyPath("patientCount.from").tryResolveAsLong(o))
                .patientCountTo(toKeyPath("patientCount.to").tryResolveAsLong(o))
                .page(toKeyPath("page").tryResolveAsLong(o))
                .pageSize(toKeyPath("pageSize").tryResolveAsLong(o))
                .projectId(toKeyPath("projectId").tryResolveAsString(o))
                .projectName(toKeyPath("projectName").tryResolveAsString(o))
                .startTimeFrom(toKeyPath("startTime.from").tryResolveAsLong(o))
                .startTimeTo(toKeyPath("startTime.to").tryResolveAsLong(o))
                .sortedBys(Optional.ofNullable(toKeyPath("sortedBys").tryResolveAsJSONObject(o))
                    .map(obj -> obj.entrySet()
                        .stream()
                        .collect(toMap(
                            Map.Entry::getKey,
                            e -> "asc".equalsIgnoreCase(GenericTypeConverters.toString(e.getValue())))))
                    .orElse(null))
                .state(toKeyPath("state").tryResolveAsInteger(o))
                .taskId(toKeyPath("taskId").tryResolveAsLong(o))
                .userId(toKeyPath("userId").tryResolveAsString(o))
                .build();
            return service.list(params);
        });
    }

    public static final String PROJECT_EXPORT_TASK_API_PATH = "/Projects/Export/Task";
    public static final String PROJECT_EXPORT_TASK_CREATE_SUB_API_PATH = "/Create";
    public static final String PROJECT_EXPORT_TASK_INFO_SUB_API_PATH = "/Info";
    public static final String PROJECT_EXPORT_TASK_SAVE_INFO_SUB_API_PATH = "/SaveInfo";
    public static final String PROJECT_EXPORT_TASK_START_SUB_API_PATH = "/Start";
    public static final String PROJECT_EXPORT_TASK_CANCEL_SUB_API_PATH = "/Cancel";
    public static final String PROJECT_EXPORT_TASK_CANCEL_BY_PROJECT_ID_SUB_API_PATH = "/CancelByProjectId";
    public static final String PROJECT_EXPORT_TASK_RETRY_SUB_API_PATH = "/Retry";
    public static final String PROJECT_EXPORT_TASK_DELETE_SUB_API_PATH = "/Delete";
    public static final String PROJECT_EXPORT_TASK_DOWNLOAD_SUB_API_PATH = "/Download";
    public static final String PROJECT_EXPORT_TASK_LIST_SUB_API_PATH = "/List";

    @Autowired
    private ProjectExportTaskService service;

}
