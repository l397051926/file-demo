package com.gennlife.fs.configurations.projectexport;

import java.beans.PropertyEditorSupport;

import static com.gennlife.darren.util.Constants.STRING_NULL_VALUE;

public class ModelVersionEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        super.setValue(new ModelVersion(text));
    }

    @Override
    public String getAsText() {
        ModelVersion value = (ModelVersion)super.getValue();
        return value != null ? value.toString() : STRING_NULL_VALUE;
    }

}
