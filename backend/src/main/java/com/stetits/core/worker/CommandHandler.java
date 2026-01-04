package com.stetits.core.worker;

public interface CommandHandler {
    String type();                 // ex: "APPLY_STACK_VERSION"
    void execute(CommandContext ctx) throws Exception;
}