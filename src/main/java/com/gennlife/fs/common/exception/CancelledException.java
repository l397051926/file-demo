package com.gennlife.fs.common.exception;

public class CancelledException extends ResponseException {

    public CancelledException() {
        super();
    }

    public CancelledException(String message) {
        super(message);
    }

    public CancelledException(String message, Throwable cause) {
        super(message, cause);
    }

    public CancelledException(Throwable cause) {
        super(cause);
    }

    @Override
    protected ResponseCode code() {
        return ResponseCode.CANCELLED;
    }

}
