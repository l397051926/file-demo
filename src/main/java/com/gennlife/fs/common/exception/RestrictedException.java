package com.gennlife.fs.common.exception;

public class RestrictedException extends ResponseException {

    public RestrictedException() {
        super();
    }

    public RestrictedException(String message) {
        super(message);
    }

    public RestrictedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestrictedException(Throwable cause) {
        super(cause);
    }

    @Override
    protected ResponseCode code() {
        return ResponseCode.RESTRICTED;
    }

}
