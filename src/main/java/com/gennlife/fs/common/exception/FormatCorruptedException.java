package com.gennlife.fs.common.exception;

public class FormatCorruptedException extends ResponseException {

    public FormatCorruptedException() {
        super();
    }

    public FormatCorruptedException(String message) {
        super(message);
    }

    public FormatCorruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormatCorruptedException(Throwable cause) {
        super(cause);
    }

    @Override
    protected ResponseCode code() {
        return ResponseCode.FORMAT_CORRUPTED;
    }

}
