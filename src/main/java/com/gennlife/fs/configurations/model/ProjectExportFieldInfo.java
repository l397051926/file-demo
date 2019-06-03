package com.gennlife.fs.configurations.model;

import lombok.Builder;

@Builder
public class ProjectExportFieldInfo {

    public int index;
    public boolean selectedByDefault;
    public boolean mergeCells;
    public boolean sorted;

}
