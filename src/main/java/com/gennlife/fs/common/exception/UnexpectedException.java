package com.gennlife.fs.common.exception;

public class UnexpectedException extends ResponseException {

    public UnexpectedException() {
        super();
    }

    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedException(Throwable cause) {
        super(cause);
    }

    @Override
    protected ResponseCode code() {
        return ResponseCode.UNEXPECTED;
    }

}
