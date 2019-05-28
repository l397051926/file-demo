package com.gennlife.fs.configurations.projectexport;

import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.collection.keypath.KeyPathSet;
import com.gennlife.darren.excel.ExcelTitle;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.gennlife.darren.excel.ExcelFileExtension.XLSX;
import static com.gennlife.darren.excel.ExcelSheetHelper.loadRequestObjects;
import static com.gennlife.darren.excel.ExcelWorkbookHelper.read;
import static com.gennlife.fs.configurations.projectexport.Model.*;
import static java.util.stream.Collectors.*;

@Component
@Scope("singleton")
@PropertySource("classpath:general.properties")
class _DefaultModelsGenerator {

    private static class Line {
        @ExcelTitle("组序号") int groupOrdinal;
        @ExcelTitle("组中文名") String groupDisplayName;
        @ExcelTitle("组英文名") String groupName;
        @ExcelTitle("字段序号") int fieldOrdinal;
        @ExcelTitle("列中文名") String fieldDisplayName;
        @ExcelTitle("列英文名") String fieldName;
        @ExcelTitle("是否支持导出") boolean exportSupported;
        @ExcelTitle("是否默认选中") boolean selectedByDefault;
        @ExcelTitle("是否合并单元格") boolean mergeCells;
        @ExcelTitle("就诊排序字段") boolean sorted;
        KeyPath displayPath() {
            return KeyPath.compile(groupDisplayName).keyPathByAppending(KeyPath.compile(fieldDisplayName));
        }
        KeyPath path() {
            return KeyPath.compile(groupName).keyPathByAppending(KeyPath.compile(fieldName));
        }
    }

    @Autowired
    private void setEnv(Environment env) throws Exception {
        val resolver = new RelaxedPropertyResolver(env, "gennlife.fs.project-export.model.");
        val version = resolver.getProperty("version");
        val workbook = read(getClass().getResourceAsStream("/configurations/project-export/model/" + version + ".xlsx"), XLSX);
        val modelNames = resolver.getSubProperties("type.")
            .keySet()
            .stream()
            .map(rem -> rem.split("\\.")[0])
            .collect(toSet());
        modelNames.remove(CUSTOM_MODEL_NAME);
        CUSTOM_MODEL_DISPLAY_NAME = resolver.getProperty("type.custom.display-name");
        for (val modelName: modelNames) {
            val prefix = "type." + modelName + ".";
            val sheetTitle = resolver.getProperty(prefix + "sheet-title");
            val lines = loadRequestObjects(workbook.getSheet(sheetTitle), Line.class, 2, false);
            val model = new Model();
            model._name = modelName;
            model._rwsName = resolver.getProperty(prefix + "rws-name");
            model._indexName = resolver.getProperty(prefix + "index-name");
            model._displayName = resolver.getProperty(prefix + "display-name");
            model._patientSnField = KeyPath.compile(resolver.getProperty(prefix + "patient-sn-field"));
            model._partitionGroup = KeyPath.compile(resolver.getProperty(prefix + "partition-group"));
            model._partitionField = KeyPath.compile(resolver.getProperty(prefix + "partition-field"));
            model._sortFields = Stream
                .of(resolver.getProperty(prefix + "sort-fields").split(","))
                .map(KeyPath::compile)
                .collect(toCollection(() -> new KeyPathSet(LinkedHashMap::new)));
            model._allFieldInfo = IntStream.range(0, lines.size())
                .boxed()
                .collect(toMap(
                    i -> lines.get(i).path(),
                    i -> {
                        val line = lines.get(i);
                        return FieldInfo.builder()
                            .path(line.path())
                            .displayPath(line.displayPath())
                            .groupOrdinal(line.groupOrdinal)
                            .fieldOrdinal(line.fieldOrdinal)
                            .index(i)
                            .exportSupported(line.exportSupported)
                            .selectedByDefault(line.selectedByDefault)
                            .mergeCells(line.mergeCells)
                            .sorted(line.sorted)
                            .build();
                    },
                    (a, b) -> a,
                    LinkedHashMap::new));
            model.generateCaches();
            MODELS.put(modelName, model);
            MODELS_RWS.put(model.rwsName(), model);
        }
    }

}
