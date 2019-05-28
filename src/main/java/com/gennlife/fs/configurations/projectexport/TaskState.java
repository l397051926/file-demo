package com.gennlife.fs.configurations.projectexport;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public enum TaskState {

    PENDING(1),
    QUEUING(2),
    RUNNING(3),
    FAILED(4),
    FINISHED(5),
    EXPIRED(6);

    TaskState(int value) {
        _value = value;
    }

    public int value() {
        return _value;
    }

    public static TaskState withValue(int value) {
        return _enums.get(value);
    }

    private int _value;

    private static final Map<Integer, TaskState> _enums = Stream.of(TaskState.values())
        .collect(toMap(TaskState::value, identity()));

}
