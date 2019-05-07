package com.gennlife.fs.common.exception;

public class LimitExceededException extends ResponseException {

    public LimitExceededException() {
        super();
    }

    public LimitExceededException(String message) {
        super(message);
    }

    public LimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimitExceededException(Throwable cause) {
        super(cause);
    }

    @Override
    protected ResponseCode code() {
        return ResponseCode.LIMIT_EXCEEDED;
    }

}
