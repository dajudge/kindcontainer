package com.dajudge.kindcontainer.helm;

import com.dajudge.kindcontainer.BaseSidecarContainer.ExecInContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class InstallFluent<P> {

    private final ExecInContainer c;
    private final P parent;
    private String namespace;
    private boolean createNamespace;
    private String version;
    private final Map<String, String> params = new HashMap<>();

    private final List<String> values = new ArrayList<>();

    public InstallFluent(final ExecInContainer c, final P parent) {
        this.c = c;
        this.parent = parent;
    }

    public InstallFluent<P> set(final String key, final String value) {
        params.put(key, value);
        return this;
    }

    public InstallFluent<P> values(final String path) {
        values.add(path);
        return this;
    }

    public InstallFluent<P> namespace(final String namespace) {
        this.namespace = namespace;
        return this;
    }

    public InstallFluent<P> createNamespace(final boolean createNamespace) {
        this.createNamespace = createNamespace;
        return this;
    }


    public InstallFluent<P> createNamespace() {
        return createNamespace(true);
    }

    public InstallFluent<P> version(final String version) {
        this.version = requireNonNull(version, "version cannot be null");
        return this;
    }

    public P run(final String releaseName, final String chart) throws IOException, InterruptedException, ExecutionException {
        try {
            final List<String> cmdline = new ArrayList<>(asList("helm", "install"));
            if (namespace != null) {
                cmdline.addAll(asList("--namespace", namespace));
            }
            if (createNamespace) {
                cmdline.add("--create-namespace");
            }
            if (version != null) {
                cmdline.addAll(asList("--version", version));
            }
            params.forEach((k, v) -> cmdline.addAll(asList("--set", String.format("%s=%s", k, v))));
            cmdline.addAll(asList(releaseName, chart));
            values.forEach(v -> {
                cmdline.addAll(asList("-f", v));
            });
            c.safeExecInContainer(cmdline.toArray(new String[]{}));
            return parent;
        } finally {
            createNamespace = false;
            namespace = null;
            version = null;
            params.clear();
        }
    }
}
