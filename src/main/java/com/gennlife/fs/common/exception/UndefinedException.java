package com.gennlife.fs.common.exception;

public class UndefinedException extends ResponseException {

    public UndefinedException() {
        super();
    }

    public UndefinedException(String message) {
        super(message);
    }

    public UndefinedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UndefinedException(Throwable cause) {
        super(cause);
    }

    @Override
    protected ResponseCode code() {
        return ResponseCode.UNDEFINED;
    }

}
