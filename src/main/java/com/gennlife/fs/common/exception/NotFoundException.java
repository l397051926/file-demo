package com.gennlife.fs.common.exception;

public class NotFoundException extends ResponseException {

    public NotFoundException() {
        super();
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(Throwable cause) {
        super(cause);
    }

    @Override
    protected ResponseCode code() {
        return ResponseCode.NOT_FOUND;
    }

}
