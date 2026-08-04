package com.tasktriage.backend.task;

public class InvalidTaskTransitionException extends RuntimeException {

    public InvalidTaskTransitionException(TaskStatus from, TaskStatus to) {
        super("Cannot transition task status from " + from + " to " + to);
    }
}
