package com.dajudge.kindcontainer.helm;

import com.dajudge.kindcontainer.BaseSidecarContainer.ExecInContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.*;

import static java.util.Arrays.asList;

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

    /**
     * Adds the given key/value as --set parameter to the Helm install command.
     * @param key required
     * @param value required
     * @return The fluent API
     */
    public InstallFluent<P> set(final String key, final String value) {
        params.put(key, value);
        return this;
    }

    /**
     * Adds the given values file as -f parameter to the Helm install command.
     * Make sure the values file is available in the Helm container.
     * @see ContainerState#copyFileToContainer(MountableFile, String)
     * @param path Path to the values file in the Helm container.
     * @return The fluent API
     */
    public InstallFluent<P> values(final String path) {
        values.add(path);
        return this;
    }

    /**
     * Sets the given namespace as target namespace (--namespace parameter) for the Helm install command.
     * @param namespace required
     * @return The fluent API
     */
    public InstallFluent<P> namespace(final String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * Enables or disables the creation of the target namespace for the Helm install command. (--create-namespace parameter)
     * @param createNamespace true to enable creation, false otherwise
     * @return The fluent API
     */
    public InstallFluent<P> createNamespace(final boolean createNamespace) {
        this.createNamespace = createNamespace;
        return this;
    }


    /**
     * Enables the creation of the target namespace for the Helm install command.
     * @return The fluent API
     */
    public InstallFluent<P> createNamespace() {
        return createNamespace(true);
    }

    /**
     * Sets the version for the Helm chart to be installed. (--version parameter)
     * @param version required
     * @return The fluent API
     */
    public InstallFluent<P> version(final String version) {
        this.version = version;
        return this;
    }

    /**
     * Runs the Helm install command.
     * @param releaseName The release name of the Helm installation
     * @param chart The chart name of the Helm installation
     * @return Parent container
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
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
