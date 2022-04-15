package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class WaitFluent<P> {
    private final BaseSidecarContainer.ExecInContainer exec;
    private final List<String> conditions = new ArrayList<>();
    private String namespace;
    private String timeout;

    public WaitFluent(final BaseSidecarContainer.ExecInContainer exec) {
        this.exec = exec;
    }

    public WaitFluent<P> namespace(final String namespace) {
        this.namespace = namespace;
        return this;
    }

    public WaitFluent<P> forCondition(final String condition) {
        conditions.add("condition=" + condition);
        return this;
    }

    public WaitFluent<P> timeout(final String timeout) {
        this.timeout = timeout;
        return this;
    }

    public void run(final String kind, final String name) throws IOException, ExecutionException, InterruptedException {
        try {
            final List<String> cmdline = new ArrayList<>(asList("kubectl", "wait"));
            conditions.forEach(c -> {
                cmdline.add("--for");
                cmdline.add(c);
            });
            if (namespace != null) {
                cmdline.add("-n");
                cmdline.add(namespace);
            }
            if (timeout != null) {
                cmdline.add("--timeout");
                cmdline.add(timeout);
            }
            cmdline.add(kind);
            cmdline.add(name);
            exec.safeExecInContainer(cmdline.toArray(new String[]{}));
        } finally {
            resetFluent();
        }
    }

    private void resetFluent() {
        conditions.clear();
        timeout = null;
        namespace = null;
    }
}
