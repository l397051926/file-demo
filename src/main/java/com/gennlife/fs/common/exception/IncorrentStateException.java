package com.gennlife.fs.common.exception;

public class IncorrentStateException extends ResponseException {

    public IncorrentStateException() {
        super();
    }

    public IncorrentStateException(String message) {
        super(message);
    }

    public IncorrentStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncorrentStateException(Throwable cause) {
        super(cause);
    }

    @Override
    protected ResponseCode code() {
        return ResponseCode.INCORRECT_STATE;
    }

}
