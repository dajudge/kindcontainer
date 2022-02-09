package com.dajudge.kindcontainer.exception;

import org.testcontainers.containers.Container.ExecResult;

import static java.util.Arrays.asList;

public class ExecutionException extends Exception {
    private final String[] commandLine;
    private final ExecResult result;

    public ExecutionException(final String[] commandLine, final ExecResult result) {
        super("Failed to execute command: " + asList(commandLine) + "\n" + result.getStderr());
        this.commandLine = commandLine;
        this.result = result;
    }

    public String[] getCommandLine() {
        return commandLine;
    }

    public ExecResult getResult() {
        return result;
    }
}
