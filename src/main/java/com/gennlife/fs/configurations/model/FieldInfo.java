package com.gennlife.fs.configurations.model;

import com.gennlife.darren.collection.keypath.KeyPath;
import lombok.Builder;

import java.time.format.DateTimeFormatter;

@Builder
public class FieldInfo {

    public KeyPath path;
    public KeyPath displayPath;
    public DataType type;

    // if type == DATE
    public String dateFormat;
    public DateTimeFormatter dateFormatter;

    // if supports project export
    public ProjectExportFieldInfo projectExport;

    public boolean supportsProjectExport() {
        return projectExport != null;
    }

}
