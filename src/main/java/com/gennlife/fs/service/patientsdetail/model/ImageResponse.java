package com.gennlife.fs.service.patientsdetail.model;

import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.google.gson.JsonObject;

/**
 * Created by Chenjinfeng on 2016/11/2.
 */
public class ImageResponse extends PatientDetailService implements ResponseInterface, ImageInterface {
    ResponseInterface template = null;
    JsonObject result = null;
    private String[] imagekeys;

    public ImageResponse(ResponseInterface template) {
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
            removeEmpty(getimagekeys());
        }

    }

    @Override
    public String[] getimagekeys() {
        return imagekeys;
    }

    @Override
    public void setimagekeys(String... imagekeys) {
        this.imagekeys = imagekeys;

    }
}
