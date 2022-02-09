package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer.ExecInContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeleteFluent {
    private final ExecInContainer exec;
    private String namespace;
    private boolean ignoreNotFound;
    private boolean force;
    private Integer gracePeriod;

    DeleteFluent(final ExecInContainer exec) {
        this.exec = exec;
    }

    public void run(final String kind, final String name) throws IOException, ExecutionException, InterruptedException {
        final List<String> cmdline = new ArrayList<>();
        cmdline.add("kubectl");
        cmdline.add("delete");
        if (namespace != null) {
            cmdline.add("--namespace");
            cmdline.add(namespace);
        }
        if (ignoreNotFound) {
            cmdline.add("--ignore-not-found=true");
        }
        if (force) {
            cmdline.add("--force=true");
        }
        if (gracePeriod != null) {
            cmdline.add("--grace-period=0");
        }
        cmdline.add(kind);
        cmdline.add(name);
        exec.safeExecInContainer(cmdline.toArray(new String[]{}));
    }

    public DeleteFluent namespace(final String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DeleteFluent ignoreNotFound() {
        ignoreNotFound = true;
        return this;
    }

    public DeleteFluent force() {
        force = true;
        return this;
    }

    public DeleteFluent gracePeriod(int value) {
        gracePeriod = value;
        return this;
    }
}
