package com.stetits.core.stack;

import java.util.List;

public record StackPlan(
        String stackId,
        String version,
        List<Action> actions
) {
    public record Action(String kind, String target, String detail) {}
}
