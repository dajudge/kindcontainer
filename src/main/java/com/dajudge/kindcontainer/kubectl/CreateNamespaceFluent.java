package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;

public class CreateNamespaceFluent<P> {

    private final BaseSidecarContainer.ExecInContainer exec;
    private final P parent;

    public CreateNamespaceFluent(final BaseSidecarContainer.ExecInContainer exec, final P parent) {
        this.exec = exec;
        this.parent = parent;
    }

    public P run(final String name) throws IOException, ExecutionException, InterruptedException {
        exec.safeExecInContainer("kubectl", "create", "namespace", name);
        return parent;
    }
}
