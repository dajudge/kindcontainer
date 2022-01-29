/*
Copyright 2020-2022 Alex Stockinger

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
package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer.ExecInContainer;
import com.dajudge.kindcontainer.BaseSidecarContainer.FileTarget;
import com.dajudge.kindcontainer.exception.ExecutionException;
import org.testcontainers.images.builder.Transferable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApplyFluent {
    private final ExecInContainer exec;
    private final FileTarget fileTarget;
    private final List<Runnable> preExecutionRunnables = new ArrayList<>();
    private String namespace;

    ApplyFluent(final ExecInContainer exec, final FileTarget fileTarget) {
        this.exec = exec;
        this.fileTarget = fileTarget;
    }

    public ApplyFluent namespace(final String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ApplyFluent withFile(final Transferable file, final String path) {
        preExecutionRunnables.add(() -> fileTarget.copyFileToContainer(file, path));
        return this;
    }

    public void run(final String path) throws IOException, ExecutionException, InterruptedException {
        final List<String> cmdline = new ArrayList<>();
        cmdline.add("kubectl");
        cmdline.add("apply");
        if (namespace != null) {
            cmdline.add("--namespace");
            cmdline.add(namespace);
        }
        cmdline.add("-f");
        cmdline.add(path);
        preExecutionRunnables.forEach(Runnable::run);
        exec.safeExecInContainer(cmdline.toArray(new String[]{}));
    }
}
