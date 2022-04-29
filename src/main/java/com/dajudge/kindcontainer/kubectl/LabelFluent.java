package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LabelFluent<P> {
    private final BaseSidecarContainer.ExecInContainer exec;
    private final P parent;
    private final Map<String, String> labels = new HashMap<>();

    public LabelFluent(final BaseSidecarContainer.ExecInContainer exec, final P parent) {
        this.exec = exec;
        this.parent = parent;
    }

    private void reset() {
        labels.clear();
    }

    public LabelFluent<P> with(final String key, final String value) {
        labels.put(key, value);
        return this;
    }

    public LabelFluent<P> with(final Map<String, String> labels) {
        this.labels.putAll(labels);
        return this;
    }

    public void run(final String kind, final String name) throws IOException, ExecutionException, InterruptedException {
        try {
            final List<String> cmd = new ArrayList<>();
            cmd.add("kubectl");
            cmd.add("label");
            cmd.add(kind);
            cmd.add(name);
            labels.forEach((k, v) -> {
                cmd.add(String.format("%s=%s", k, v));
            });
            exec.safeExecInContainer(cmd.toArray(new String[]{}));
        } finally {
            reset();
        }
    }
}
