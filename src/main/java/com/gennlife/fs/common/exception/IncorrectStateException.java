package com.gennlife.fs.common.exception;

public class IncorrectStateException extends ResponseException {

    public IncorrectStateException() {
        super();
    }

    public IncorrectStateException(String message) {
        super(message);
    }

    public IncorrectStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncorrectStateException(Throwable cause) {
        super(cause);
    }

    @Override
    protected ResponseCode code() {
        return ResponseCode.INCORRECT_STATE;
    }

}
