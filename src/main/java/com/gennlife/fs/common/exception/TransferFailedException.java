package com.gennlife.fs.common.exception;

public class TransferFailedException extends ResponseException {

    public TransferFailedException() {
        super();
    }

    public TransferFailedException(String message) {
        super(message);
    }

    public TransferFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransferFailedException(Throwable cause) {
        super(cause);
    }

    @Override
    protected ResponseCode code() {
        return ResponseCode.TRANSFER_FAILED;
    }

}
