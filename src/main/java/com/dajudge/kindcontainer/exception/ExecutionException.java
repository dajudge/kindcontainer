/*
Copyright 2020-2021 Alex Stockinger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
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
