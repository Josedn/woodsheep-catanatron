package com.catanatron.core.model;

public final class ActionRecord<R> {
    public final Action<?> action;
    public final R result;

    public ActionRecord(Action<?> action, R result) {
        this.action = action;
        this.result = result;
    }
}

