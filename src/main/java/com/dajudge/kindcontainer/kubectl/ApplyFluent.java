package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer.ExecInContainer;
import com.dajudge.kindcontainer.BaseSidecarContainer.FileTarget;
import com.dajudge.kindcontainer.exception.ExecutionException;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApplyFluent {
    private final ExecInContainer exec;
    private final FileTarget fileTarget;
    private final List<Runnable> preExecutionRunnables = new ArrayList<>();
    private final List<String> files = new ArrayList<>();
    private String namespace;

    ApplyFluent(final ExecInContainer exec, final FileTarget fileTarget) {
        this.exec = exec;
        this.fileTarget = fileTarget;
    }

    public ApplyFluent namespace(final String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ApplyFluent fileFromClasspath(final String resourceName) {
        final String path = "/tmp/classpath:" + resourceName
                .replaceAll("_", "__")
                .replaceAll("[^a-zA-Z0-9.]", "_");
        files.add(path);
        preExecutionRunnables.add(() -> fileTarget.copyFileToContainer(
                MountableFile.forClasspathResource(resourceName),
                path
        ));
        return this;
    }

    public void run() throws IOException, ExecutionException, InterruptedException {
        final List<String> cmdline = new ArrayList<>();
        cmdline.add("kubectl");
        cmdline.add("apply");
        if (namespace != null) {
            cmdline.add("--namespace");
            cmdline.add(namespace);
        }
        files.forEach(file -> {
            cmdline.add("-f");
            cmdline.add(file);
        });
        preExecutionRunnables.forEach(Runnable::run);
        exec.safeExecInContainer(cmdline.toArray(new String[]{}));
    }
}
