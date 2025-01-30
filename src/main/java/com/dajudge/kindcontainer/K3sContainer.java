package com.dajudge.kindcontainer;

/*
 * This file is loosely inspired by https://github.com/testcontainers/testcontainers-java/blob/1.16.3/modules/k3s/src/main/java/org/testcontainers/k3s/K3sContainer.java
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2019 Richard North
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.latest;
import static com.dajudge.kindcontainer.client.KubeConfigUtils.replaceServerInKubeconfig;
import static com.github.dockerjava.api.model.DockerObjectAccessor.overrideRawValue;
import static java.util.Arrays.asList;

public class K3sContainer<SELF extends K3sContainer<SELF>> extends KubernetesWithKubeletContainer<SELF> {
    private static final Logger LOG = LoggerFactory.getLogger(K3sContainer.class);
    private static final int INTERNAL_API_SERVER_PORT = 6443;
    private static final HashMap<String, String> TMP_FILESYSTEMS = new HashMap<String, String>() {{
        put("/run", "");
        put("/var/run", "");
    }};
    private final K3sContainerVersion version;
    private int minNodePort = 30000;
    private int maxNodePort = 32767;
    private Function<List<String>, List<String>> cmdLineModifier = Function.identity();

    public K3sContainer() {
        this(latest(K3sContainerVersion.class));
    }

    public K3sContainer(final K3sContainerVersion version) {
        this(version.toImageSpec());
    }

    public K3sContainer(final KubernetesImageSpec<K3sContainerVersion> imageSpec) {
        super(imageSpec.getImage());
        this.version = imageSpec.getVersion();
        this
                .withExposedPorts(INTERNAL_API_SERVER_PORT)
                .withPrivilegedMode(true)
                .withCreateContainerCmdModifier(it -> overrideRawValue(it.getHostConfig(), "CgroupnsMode", "host"))
                .withTmpFs(TMP_FILESYSTEMS);
    }

    @Override
    public void start() {
        final List<String> cmdLine = cmdLineModifier.apply(new ArrayList<>(asList(
                "server",
                getDisabledComponentsCmdlineArg(),
                String.format("--tls-san=%s", this.getHost()),
                String.format("--service-node-port-range=%d-%d", minNodePort, maxNodePort)
        )));
        LOG.debug("K3s command line: {}", cmdLine);
        this.withCommand(cmdLine.toArray(new String[0]));
        super.start();
    }

    private String getDisabledComponentsCmdlineArg() {
        if (new KubernetesVersionDescriptor(1, 25, 0).compareTo(version.descriptor()) <= 0) {
            return "--disable=traefik";
        }
        return "--no-deploy=traefik";
    }

    @Override
    public SELF withNodePortRange(final int minPort, final int maxPort) {
        this.minNodePort = minPort;
        this.maxNodePort = maxPort;
        return self();
    }

    /**
     * Sets a command line modifier for the K3s container, e.g. to configure etcd or the K8s API server.
     *
     * @param modifier the command line modifier
     * @return <code>this</code>
     */
    public SELF withCommandLineModifier(final Function<List<String>, List<String>> modifier) {
        this.cmdLineModifier = modifier;
        return self();
    }

    @Override
    public int getInternalPort() {
        return INTERNAL_API_SERVER_PORT;
    }

    @Override
    protected String getKubeconfig(final String server) {
        return replaceServerInKubeconfig(server, getOriginalKubeconfig());
    }

    private String getOriginalKubeconfig() {
        return copyFileFromContainer("/etc/rancher/k3s/k3s.yaml", Utils::readString);
    }
}
