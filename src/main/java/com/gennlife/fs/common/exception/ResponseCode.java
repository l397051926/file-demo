package com.gennlife.fs.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ResponseCode {

    OK(0, "No error."),
    UNIMPLEMENTED(1, "Not yet implemented."),
    UNEXPECTED(2, "Unexpected error."),
    TRANSFER_FAILED(3, "Transfer failed."),
    FORMAT_CORRUPTED(4, "Format corrupted."),
    NOT_FOUND(5, "Not found."),
    INCORRECT_STATE(6, "Incorrect state."),
    RESTRICTED(7, "Operation is not allowed."),
    CANCELLED(8, "Operation has been cancelled."),
    LIMIT_EXCEEDED(9, "Limit exceeded."),
    UNDEFINED(Integer.MAX_VALUE, "Undefined error.");

    @Getter private int value;
    @Getter private String description;

}
