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
