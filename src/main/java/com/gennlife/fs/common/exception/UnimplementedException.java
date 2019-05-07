package com.gennlife.fs.common.exception;

public class UnimplementedException extends ResponseException {

    @Override
    protected ResponseCode code() {
        return ResponseCode.UNIMPLEMENTED;
    }

}
