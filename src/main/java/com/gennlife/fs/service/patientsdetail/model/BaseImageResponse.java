package com.gennlife.fs.service.patientsdetail.model;

import com.gennlife.fs.common.response.ResponseInterface;
import com.google.gson.JsonObject;

/**
 * Created by Chenjinfeng on 2016/11/2.
 */
public abstract class BaseImageResponse  implements ResponseInterface, ImageInterface {
    protected ResponseInterface template = null;
    protected JsonObject result = null;
    protected String[] imagekeys;

    public BaseImageResponse(ResponseInterface template) {
        this.template = template;
    }

    @Override
    public String get_error() {
        return template.get_error();
    }

    @Override
    public JsonObject get_result() {
        return result;
    }

    @Override
    public void setResult(JsonObject result) {
        this.result = result;
    }

    @Override
    public void setError(String error) {
        template.setError(error);
    }

    @Override
    public void execute(JsonObject param) {
        template.execute(param);
        result = template.get_result();
        if (result != null) {
            setImageData();
            removeEmpty(getimagekeys());
        }

    }
    public abstract void setImageData();
    @Override
    public String[] getimagekeys() {
        return imagekeys;
    }

    @Override
    public void setimagekeys(String... imagekeys) {
        this.imagekeys = imagekeys;

    }
}
