package com.gennlife.fs.common.configurations;

import com.gennlife.darren.collection.keypath.KeyPath;
import lombok.Builder;

@Builder
public class FieldInfo {
    public int groupOrdinal;
    public int fieldOrdinal;
    public int index;
    public KeyPath path;
    public KeyPath displayPath;
    public boolean exportSupported;
    public boolean selectedByDefault;
    public boolean mergeCells;
    public boolean sorted;
}
